import {Injectable} from '@angular/core';
import {CanActivate, Router} from '@angular/router';
import {Observable, of} from 'rxjs';
import {filter, map, take} from 'rxjs/operators';
import {AuthService} from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
  }

  canActivate(): Observable<boolean> {
    // First check if we have tokens and can authenticate immediately
    const hasTokens = this.authService.getAccessToken() || this.authService.getRefreshToken();

    if (!hasTokens) {
      // No tokens, immediate redirect to login
      this.router.navigate(['/login']);
      return of(false);
    }

    // We have tokens, wait for authentication check to complete
    return this.authService.isAuthenticated$.pipe(
      filter((isAuthenticated): isAuthenticated is boolean => isAuthenticated !== null), // Wait until authentication status is determined
      take(1),
      map(isAuthenticated => {
        if (isAuthenticated) {
          return true;
        } else {
          this.router.navigate(['/login']);
          return false;
        }
      })
    );
  }
}
