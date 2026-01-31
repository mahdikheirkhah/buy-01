import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';

@Injectable({
  providedIn: 'root'
})
export class LoggedInGuard implements CanActivate {

  constructor(private cookieService: CookieService, private router: Router) {}

  canActivate(): boolean {
    const isLoggedIn = this.cookieService.check('jwt');

    if (isLoggedIn) {
      return true; // If logged in, allow access
    }

    // If not logged in, redirect to login page
    this.router.navigate(['/login']);
    return false;
  }
}
