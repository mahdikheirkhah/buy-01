import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';
import { AuthGuard } from './auth-guard';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let routerMock: jasmine.SpyObj<Router>;
  let cookieServiceMock: jasmine.SpyObj<CookieService>;

  beforeEach(() => {
    routerMock = jasmine.createSpyObj('Router', ['navigate']);
    cookieServiceMock = jasmine.createSpyObj('CookieService', ['check']);

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: Router, useValue: routerMock },
        { provide: CookieService, useValue: cookieServiceMock }
      ]
    });

    guard = TestBed.inject(AuthGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should allow access when not logged in', () => {
    cookieServiceMock.check.and.returnValue(false);
    expect(guard.canActivate()).toBe(true);
  });

  it('should redirect to home and deny access when already logged in', () => {
    cookieServiceMock.check.and.returnValue(true);
    expect(guard.canActivate()).toBe(false);
    expect(routerMock.navigate).toHaveBeenCalledWith(['/home']);
  });
});
