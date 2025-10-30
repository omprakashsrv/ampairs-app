import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Observable} from 'rxjs';
import {finalize} from 'rxjs/operators';
import {LoadingService} from '../services/loading.service';

@Injectable()
export class LoadingInterceptor implements HttpInterceptor {

  constructor(private loadingService: LoadingService) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Don't show loading for authentication endpoints to avoid UI flicker
    const skipLoading = this.shouldSkipLoading(req.url);

    if (!skipLoading) {
      this.loadingService.setLoading(true);
    }

    return next.handle(req).pipe(
      finalize(() => {
        if (!skipLoading) {
          this.loadingService.setLoading(false);
        }
      })
    );
  }

  private shouldSkipLoading(url: string): boolean {
    const skipEndpoints = [
      '/auth/refresh', // Don't show loading for token refresh
      '/auth/logout'   // Don't show loading for logout
    ];

    return skipEndpoints.some(endpoint => url.includes(endpoint));
  }
}
