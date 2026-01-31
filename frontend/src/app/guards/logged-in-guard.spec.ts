import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';
import { LoggedInGuard } from './logged-in-guard';

describe('LoggedInGuard', () => {
  let guard: LoggedInGuard;
  let routerMock: jasmine.SpyObj<Router>;
  let cookieServiceMock: jasmine.SpyObj<CookieService>;
  let mockRoute: ActivatedRouteSnapshot;
  let mockState: RouterStateSnapshot;

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
    mockRoute = {} as ActivatedRouteSnapshot;
    mockState = { url: '/my-products' } as RouterStateSnapshot;
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

  it('should check for jwt cookie', () => {
    cookieServiceMock.check.and.returnValue(true);
    guard.canActivate();
    expect(cookieServiceMock.check).toHaveBeenCalledWith('jwt');
  });

  it('should prevent navigation multiple times when not logged in', () => {
    cookieServiceMock.check.and.returnValue(false);

    expect(guard.canActivate()).toBe(false);
    expect(guard.canActivate()).toBe(false);

    expect(routerMock.navigate).toHaveBeenCalledTimes(2);
  });

  it('should handle edge case of undefined cookie check', () => {
    cookieServiceMock.check.and.returnValue(false);
    const result = guard.canActivate();
    expect(result).toBe(false);
  });
});

