import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CookieService } from 'ngx-cookie-service';
import { AuthService } from './auth';
import { User } from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let cookieServiceMock: jasmine.SpyObj<CookieService>;

  // Mock user data
  const mockUser: User = {
    id: '123',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    role: 'CLIENT',
    avatarUrl: '/uploads/avatar.jpg'
  };

  const mockCredentials = {
    email: 'john@example.com',
    password: 'password123'
  };

  beforeEach(() => {
    // Create spy object for CookieService
    cookieServiceMock = jasmine.createSpyObj('CookieService', ['check', 'set', 'delete']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: CookieService, useValue: cookieServiceMock }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // Verify no outstanding HTTP requests
  });

  // ============ Service Creation ============
  describe('Service Creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should initialize with null currentUser', (done) => {
      service.currentUser$.subscribe(user => {
        expect(user).toBeNull();
        done();
      });
    });
  });

  // ============ Login Tests ============
  describe('login()', () => {
    it('should send login request with credentials', () => {
      service.login(mockCredentials).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockCredentials);
      expect(req.request.withCredentials).toBe(true);

      req.flush({ success: true });

      // login() triggers fetchCurrentUser, so we need to handle that request too
      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);
    });

    it('should fetch current user after successful login', (done) => {
      service.login(mockCredentials).subscribe();

      const loginReq = httpMock.expectOne('https://localhost:8443/api/auth/login');
      expect(loginReq.request.method).toBe('POST');
      loginReq.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(userReq.request.method).toBe('GET');
      userReq.flush(mockUser);

      service.currentUser$.subscribe(user => {
        expect(user).toEqual(mockUser);
        done();
      });
    });

    it('should update currentUser$ after successful login', (done) => {
      let testCompleted = false;

      service.login(mockCredentials).subscribe(() => {
        // Wait a tick for the fetchCurrentUser to complete
        setTimeout(() => {
          service.currentUser$.subscribe(user => {
            if (!testCompleted) {
              expect(user).toEqual(mockUser);
              testCompleted = true;
              done();
            }
          });
        }, 0);
      });

      const loginReq = httpMock.expectOne('https://localhost:8443/api/auth/login');
      loginReq.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);
    });

    it('should handle login error', () => {
      const errorMessage = 'Invalid credentials';

      service.login(mockCredentials).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.error).toEqual(errorMessage);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush(errorMessage, { status: 401, statusText: 'Unauthorized' });
    });
  });

  // ============ Logout Tests ============
  describe('logout()', () => {
    it('should send logout request', () => {
      service.logout().subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/logout');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);

      req.flush({ success: true });
    });

    it('should clear currentUser$ on logout', (done) => {
      // First set a user
      service.logout().subscribe(() => {
        service.currentUser$.subscribe(user => {
          expect(user).toBeNull();
          done();
        });
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/logout');
      req.flush({ success: true });
    });

    it('should handle logout error gracefully', () => {
      service.logout().subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/logout');
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  // ============ Fetch Current User Tests ============
  describe('fetchCurrentUser()', () => {
    it('should fetch user data from /me endpoint', () => {
      service.fetchCurrentUser().subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockUser);
    });

    it('should update currentUser$ with fetched data', (done) => {
      service.fetchCurrentUser().subscribe(user => {
        expect(user).toEqual(mockUser);
        service.currentUser$.subscribe(currentUser => {
          expect(currentUser).toEqual(mockUser);
          done();
        });
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);
    });

    it('should set currentUser$ to null on 401 error', (done) => {
      service.fetchCurrentUser().subscribe(
        () => fail('should have failed'),
        () => {
          service.currentUser$.subscribe(user => {
            expect(user).toBeNull();
            done();
          });
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should throw error on other HTTP errors', () => {
      service.fetchCurrentUser().subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  // ============ Init Tests ============
  describe('init()', () => {
    it('should fetch user on first call', () => {
      service.init().subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(req.request.method).toBe('GET');

      req.flush(mockUser);
    });

    it('should set currentUser$ on successful init', (done) => {
      service.init().subscribe(() => {
        service.currentUser$.subscribe(user => {
          expect(user).toEqual(mockUser);
          done();
        });
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);
    });

    it('should set currentUser$ to null on failed init', (done) => {
      service.init().subscribe(
        () => fail('should have failed'),
        () => {
          service.currentUser$.subscribe(user => {
            expect(user).toBeNull();
            done();
          });
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should not fetch user on subsequent calls', (done) => {
      // First call
      service.init().subscribe(() => {
        // After first call completes, make second call
        let secondCallEmitted = false;
        service.init().subscribe(() => {
          secondCallEmitted = true;
        });

        // Small delay to ensure no HTTP request is made
        setTimeout(() => {
          // Verify no additional HTTP request was made
          httpMock.expectNone('https://localhost:8443/api/users/me');
          // The second call should emit (returns currentUser$ observable)
          expect(secondCallEmitted).toBe(true);
          done();
        }, 100);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });

    it('should return currentUser$ observable on subsequent calls', (done) => {
      // First call
      service.init().subscribe(() => {
        // After first init completes, verify the cached state
        service.init().subscribe(cachedUser => {
          // Second call returns the observable, verify it has the cached user
          service.currentUser$.subscribe(user => {
            expect(user).toEqual(mockUser);
            done();
          });
        });
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);

      // No additional HTTP request should be made on second init call
      httpMock.expectNone('https://localhost:8443/api/users/me');
    });
  });

  // ============ Register Tests ============
  describe('register()', () => {
    it('should send register request with FormData', () => {
      const formData = new FormData();
      formData.append('email', mockCredentials.email);
      formData.append('password', mockCredentials.password);

      service.register(formData).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      expect(req.request.method).toBe('POST');

      req.flush({ success: true });
    });

    it('should handle registration success', () => {
      const formData = new FormData();
      const response = { success: true, message: 'User registered' };

      service.register(formData).subscribe(result => {
        expect(result).toEqual(response);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.flush(response);
    });

    it('should handle registration error', () => {
      const formData = new FormData();
      const errorMessage = 'Email already exists';

      service.register(formData).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.error).toEqual(errorMessage);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.flush(errorMessage, { status: 409, statusText: 'Conflict' });
    });
  });

  // ============ Current User Role Getter Tests ============
  describe('currentUserRole getter', () => {
    it('should return null when no user is logged in', () => {
      expect(service.currentUserRole).toBeNull();
    });

    it('should return user role when user is logged in', (done) => {
      service.fetchCurrentUser().subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);

      // Give observable time to emit
      setTimeout(() => {
        expect(service.currentUserRole).toBe('CLIENT');
        done();
      }, 100);
    });

    it('should return null after logout', (done) => {
      // First fetch a user
      service.fetchCurrentUser().subscribe();
      let req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);

      // Then logout
      service.logout().subscribe();
      req = httpMock.expectOne('https://localhost:8443/api/users/logout');
      req.flush({ success: true });

      setTimeout(() => {
        expect(service.currentUserRole).toBeNull();
        done();
      }, 100);
    });

    it('should handle different user roles', (done) => {
      const sellerUser = { ...mockUser, role: 'SELLER' };
      service.fetchCurrentUser().subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(sellerUser);

      setTimeout(() => {
        expect(service.currentUserRole).toBe('SELLER');
        done();
      }, 100);
    });

    it('should handle admin role', (done) => {
      const adminUser = { ...mockUser, role: 'ADMIN' };
      service.fetchCurrentUser().subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(adminUser);

      setTimeout(() => {
        expect(service.currentUserRole).toBe('ADMIN');
        done();
      }, 100);
    });
  });

  // ============ Edge Cases and Error Scenarios ============
  describe('Edge Cases and Error Scenarios', () => {
    it('should handle login with null credentials', () => {
      service.login(null as any).subscribe({
        next: () => { },
        error: (err) => {
          expect(err).toBeDefined();
        }
      });

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.error(new ErrorEvent('Network error'));
    });

    it('should handle fetchCurrentUser with 401 Unauthorized', () => {
      service.fetchCurrentUser().subscribe({
        next: () => { },
        error: (err) => {
          expect(err.status).toBe(401);
        }
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle fetchCurrentUser with 500 Server Error', () => {
      service.fetchCurrentUser().subscribe({
        next: () => { },
        error: (err) => {
          expect(err.status).toBe(500);
        }
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush({ message: 'Internal Server Error' }, { status: 500, statusText: 'Server Error' });
    });

    it('should handle logout with error', () => {
      service.logout().subscribe({
        next: () => { },
        error: (err) => {
          expect(err).toBeDefined();
        }
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/logout');
      req.error(new ErrorEvent('Network error'));
    });

    it('should emit currentUser$ when user data changes', (done) => {
      let emissionCount = 0;
      service.currentUser$.subscribe(user => {
        emissionCount++;
        if (emissionCount === 2) {
          expect(user).toEqual(mockUser);
          done();
        }
      });

      service.fetchCurrentUser().subscribe();
      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);
    });

    it('should preserve currentUser$ data after error in fetchCurrentUser', (done) => {
      // First, set a user
      service.fetchCurrentUser().subscribe();
      let req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);

      // Then try to fetch again with error
      setTimeout(() => {
        service.fetchCurrentUser().subscribe({
          error: () => {
            setTimeout(() => {
              service.currentUser$.subscribe(user => {
                expect(user).toEqual(mockUser);
                done();
              });
            }, 50);
          }
        });
        req = httpMock.expectOne('https://localhost:8443/api/users/me');
        req.flush({ message: 'Error' }, { status: 500, statusText: 'Server Error' });
      }, 100);
    });

    it('should handle multiple consecutive login attempts', () => {
      const firstLogin = mockCredentials;
      const secondLogin = { email: 'jane@example.com', password: 'password456' };

      service.login(firstLogin).subscribe();
      let req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);

      service.login(secondLogin).subscribe();
      req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      req = httpMock.expectOne('https://localhost:8443/api/users/me');
      const janeDoe = { ...mockUser, email: 'jane@example.com' };
      req.flush(janeDoe);

      service.currentUser$.subscribe(user => {
        if (user) {
          expect(user.email).toBe('jane@example.com');
        }
      });
    });

    it('should handle register with empty password', () => {
      const formData = new FormData();
      formData.append('email', 'test@example.com');
      formData.append('password', '');

      service.register(formData).subscribe({
        next: () => { },
        error: (err) => {
          expect(err).toBeDefined();
        }
      });

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.flush({ message: 'Password is required' }, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle fetchCurrentUser with malformed response', () => {
      service.fetchCurrentUser().subscribe({
        next: (user) => {
          expect(user).toBeTruthy();
        },
        error: () => { }
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush({ incomplete: 'data' });
    });
  });
});
