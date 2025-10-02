import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private router: Router, private cookieService: CookieService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Check if the error is a 401 Unauthorized
        if (error.status === 401) {
          // The token is invalid or expired, so log the user out
          console.log('Session expired or token is invalid. Redirecting to login.');
          this.cookieService.delete('jwt', '/'); // Delete the invalid cookie
          this.router.navigate(['/login']); // Redirect to the login page
        }
        return throwError(() => error);
      })
    );
  }
}
