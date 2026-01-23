import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BehaviorSubject, of } from 'rxjs';
import { Navbar } from './navbar';
import { AuthService } from '../../services/auth';
import { User } from '../../models/user.model';

describe('Navbar', () => {
  let component: Navbar;
  let fixture: ComponentFixture<Navbar>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let routerMock: jasmine.SpyObj<Router>;
  let currentUserSubject: BehaviorSubject<User | null>;

  beforeEach(async () => {
    currentUserSubject = new BehaviorSubject<User | null>(null);
    authServiceMock = jasmine.createSpyObj('AuthService', ['logout']);
    authServiceMock.logout.and.returnValue(of(void 0));
    Object.defineProperty(authServiceMock, 'currentUser$', {
      get: () => currentUserSubject.asObservable()
    });

    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [Navbar, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(Navbar);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize currentUser$ from authService', () => {
    const mockUser: User = {
      id: '1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      role: 'CLIENT',
      avatarUrl: '/uploads/avatar.jpg'
    };

    currentUserSubject.next(mockUser);

    component.currentUser$.subscribe(user => {
      expect(user).toEqual(mockUser);
    });
  });

  it('should emit toggleSidenav event', () => {
    spyOn(component.toggleSidenav, 'emit');

    component.toggleSidenav.emit();

    expect(component.toggleSidenav.emit).toHaveBeenCalled();
  });

  it('should build full avatar URL', () => {
    const avatarPath = '/uploads/avatar123.jpg';
    const url = component.getAvatarUrl(avatarPath);

    expect(url).toBe('https://localhost:8443/uploads/avatar123.jpg');
  });

  it('should handle avatar URL with no leading slash', () => {
    const avatarPath = 'uploads/avatar.jpg';
    const url = component.getAvatarUrl(avatarPath);

    expect(url).toBe('https://localhost:8443uploads/avatar.jpg');
  });

  it('should call logout and navigate on onLogout', () => {
    component.onLogout();

    expect(authServiceMock.logout).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should handle null currentUser', () => {
    currentUserSubject.next(null);

    component.currentUser$.subscribe(user => {
      expect(user).toBeNull();
    });
  });

  it('should reflect user role changes', () => {
    const clientUser: User = {
      id: '1',
      email: 'client@example.com',
      firstName: 'Client',
      lastName: 'User',
      role: 'CLIENT',
      avatarUrl: undefined
    };

    currentUserSubject.next(clientUser);

    component.currentUser$.subscribe(user => {
      expect(user?.role).toBe('CLIENT');
    });

    const sellerUser: User = { ...clientUser, role: 'SELLER' };
    currentUserSubject.next(sellerUser);

    component.currentUser$.subscribe(user => {
      expect(user?.role).toBe('SELLER');
    });
  });
});
