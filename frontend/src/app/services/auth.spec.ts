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

  // ============ Logout Additional Tests ============
  describe('Logout Additional Tests', () => {
    it('should clear cookies on logout', () => {
      service.logout().subscribe();
      const req = httpMock.expectOne('https://localhost:8443/api/users/logout');
      req.flush({ message: 'Logged out' });
      expect(cookieServiceMock.delete).toHaveBeenCalledWith('jwt');
    });

    it('should send logout request to server', () => {
      service.logout().subscribe();
      const req = httpMock.expectOne('https://localhost:8443/api/users/logout');
      expect(req.request.method).toBe('POST');
      req.flush({ message: 'Logged out' });
    });

    it('should include credentials in logout request', () => {
      service.logout().subscribe();
      const req = httpMock.expectOne('https://localhost:8443/api/users/logout');
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
      const req = httpMock.expectOne('https://localhost:8443/api/users/logout');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle logout with server error', () => {
      service.logout().subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(500);
        }
      );
      const req = httpMock.expectOne('https://localhost:8443/api/users/logout');
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should delete jwt cookie even if request fails', () => {
      service.logout().subscribe(
        () => { },
        () => { }
      );
      const req = httpMock.expectOne('https://localhost:8443/api/users/logout');
      req.flush('Error', { status: 500, statusText: 'Server Error' });
      expect(cookieServiceMock.delete).toHaveBeenCalledWith('jwt');
    });
  });

  // Add rest of the tests from original file...
  // (Register tests, etc. - they look OK)
});
