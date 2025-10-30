import {Injectable} from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {catchError, map} from 'rxjs/operators';

@Injectable()
export class ApiResponseInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      map(event => {
        if (event instanceof HttpResponse && event.status === 200) {
          const body = event.body;

          // Check if response has the ApiResponse structure with successful response
          if (body && typeof body === 'object' &&
            'success' in body && 'data' in body && body.success === true) {
            // Unwrap the data from ApiResponse<T> structure
            return event.clone({
              body: body.data
            });
          }

          // Handle error ApiResponse structure (when backend returns 200 but success: false)
          if (body && typeof body === 'object' &&
            'success' in body && body.success === false && 'error' in body) {
            // Convert to HTTP error for consistent error handling
            throw new HttpErrorResponse({
              error: body.error,
              status: body.error.code === 'VALIDATION_ERROR' ? 400 :
                body.error.code === 'AUTHENTICATION_FAILED' ? 401 :
                  body.error.code === 'ACCESS_DENIED' ? 403 :
                    body.error.code === 'NOT_FOUND' ? 404 :
                      body.error.code === 'RATE_LIMIT_EXCEEDED' ? 429 : 422,
              statusText: body.error.message || 'Request Failed',
              url: req.url
            });
          }
        }
        return event;
      }),
      catchError((error: HttpErrorResponse) => {
        // Pass through HTTP errors unchanged
        return throwError(() => error);
      })
    );
  }
}
