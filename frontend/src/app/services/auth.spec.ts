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

    it('should handle login with very long email', () => {
      const longEmailLogin = {
        email: 'a'.repeat(200) + '@example.com',
        password: 'password123'
      };

      service.login(longEmailLogin).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      expect(req.request.body.email).toBe(longEmailLogin.email);
      req.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);
    });

    it('should handle login with special characters in email', () => {
      const specialEmailLogin = {
        email: 'test+tag@example.co.uk',
        password: 'password123'
      };

      service.login(specialEmailLogin).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      expect(req.request.body.email).toBe(specialEmailLogin.email);
      req.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);
    });

    it('should handle login with very long password', () => {
      const longPasswordLogin = {
        email: 'john@example.com',
        password: 'a'.repeat(500)
      };

      service.login(longPasswordLogin).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      expect(req.request.body.password.length).toBe(500);
      req.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);
    });

    it('should handle login with special password characters', () => {
      const specialPasswordLogin = {
        email: 'john@example.com',
        password: 'P@$$w0rd!#%&*()'
      };

      service.login(specialPasswordLogin).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      expect(req.request.body.password).toBe(specialPasswordLogin.password);
      req.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);
    });

    it('should handle server returning different user after login', () => {
      const differentUser = { ...mockUser, id: '456', firstName: 'Jane' };

      service.login(mockCredentials).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(differentUser);

      service.currentUser$.subscribe(user => {
        if (user) {
          expect(user.firstName).toBe('Jane');
        }
      });
    });

    it('should handle login network timeout', () => {
      service.login(mockCredentials).subscribe(
        () => fail('should have timed out'),
        (error) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.error(new ProgressEvent('Network timeout'));
    });

    it('should include credentials in login request', () => {
      service.login(mockCredentials).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      expect(req.request.withCredentials).toBe(true);
      req.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);
    });
  });

  describe('Logout Additional Tests', () => {
    it('should clear cookies on logout', () => {
      service.logout().subscribe();

      expect(cookieServiceMock.delete).toHaveBeenCalledWith('jwt');
    });

    it('should send logout request to server', () => {
      service.logout().subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/logout');
      expect(req.request.method).toBe('POST');
      req.flush({ message: 'Logged out' });
    });

    it('should include credentials in logout request', () => {
      service.logout().subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/logout');
      expect(req.request.withCredentials).toBe(true);
      req.flush({ message: 'Logged out' });
    });

    it('should handle logout error gracefully', () => {
      service.logout().subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(401);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/logout');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle logout with server error', () => {
      service.logout().subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/logout');
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should delete jwt cookie even if request fails', () => {
      service.logout().subscribe(
        () => { },
        () => { }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/logout');
      req.flush('Error', { status: 500, statusText: 'Server Error' });

      expect(cookieServiceMock.delete).toHaveBeenCalledWith('jwt');
    });
  });

  describe('Register Additional Tests', () => {
    it('should handle register with special characters', () => {
      const formData = new FormData();
      formData.append('email', 'test+special@example.com');
      formData.append('password', 'P@$$w0rd!');
      formData.append('firstName', 'Jöhn');

      service.register(formData).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      expect(req.request.method).toBe('POST');
      req.flush({ success: true });
    });

    it('should handle register with duplicate email', () => {
      const formData = new FormData();
      formData.append('email', 'john@example.com');
      formData.append('password', 'password123');

      service.register(formData).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(409);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.flush('Email already exists', { status: 409, statusText: 'Conflict' });
    });

    it('should handle register with invalid email format', () => {
      const formData = new FormData();
      formData.append('email', 'invalid-email');
      formData.append('password', 'password123');

      service.register(formData).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(400);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.flush('Invalid email format', { status: 400, statusText: 'Bad Request' });
    });

    it('should handle register with short password', () => {
      const formData = new FormData();
      formData.append('email', 'test@example.com');
      formData.append('password', '123');

      service.register(formData).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(400);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.flush('Password too short', { status: 400, statusText: 'Bad Request' });
    });

    it('should include credentials in register request', () => {
      const formData = new FormData();
      formData.append('email', 'test@example.com');
      formData.append('password', 'password123');

      service.register(formData).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      expect(req.request.withCredentials).toBe(true);
      req.flush({ success: true });
    });

    it('should handle register server error', () => {
      const formData = new FormData();
      formData.append('email', 'test@example.com');
      formData.append('password', 'password123');

      service.register(formData).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });;

  describe('Password Reset Tests', () => {
    it('should handle register server error', () => {
      const formData = new FormData();
      formData.append('email', 'test@example.com');
      formData.append('password', 'password123');

      service.register(formData).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('Current User Observable Tests', () => {
    it('should emit currentUser when set', (done) => {
      service.currentUser$.subscribe(user => {
        if (user) {
          expect(user.id).toBe('123');
          done();
        }
      });

      service.login(mockCredentials).subscribe();

      const loginReq = httpMock.expectOne('https://localhost:8443/api/auth/login');
      loginReq.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);
    });

    it('should emit multiple times on different logins', (done) => {
      let emitCount = 0;

      service.currentUser$.subscribe(user => {
        if (user) {
          emitCount++;
          if (emitCount === 2) {
            done();
          }
        }
      });

      // First login
      service.login(mockCredentials).subscribe();
      let loginReq = httpMock.expectOne('https://localhost:8443/api/auth/login');
      loginReq.flush({ success: true });

      let userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);

      // Second login
      service.login(mockCredentials).subscribe();
      loginReq = httpMock.expectOne('https://localhost:8443/api/auth/login');
      loginReq.flush({ success: true });

      userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush({ ...mockUser, id: '456' });
    });
  });

  describe('Token Handling Tests', () => {
    it('should handle login with token in response', () => {
      service.login(mockCredentials).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({
        success: true,
        token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
      });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);
    });

    it('should handle expired token on fetchCurrentUser', () => {
      service.fetchCurrentUser().subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(401);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Token expired', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle invalid token format', () => {
      service.fetchCurrentUser().subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(401);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Invalid token', { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('Concurrent Auth Operations Tests', () => {
    it('should handle concurrent login and logout', () => {
      service.login(mockCredentials).subscribe();

      const loginReq = httpMock.expectOne('https://localhost:8443/api/auth/login');
      loginReq.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);

      service.logout().subscribe();

      const logoutReq = httpMock.expectOne('https://localhost:8443/api/auth/logout');
      logoutReq.flush({ message: 'Logged out' });

      expect(cookieServiceMock.delete).toHaveBeenCalledWith('jwt');
    });

    it('should handle concurrent login attempts', () => {
      service.login(mockCredentials).subscribe();
      service.login(mockCredentials).subscribe();

      let req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);

      req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);
    });

    it('should handle fetch current user multiple times', () => {
      service.fetchCurrentUser().subscribe(user => {
        expect(user).toEqual(mockUser);
      });

      service.fetchCurrentUser().subscribe(user => {
        expect(user).toEqual(mockUser);
      });

      let req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);

      req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);
    });

    it('should handle rapid successive logins with different users', () => {
      const user1 = { ...mockUser, id: '1', email: 'user1@example.com' };
      const user2 = { ...mockUser, id: '2', email: 'user2@example.com' };

      service.login({ email: 'user1@example.com', password: 'password1' }).subscribe();
      service.login({ email: 'user2@example.com', password: 'password2' }).subscribe();

      let req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(user1);

      req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(user2);
    });

    it('should handle login then immediate logout', () => {
      service.login(mockCredentials).subscribe();

      let req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);

      service.logout().subscribe();

      req = httpMock.expectOne('https://localhost:8443/api/auth/logout');
      req.flush({ message: 'Logged out' });

      expect(cookieServiceMock.delete).toHaveBeenCalledWith('jwt');
    });

    it('should handle logout during login request', () => {
      service.login(mockCredentials).subscribe();
      service.logout().subscribe();

      let req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUser);

      req = httpMock.expectOne('https://localhost:8443/api/auth/logout');
      req.flush({ message: 'Logged out' });
    });
  });

  describe('Edge Case Token Handling Tests', () => {
    it('should handle login with expired token cookie', () => {
      cookieServiceMock.check.and.returnValue(true);

      service.login(mockCredentials).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);
    });

    it('should handle login cookie setting', () => {
      service.login(mockCredentials).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ success: true });

      const userReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      userReq.flush(mockUser);

      expect(cookieServiceMock.set).toHaveBeenCalled();
    });

    it('should handle register with email already in use', () => {
      const registerData = {
        email: 'john@example.com',
        password: 'password123',
        firstName: 'John',
        lastName: 'Doe'
      };

      service.register(registerData).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(409);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.flush({ error: 'Email already in use' }, { status: 409, statusText: 'Conflict' });
    });

    it('should handle register with weak password validation', () => {
      const registerData = {
        email: 'new@example.com',
        password: 'weak',
        firstName: 'John',
        lastName: 'Doe'
      };

      service.register(registerData).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(400);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.flush({ error: 'Password too weak' }, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle login with missing email', () => {
      const invalidCredentials = { email: '', password: 'password123' };

      service.login(invalidCredentials).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ error: 'Email required' }, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle login with missing password', () => {
      const invalidCredentials = { email: 'john@example.com', password: '' };

      service.login(invalidCredentials).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush({ error: 'Password required' }, { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('Network Error Handling Tests', () => {
    it('should handle connection refused on login', () => {
      service.login(mockCredentials).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.error(new ProgressEvent('Connection refused'));
    });

    it('should handle timeout on register', () => {
      const registerData = {
        email: 'new@example.com',
        password: 'password123',
        firstName: 'John',
        lastName: 'Doe'
      };

      service.register(registerData).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/register');
      req.error(new ProgressEvent('Timeout'));
    });

    it('should handle CORS error on logout', () => {
      service.logout().subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/logout');
      req.error(new ProgressEvent('CORS error'));
    });

    it('should handle server error 500 on fetchCurrentUser', () => {
      service.fetchCurrentUser().subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Internal server error', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should handle server error 503 on login', () => {
      service.login(mockCredentials).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(503);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/auth/login');
      req.flush('Service unavailable', { status: 503, statusText: 'Service Unavailable' });
    });
  });
});
