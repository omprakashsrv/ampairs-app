import {Injectable, Injector} from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import {BehaviorSubject, Observable, throwError} from 'rxjs';
import {catchError, filter, map, switchMap, take} from 'rxjs/operators';
import {NotificationService} from '../services/notification.service';
import {Router} from '@angular/router';
import {environment} from '../../../environments/environment';
import Cookies from 'js-cookie';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);
  private readonly AUTH_API_URL = `${environment.apiBaseUrl}/auth/v1`;

  constructor(
    private injector: Injector,
    private notificationService: NotificationService,
    private router: Router
  ) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip interceptor for authentication endpoints
    if (this.isAuthEndpoint(req.url)) {
      return next.handle(req);
    }

    // Add authorization header if token exists
    const authReq = this.addTokenHeader(req);

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        // Handle 401 Unauthorized errors (token expired)
        if (error.status === 401 && !this.isAuthEndpoint(req.url)) {
          return this.handle401Error(authReq, next);
        }

        // Handle other errors
        return throwError(() => error);
      })
    );
  }

  private addTokenHeader(request: HttpRequest<any>): HttpRequest<any> {
    const token = this.getAccessToken();

    if (token) {
      return request.clone({
        headers: request.headers.set('Authorization', `Bearer ${token}`)
      });
    }

    return request;
  }

  private getAccessToken(): string | null {
    return Cookies.get('access_token') || null;
  }

  private getRefreshToken(): string | null {
    return Cookies.get('refresh_token') || null;
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      const refreshToken = this.getRefreshToken();

      if (refreshToken) {
        return this.performTokenRefresh(refreshToken).pipe(
          switchMap((tokenData: any) => {
            this.isRefreshing = false;

            if (tokenData && tokenData.access_token && tokenData.refresh_token) {
              this.setAuthTokens(tokenData.access_token, tokenData.refresh_token);
              this.refreshTokenSubject.next(tokenData.access_token);
              // Retry the failed request with new token
              return next.handle(this.addTokenHeader(request));
            } else {
              // Refresh failed - could be refresh token expired
              console.log('Token refresh failed, redirecting to login');
              this.refreshTokenSubject.next('REFRESH_FAILED');
              this.notificationService.showSessionExpired();
              this.clearAuthTokens();
              this.router.navigate(['/login']);
              return throwError(() => new Error('Token refresh failed - redirecting to login'));
            }
          }),
          catchError((error) => {
            this.isRefreshing = false;
            console.log('Token refresh error:', error);

            // Check if error is due to refresh token expiration
            if (error.status === 401 || error.status === 403) {
              console.log('Refresh token expired, redirecting to login');
              this.notificationService.showSessionExpired();
            } else {
              this.notificationService.showTokenRefreshFailed();
            }

            // Notify waiting requests that refresh failed
            this.refreshTokenSubject.next('REFRESH_FAILED');
            this.clearAuthTokens();
            this.router.navigate(['/login']);
            return throwError(() => new Error('Authentication failed - redirecting to login'));
          })
        );
      } else {
        // No refresh token available, redirect to login
        console.log('No refresh token available, redirecting to login');
        this.isRefreshing = false;
        this.notificationService.showSessionExpired();
        this.clearAuthTokens();
        this.router.navigate(['/login']);
        return throwError(() => new Error('No refresh token available - redirecting to login'));
      }
    } else {
      // If refresh is in progress, wait for it to complete
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap((token) => {
          if (token === 'REFRESH_FAILED') {
            // Refresh failed, redirect to login
            this.router.navigate(['/login']);
            return throwError(() => new Error('Authentication failed'));
          }
          return next.handle(this.addTokenHeader(request));
        })
      );
    }
  }

  private performTokenRefresh(refreshToken: string): Observable<any> {
    // Get HttpClient lazily to avoid circular dependency
    const http = this.injector.get(HttpClient);

    return http.post<any>(`${this.AUTH_API_URL}/refresh_token`, {
      refresh_token: refreshToken,
      device_id: this.getDeviceId()
    }).pipe(
      map(response => {
        // Handle wrapped API response structure
        if (response && response.success && response.data) {
          return response.data;
        }
        return response;
      })
    );
  }

  private getDeviceId(): string {
    // Simple device ID based on user agent and screen properties
    const userAgent = navigator.userAgent;
    const screenProps = `${screen.width}x${screen.height}`;
    const deviceString = userAgent + screenProps;

    // Create a simple hash
    let hash = 0;
    for (let i = 0; i < deviceString.length; i++) {
      const char = deviceString.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }

    return `WEB_${Math.abs(hash).toString(36).toUpperCase()}`;
  }

  private setAuthTokens(accessToken: string, refreshToken: string): void {
    const cookieOptions = {
      secure: location.protocol === 'https:',
      sameSite: 'lax' as const,
    };

    // Set cookies with default expiry times
    Cookies.set('access_token', accessToken, {
      ...cookieOptions,
      expires: 1 / 24 // 1 hour
    });

    Cookies.set('refresh_token', refreshToken, {
      ...cookieOptions,
      expires: 7 // 7 days
    });
  }

  private clearAuthTokens(): void {
    Cookies.remove('access_token');
    Cookies.remove('refresh_token');
  }

  private isAuthEndpoint(url: string): boolean {
    // List of endpoints that should not trigger token refresh
    const authEndpoints = [
      '/auth/v1/init',
      '/auth/v1/verify',
      '/auth/v1/refresh_token',
      '/auth/v1/logout'
    ];

    return authEndpoints.some(endpoint => url.includes(endpoint));
  }
}
