// src/app/interceptors/error.interceptor.ts
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar, MatSnackBarRef, SimpleSnackBar } from '@angular/material/snack-bar';
import { Router, NavigationStart } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';

let routerSubscription: Subscription | null = null;
let activeSnackRef: MatSnackBarRef<SimpleSnackBar> | null = null;

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);
  const router = inject(Router);

  if (!routerSubscription || routerSubscription.closed) {
    routerSubscription = router.events
      .pipe(filter(event => event instanceof NavigationStart))
      .subscribe(() => {
        if (activeSnackRef) {
          activeSnackRef.dismiss();
          activeSnackRef = null;
        }
      });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message = 'An error occurred';

      if (error.error instanceof ErrorEvent) {
        message = error.error.message;
      } else {
        const backend = error.error;
        message = backend?.message || backend?.error || `HTTP ${error.status}`;
      }

      // Skip error handling for auth endpoints (login/register handle their own errors)
      const isAuthEndpoint = req.url.includes('/api/auth/login') || req.url.includes('/api/auth/register');

      // Specific handling
      switch (error.status) {
        case 400:
          if (error.error?.errors) {
            const fieldErrors = Object.entries(error.error.errors)
              .map(([field, msg]) => `${field}: ${msg}`)
              .join(', ');
            message = fieldErrors;
          }
          break;
        case 401:
          // Don't show "Session expired" for login/register - let them handle it
          if (!isAuthEndpoint) {
            message = 'Session expired. Please log in.';
            router.navigate(['/login']);
          } else {
            message = 'Invalid credentials. Please try again.';
          }
          break;
        case 403:
          message = 'You are not authorized.';
          break;
        case 404:
          message = 'Resource not found.';
          break;
        case 413:
          message = 'File too large (max 2 MB).';
          break;
      }

      if (activeSnackRef) {
        activeSnackRef.dismiss();
      }
      activeSnackRef = snackBar.open(message, 'Close', { duration: 5000, panelClass: 'error-snack' });

      return throwError(() => error);
    })
  );
};
