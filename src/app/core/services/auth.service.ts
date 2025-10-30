import {computed, effect, inject, Injectable, signal} from '@angular/core';
import {toObservable} from '@angular/core/rxjs-interop';
import {HttpClient} from '@angular/common/http';
import {firstValueFrom, Observable, throwError} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {Router} from '@angular/router';
import Cookies from 'js-cookie';
import {environment} from '../../../environments/environment';
import {NotificationService} from './notification.service';
import {DeviceService} from './device.service';

export interface AuthInitRequest {
  phone: string;
  country_code: number;
  token_id?: string;
  recaptcha_token?: string;
  device_id?: string;
  device_name?: string;
  device_type?: string;
  platform?: string;
  browser?: string;
  os?: string;
}

export interface AuthInitResponse {
  message: string;
  session_id: string;
}

export interface OtpVerificationRequest {
  session_id: string;
  otp: string;
  auth_mode: string;
  recaptcha_token?: string;
  device_id?: string;
  device_name?: string;
}

export interface AuthResponse {
  access_token: string;
  refresh_token: string;
  access_token_expires_at?: string;
  refresh_token_expires_at?: string;
}

export interface FirebaseAuthRequest {
  firebase_id_token: string;
  country_code: number;
  phone: string;
  recaptcha_token?: string | null;
  device_id?: string;
  device_name?: string;
  device_type?: string;
  platform?: string;
  browser?: string;
  os?: string;
  user_agent?: string;
}

export interface User {
  id: string;
  first_name: string;
  last_name?: string;
  user_name: string;
  country_code: number;
  phone: string;
  email?: string;
  full_name: string;
  active: boolean;
}

export interface DeviceSession {
  device_id: string;
  device_name: string;
  device_type: string;
  platform: string;
  browser: string;
  os: string;
  ip_address: string;
  location?: string;
  last_activity: string;
  login_time: string;
  is_current_device: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly AUTH_API_URL = `${environment.apiBaseUrl}/auth/v1`;
  private readonly USER_API_URL = `${environment.apiBaseUrl}/user/v1`;

  // Modern dependency injection
  private http = inject(HttpClient);
  private router = inject(Router);
  private notificationService = inject(NotificationService);
  private deviceService = inject(DeviceService);

  // Signal-based state management
  private _currentUser = signal<User | null>(null);
  // Public readonly signals
  readonly currentUser = this._currentUser.asReadonly();
  // Backward compatibility Observable properties (deprecated - use signals instead)
  /** @deprecated Use currentUser signal instead */
  readonly currentUser$ = toObservable(this._currentUser);
  // Computed signals
  readonly isProfileComplete = computed(() => {
    const user = this._currentUser();
    return user && user.first_name && user.first_name.trim() !== '';
  });
  readonly userDisplayName = computed(() => {
    const user = this._currentUser();
    if (!user) return '';
    return user.full_name || `${user.first_name} ${user.last_name || ''}`.trim();
  });
  private _isAuthenticated = signal<boolean | null>(null);
  readonly isAuthenticated = this._isAuthenticated.asReadonly();
  /** @deprecated Use isAuthenticated signal instead */
  readonly isAuthenticated$ = toObservable(this._isAuthenticated);
  readonly authenticationStatus = computed(() => {
    const isAuth = this._isAuthenticated();
    if (isAuth === null) return 'checking';
    return isAuth ? 'authenticated' : 'unauthenticated';
  });
  private _loading = signal(false);
  readonly loading = this._loading.asReadonly();
  private _error = signal<string | null>(null);
  readonly error = this._error.asReadonly();

  constructor() {
    // Effect to handle authentication state changes
    effect(() => {
      const isAuth = this._isAuthenticated();
      if (isAuth === false && this.router.url !== '/login') {
        this.router.navigate(['/login']);
      }
    });

    this.checkAuthenticationStatus();
  }


  /**
   * Initialize authentication by sending mobile number
   */
  async initAuth(mobileNumber: string, recaptchaToken?: string): Promise<AuthInitResponse> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const deviceInfo = this.deviceService.getDeviceInfo();

