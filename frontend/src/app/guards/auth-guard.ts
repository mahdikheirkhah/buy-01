import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private cookieService: CookieService, private router: Router) {}

  canActivate(): boolean {
    const isLoggedIn = this.cookieService.check('jwt');

    if (isLoggedIn) {
      this.router.navigate(['/home']); // If logged in, redirect to home
      return false; // Prevent access to the login/register page
    }

    return true; // If not logged in, allow access
  }
}
