import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const cookieService = inject(CookieService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Check if the error is a 401 Unauthorized from the backend
      if (error.status === 401) {
        // Skip redirect for auth endpoints (login/register handle their own errors)
        const isAuthEndpoint = req.url.includes('/api/auth/login') || req.url.includes('/api/auth/register');
        
        if (!isAuthEndpoint) {
          // This means the JWT in the cookie is invalid or expired.
          // Log the user out.
          console.log('Session expired or token is invalid. Redirecting to login.');
          
          // Use the correct cookie name 'jwt'
          cookieService.delete('jwt', '/'); 
          
          // Redirect to the login page
          router.navigate(['/login']);
        }
      }
      
      // Pass the error along
      return throwError(() => error);
    })
  );
};
