import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { SidenavComponent } from './sidenav';
import { AuthService } from '../../services/auth';
import { User } from '../../models/user.model';

describe('SidenavComponent', () => {
  let component: SidenavComponent;
  let fixture: ComponentFixture<SidenavComponent>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let router: Router;
  let currentUserSubject: BehaviorSubject<User | null>;

  beforeEach(async () => {
    currentUserSubject = new BehaviorSubject<User | null>(null);

    authServiceMock = jasmine.createSpyObj('AuthService', ['logout']);
    authServiceMock.logout.and.returnValue(of(void 0));
    Object.defineProperty(authServiceMock, 'currentUser$', {
      get: () => currentUserSubject.asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [
        SidenavComponent,
        HttpClientTestingModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        provideRouter([])  // ✅ FIX: Use provideRouter instead of RouterTestingModule + mock
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SidenavComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');  // Spy on the real router
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call isSeller and return role from authService', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'SELLER',
      configurable: true
    });

    const result = component.isSeller();
    expect(result).toBe(true);
  });

  it('should return false from isSeller when not SELLER', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'CLIENT',
      configurable: true
    });

    const result = component.isSeller();
    expect(result).toBe(false);
  });

  it('should call logout, emit closeSidenav, and navigate', () => {
    spyOn(component.closeSidenav, 'emit');

    component.logout();

    expect(authServiceMock.logout).toHaveBeenCalled();
    expect(component.closeSidenav.emit).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should handle isSeller with ADMIN role', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'ADMIN',
      configurable: true
    });

    const result = component.isSeller();
    expect(result).toBe(false);
  });

  it('should handle isSeller with null role', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => null,
      configurable: true
    });

    const result = component.isSeller();
    expect(result).toBe(false);
  });

  it('should handle multiple logout calls', () => {
    spyOn(component.closeSidenav, 'emit');

    component.logout();
    expect(authServiceMock.logout).toHaveBeenCalledTimes(1);

    component.logout();
    expect(authServiceMock.logout).toHaveBeenCalledTimes(2);
    expect(component.closeSidenav.emit).toHaveBeenCalledTimes(2);
  });

  it('should emit closeSidenav before navigation', (done) => {
    spyOn(component.closeSidenav, 'emit');

    component.closeSidenav.subscribe(() => {
      expect(router.navigate).not.toHaveBeenCalled();
      done();
    });

    component.logout();
  });

  it('should handle logout error', () => {
    authServiceMock.logout.and.returnValue({
      subscribe: (next: any, error: any) => {
        error({ status: 500 });
      }
    } as any);

    spyOn(component.closeSidenav, 'emit');
    component.logout();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });

  it('should return true from isSeller for exact SELLER string', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'SELLER',
      configurable: true
    });

    expect(component.isSeller()).toBe(true);
  });

  it('should return false from isSeller for case-sensitive roles', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'seller',
      configurable: true
    });

    expect(component.isSeller()).toBe(false);
  });

  it('should handle role changes dynamically', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'CLIENT',
      configurable: true
    });
    expect(component.isSeller()).toBe(false);

    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'SELLER',
      configurable: true
    });
    expect(component.isSeller()).toBe(true);
  });
});