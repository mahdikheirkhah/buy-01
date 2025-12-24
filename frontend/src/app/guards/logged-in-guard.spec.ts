import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';
import { LoggedInGuard } from './logged-in-guard';

describe('LoggedInGuard', () => {
  let guard: LoggedInGuard;
  let routerMock: jasmine.SpyObj<Router>;
  let cookieServiceMock: jasmine.SpyObj<CookieService>;

  beforeEach(() => {
    routerMock = jasmine.createSpyObj('Router', ['navigate']);
    cookieServiceMock = jasmine.createSpyObj('CookieService', ['check']);

    TestBed.configureTestingModule({
      providers: [
        LoggedInGuard,
        { provide: Router, useValue: routerMock },
        { provide: CookieService, useValue: cookieServiceMock }
      ]
    });

    guard = TestBed.inject(LoggedInGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should allow access when logged in', () => {
    cookieServiceMock.check.and.returnValue(true);
    expect(guard.canActivate()).toBe(true);
  });

  it('should redirect to login and deny access when not logged in', () => {
    cookieServiceMock.check.and.returnValue(false);
    expect(guard.canActivate()).toBe(false);
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });
});