      const request: AuthInitRequest = {
        phone: mobileNumber,
        country_code: 91,
        token_id: '',
        ...(recaptchaToken && {recaptcha_token: recaptchaToken}),
        device_id: deviceInfo.device_id,
        device_name: deviceInfo.device_name,
        device_type: deviceInfo.device_type,
        platform: deviceInfo.platform,
        browser: deviceInfo.browser,
        os: deviceInfo.os
      };

      const response = await firstValueFrom(
        this.http.post<AuthInitResponse>(`${this.AUTH_API_URL}/init`, request)
          .pipe(catchError(this.handleError))
      );

      return response;
    } catch (error: any) {
      this._error.set(error.message || 'Authentication initialization failed');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }


  /**
   * Verify OTP and complete authentication
   */
  async verifyOtp(sessionId: string, otp: string, recaptchaToken?: string): Promise<AuthResponse> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const deviceInfo = this.deviceService.getDeviceInfo();

      const request: OtpVerificationRequest = {
        session_id: sessionId,
        otp: otp,
        auth_mode: 'OTP',
        ...(recaptchaToken && {recaptcha_token: recaptchaToken}),
        device_id: deviceInfo.device_id,
        device_name: deviceInfo.device_name
      };

      const response = await firstValueFrom(
        this.http.post<AuthResponse>(`${this.AUTH_API_URL}/verify`, request)
          .pipe(catchError(this.handleError))
      );

      if (response.access_token && response.refresh_token) {
        this.setAuthTokens(response.access_token, response.refresh_token,
          response.access_token_expires_at, response.refresh_token_expires_at);
        this._isAuthenticated.set(true);

        // Get user profile after successful authentication
        try {
          const user = await this.getUserProfile();
          this._currentUser.set(user);
        } catch (error) {
          console.error('Failed to get user profile:', error);
        }
      }

      return response;
    } catch (error: any) {
      this._error.set(error.message || 'OTP verification failed');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Authenticate with Firebase token (for desktop app authentication)
   */
  async authenticateWithFirebase(request: FirebaseAuthRequest): Promise<AuthResponse> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const response = await firstValueFrom(
        this.http.post<AuthResponse>(`${this.AUTH_API_URL}/verify/firebase`, request)
          .pipe(catchError(this.handleError))
      );

      if (response.access_token && response.refresh_token) {
        this.setAuthTokens(response.access_token, response.refresh_token,
          response.access_token_expires_at, response.refresh_token_expires_at);
        this._isAuthenticated.set(true);

        // Get user profile after successful authentication
        try {
          const user = await this.getUserProfile();
          this._currentUser.set(user);
        } catch (error) {
          console.error('Failed to get user profile:', error);
        }
      }

      return response;
    } catch (error: any) {
      this._error.set(error.message || 'Firebase authentication failed');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Refresh access token using refresh token (async version)
   */
  async refreshTokenAsync(): Promise<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      await this.logout('No refresh token available');
      throw new Error('No refresh token available');
    }

    const deviceInfo = this.deviceService.getDeviceInfo();

    try {
      const response = await firstValueFrom(
        this.http.post<AuthResponse>(`${this.AUTH_API_URL}/refresh_token`, {
          refresh_token: refreshToken,
          device_id: deviceInfo.device_id
        }).pipe(catchError(this.handleError))
      );

      if (response && response.access_token && response.refresh_token) {
        this.setAuthTokens(response.access_token, response.refresh_token,
          response.access_token_expires_at, response.refresh_token_expires_at);
        this._isAuthenticated.set(true);
      } else {
        await this.logout('Token refresh failed');
        throw new Error('Token refresh failed');
      }

      return response;
    } catch (error: any) {
      // Handle different types of refresh token failures
      let logoutReason = 'Token refresh error';
      if (error.status === 401) {
        logoutReason = 'Refresh token expired or invalid';
      } else if (error.status === 403) {
        logoutReason = 'Refresh token forbidden';
      } else if (error.status === 400) {
        logoutReason = 'Invalid refresh token format';
      }
      await this.logout(logoutReason);
      throw error;
    }
  }

  /**
   * Refresh access token using refresh token (legacy Observable version)
   */
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.logout('No refresh token available');
      return throwError(() => new Error('No refresh token available'));
    }

    const deviceInfo = this.deviceService.getDeviceInfo();

    return this.http.post<AuthResponse>(`${this.AUTH_API_URL}/refresh_token`, {
      refresh_token: refreshToken,
      device_id: deviceInfo.device_id
    }).pipe(
      map(response => {
        if (response && response.access_token && response.refresh_token) {
          this.setAuthTokens(response.access_token, response.refresh_token, response.access_token_expires_at, response.refresh_token_expires_at);
          this._isAuthenticated.set(true);
        } else {
          // If refresh fails, logout user
          this.logout('Token refresh failed');
          throw new Error('Token refresh failed');
        }
        return response;
      }),
      catchError((error: any) => {
        // Handle different types of refresh token failures
        let logoutReason = 'Token refresh error';
        if (error.status === 401) {
          logoutReason = 'Refresh token expired or invalid';
        } else if (error.status === 403) {
          logoutReason = 'Refresh token forbidden';
        } else if (error.status === 400) {
          logoutReason = 'Invalid refresh token format';
        }
        this.logout(logoutReason);
        return this.handleError(error);
      })
    );
  }

  /**
   * Logout user and clear all tokens
   */
  async logout(reason?: string): Promise<void> {
    this._loading.set(true);

    try {
      // Log the reason for logout for debugging
      if (reason) {
        console.log('Logout reason:', reason);

        // Show appropriate notification based on reason
        if (reason.includes('expired') || reason.includes('invalid')) {
          this.notificationService.showSessionExpired();
        } else if (reason.includes('refresh')) {
          this.notificationService.showTokenRefreshFailed();
        }
      }

      // Call logout endpoint to invalidate session on server
      const accessToken = this.getAccessToken();
      if (accessToken) {
        try {
          await firstValueFrom(this.http.post(`${this.AUTH_API_URL}/logout`, {}));
        } catch (error) {
          console.error('Logout error:', error);
        }
      }

      // Clear state and tokens
      this.clearAuthTokens();
      this._currentUser.set(null);
      this._isAuthenticated.set(false);
      this._error.set(null);

      // Navigation will be handled by the effect in constructor
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Get current access token
   */
  getAccessToken(): string | null {
    return Cookies.get('access_token') || null;
  }

  /**
   * Get current refresh token
   */
  getRefreshToken(): string | null {
    return Cookies.get('refresh_token') || null;
  }

  /**
   * Check if token is valid (new method)
   */
  isTokenValid(): boolean {
    const token = this.getAccessToken();
    if (!token) {
      return false;
    }
    try {
      // Check if token is expired (basic JWT expiration check)
      const parts = token.split('.');
      if (parts.length !== 3 || !parts[1]) return false;
      const payload = JSON.parse(atob(parts[1]));
      const currentTime = Math.floor(Date.now() / 1000);
      return payload.exp > currentTime;
    } catch (error) {
      return false;
    }
  }

  /**
   * Check if user is currently authenticated (legacy method - use signal instead)
   * @deprecated Use isAuthenticated signal instead
   */
  isAuthenticatedLegacy(): boolean {
    return this.isTokenValid();
  }

  /**
   * Get current user information (legacy method - use signal instead)
   */
  getCurrentUser(): User | null {
    return this._currentUser();
  }

  /**
   * Get all active device sessions for the current user
   */
  getDeviceSessions(): Observable<DeviceSession[]> {
    return this.http.get<DeviceSession[]>(`${this.AUTH_API_URL}/devices`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Logout from a specific device
   */
  logoutDevice(deviceId: string): Observable<any> {
    return this.http.post(`${this.AUTH_API_URL}/devices/${deviceId}/logout`, {})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Logout from all devices
   */
  logoutAllDevices(): Observable<any> {
    return this.http.post(`${this.AUTH_API_URL}/logout/all`, {})
      .pipe(
        map(() => {
          // Clear local tokens and update state
          this.clearAuthTokens();
          this._currentUser.set(null);
          this._isAuthenticated.set(false);

          // Navigate to login page
          if (this.router.url !== '/login') {
            this.router.navigate(['/login']);
          }
        }),
        catchError(this.handleError)
      );
  }

  // Make user profile API public and add update name API
  public async getUserProfile(): Promise<User> {
    try {
      const user = await firstValueFrom(
        this.http.get<User>(`${this.USER_API_URL}`)
          .pipe(catchError(this.handleError))
      );
      return user;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to get user profile');
      throw error;
    }
  }

  public isProfileIncomplete(user: User | null): boolean {
    return !user || !user.first_name || user.first_name.trim() === '';
  }

  public async updateUserName(firstName: string, lastName?: string): Promise<User> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const body: any = {first_name: firstName};
      if (lastName && lastName.trim()) {
        body.last_name = lastName;
      }

      const updatedUser = await firstValueFrom(
        this.http.post<User>(`${this.USER_API_URL}/update`, body)
          .pipe(catchError(this.handleError))
      );

      this._currentUser.set(updatedUser);
      return updatedUser;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to update user profile');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Check authentication status on service initialization
   */
  private async checkAuthenticationStatus(): Promise<void> {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();

    if (accessToken && this.isTokenValid()) {
      // If we have a valid access token, set authenticated state and get user info
      this._isAuthenticated.set(true);

      try {
        const user = await this.getUserProfile();
        this._currentUser.set(user);
      } catch (error) {
        // If getting user profile fails, try to refresh token
        try {
          await this.refreshTokenAsync();
          // After successful refresh, try to get user profile again
          const user = await this.getUserProfile();
          this._currentUser.set(user);
        } catch (refreshError) {
          await this.logout('Token refresh failed during authentication check');
        }
      }
    } else if (refreshToken) {
      // If we only have refresh token, try to refresh
      try {
        await this.refreshTokenAsync();
        this._isAuthenticated.set(true);
        // After successful refresh, get user profile
        const user = await this.getUserProfile();
        this._currentUser.set(user);
      } catch (error) {
        await this.logout('Refresh token expired or invalid');
      }
    } else {
      // No tokens available, ensure logged out state
      this._isAuthenticated.set(false);
      this._currentUser.set(null);
    }
  }

  /**
   * Store authentication tokens in secure cookies
   */
  private setAuthTokens(accessToken: string, refreshToken: string, accessTokenExpiresAt?: string, refreshTokenExpiresAt?: string): void {
    // Set cookies with secure options
    const cookieOptions = {
      secure: location.protocol === 'https:', // Only secure in production HTTPS
      sameSite: 'lax' as const, // Changed from strict to lax for better compatibility
    };

    // Calculate expires from server-provided dates or fallback to defaults
    let accessTokenExpires = 1 / 24; // Default 1 hour
    let refreshTokenExpires = 7; // Default 7 days

    if (accessTokenExpiresAt) {
      try {
        const expiresDate = new Date(accessTokenExpiresAt);
        const now = new Date();
        const diffMs = expiresDate.getTime() - now.getTime();
        const diffDays = diffMs / (1000 * 60 * 60 * 24);
        if (diffDays > 0) {
          accessTokenExpires = diffDays;
        }
      } catch (error) {
        console.warn('Failed to parse access token expiry date, using default:', error);
      }
    }

    if (refreshTokenExpiresAt) {
      try {
        const expiresDate = new Date(refreshTokenExpiresAt);
        const now = new Date();
        const diffMs = expiresDate.getTime() - now.getTime();
        const diffDays = diffMs / (1000 * 60 * 60 * 24);
        if (diffDays > 0) {
          refreshTokenExpires = diffDays;
        }
      } catch (error) {
        console.warn('Failed to parse refresh token expiry date, using default:', error);
      }
    }

    // Set cookies with calculated expiry times
    Cookies.set('access_token', accessToken, {
      ...cookieOptions,
      expires: accessTokenExpires
    });

    Cookies.set('refresh_token', refreshToken, {
      ...cookieOptions,
      expires: refreshTokenExpires
    });

  }

  /**
   * Clear all authentication tokens
   */
  private clearAuthTokens(): void {
    Cookies.remove('access_token');
    Cookies.remove('refresh_token');
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('Auth Service Error:', error);
    let errorMessage = 'An unexpected error occurred';
    // Extract error message from interceptor-formatted error
    if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }
    return throwError(() => new Error(errorMessage));
  }
}
