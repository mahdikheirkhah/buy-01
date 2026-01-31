import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';
import { AuthGuard } from './auth-guard';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let routerMock: jasmine.SpyObj<Router>;
  let cookieServiceMock: jasmine.SpyObj<CookieService>;
  let mockRoute: ActivatedRouteSnapshot;
  let mockState: RouterStateSnapshot;

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
    mockRoute = {} as ActivatedRouteSnapshot;
    mockState = { url: '/login' } as RouterStateSnapshot;
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

  it('should check for jwt cookie', () => {
    cookieServiceMock.check.and.returnValue(false);
    guard.canActivate();
    expect(cookieServiceMock.check).toHaveBeenCalledWith('jwt');
  });

  it('should handle multiple consecutive calls correctly', () => {
    cookieServiceMock.check.and.returnValue(true);

    expect(guard.canActivate()).toBe(false);
    expect(routerMock.navigate).toHaveBeenCalledTimes(1);

    expect(guard.canActivate()).toBe(false);
    expect(routerMock.navigate).toHaveBeenCalledTimes(2);
  });
});

