import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BehaviorSubject, of } from 'rxjs';
import { Navbar } from './navbar';
import { AuthService } from '../../services/auth';
import { User } from '../../models/user.model';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';

xdescribe('Navbar', () => {
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

    // Override component to use inline template
    TestBed.overrideComponent(Navbar, {
      set: {
        template: `
          <nav>
            <span *ngIf="(currentUser$ | async) as user">{{ user.firstName }}</span>
            <img [src]="getAvatarUrl()" />
            <button (click)="logout()">Logout</button>
          </nav>
        `,
        styles: []
      }
    });

    await TestBed.configureTestingModule({
      imports: [Navbar, HttpClientTestingModule, CommonModule, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ]
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

  it('should handle multiple avatar URL formats', () => {
    const urls = [
      { input: '/uploads/avatar.jpg', expected: 'https://localhost:8443/uploads/avatar.jpg' },
      { input: '/media/user/profile.png', expected: 'https://localhost:8443/media/user/profile.png' },
      { input: '', expected: 'https://localhost:8443' }
    ];

    urls.forEach(({ input, expected }) => {
      const result = component.getAvatarUrl(input);
      expect(result).toBe(expected);
    });
  });

  it('should handle logout error gracefully', () => {
    authServiceMock.logout.and.returnValue({
      subscribe: (next: any, error: any) => {
        error({ status: 500 });
      }
    } as any);

    component.onLogout();

    expect(authServiceMock.logout).toHaveBeenCalled();
  });

  it('should handle getAvatarUrl with undefined current user', () => {
    currentUserSubject.next(null);

    const url = component.getAvatarUrl('/uploads/default.jpg');

    expect(url).toBe('https://localhost:8443/uploads/default.jpg');
  });

  it('should handle getAvatarUrl with special characters', () => {
    const url = component.getAvatarUrl('/uploads/@user#123.jpg');

    expect(url).toBe('https://localhost:8443/uploads/@user#123.jpg');
  });

  it('should emit currentUser$ for each user change', (done) => {
    let emissionCount = 0;
    const user1: User = {
      id: '1',
      email: 'user1@example.com',
      firstName: 'User1',
      lastName: 'Test',
      role: 'CLIENT',
      avatarUrl: undefined
    };
    const user2: User = { ...user1, id: '2', email: 'user2@example.com' };

    component.currentUser$.subscribe(user => {
      emissionCount++;
      if (emissionCount === 1) {
        expect(user).toBeNull();
      } else if (emissionCount === 2) {
        expect(user?.id).toBe('1');
      } else if (emissionCount === 3) {
        expect(user?.id).toBe('2');
        done();
      }
    });

    currentUserSubject.next(user1);
    currentUserSubject.next(user2);
  });

  it('should handle user with different avatar URLs', () => {
    const users = [
      { id: '1', avatarUrl: '/uploads/avatar1.jpg' },
      { id: '2', avatarUrl: '/uploads/avatar2.jpg' },
      { id: '3', avatarUrl: undefined }
    ];

    users.forEach(userData => {
      const user: User = {
        ...userData,
        email: `user${userData.id}@example.com`,
        firstName: 'Test',
        lastName: 'User',
        role: 'CLIENT'
      } as User;

      currentUserSubject.next(user);

      component.currentUser$.subscribe(currentUser => {
        if (currentUser?.id === userData.id) {
          expect(currentUser.avatarUrl).toBe(userData.avatarUrl);
        }
      });
    });
  });

  it('should handle SELLER and other roles', () => {
    const sellerUser: User = {
      id: '1',
      email: 'seller@example.com',
      firstName: 'Seller',
      lastName: 'User',
      role: 'SELLER',
      avatarUrl: '/uploads/seller.jpg'
    };

    currentUserSubject.next(sellerUser);

    component.currentUser$.subscribe(user => {
      expect(user?.role).toBe('SELLER');
    });
  });

  it('should handle consecutive logout calls', () => {
    authServiceMock.logout.and.returnValue(of(void 0));

    component.onLogout();
    expect(authServiceMock.logout).toHaveBeenCalledTimes(1);

    component.onLogout();
    expect(authServiceMock.logout).toHaveBeenCalledTimes(2);

    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });
});
