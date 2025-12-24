import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UserService } from './user';
import { AuthService } from './auth';
import { UpdateUserDTO } from '../models/update-user.dto';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  let authServiceMock: jasmine.SpyObj<AuthService>;

  // Mock data
  const mockUpdateUserDTO: UpdateUserDTO = {
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com'
  };

  const mockUpdatedUser = {
    id: 'user-123',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    role: 'CLIENT',
    avatarUrl: '/uploads/avatar.jpg'
  };

  beforeEach(() => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    authServiceMock.fetchCurrentUser.and.returnValue({
      subscribe: jasmine.createSpy('subscribe').and.returnValue({})
    } as any);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        UserService,
        { provide: AuthService, useValue: authServiceMock }
      ]
    });

    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ============ Service Creation ============
  describe('Service Creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  // ============ Delete User Tests ============
  describe('deleteUser()', () => {
    it('should send DELETE request with password parameter', () => {
      service.deleteUser('password123').subscribe();

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users' &&
        request.params.get('password') === 'password123'
      );
      expect(req.request.method).toBe('DELETE');
      expect(req.request.withCredentials).toBe(true);

      req.flush({ message: 'User deleted successfully' });
    });

    it('should include password in query parameters', () => {
      service.deleteUser('mySecurePassword').subscribe();

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      expect(req.request.params.get('password')).toBe('mySecurePassword');

      req.flush({ message: 'User deleted' });
    });

    it('should return success response on delete', () => {
      const response = { message: 'User deleted successfully' };

      service.deleteUser('password123').subscribe(result => {
        expect(result.message).toBe('User deleted successfully');
      });

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      req.flush(response);
    });

    it('should handle delete error with wrong password', () => {
      service.deleteUser('wrongPassword').subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(401);
        }
      );

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      req.flush('Invalid password', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle delete error when user not found', () => {
      service.deleteUser('password123').subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(404);
        }
      );

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      req.flush('User not found', { status: 404, statusText: 'Not Found' });
    });
  });

  // ============ Delete Avatar Tests ============
  describe('deleteAvatar()', () => {
    it('should send DELETE request to avatar endpoint', () => {
      service.deleteAvatar().subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/avatar');
      expect(req.request.method).toBe('DELETE');
      expect(req.request.withCredentials).toBe(true);

      req.flush('Avatar deleted');
    });

    it('should return text response', () => {
      service.deleteAvatar().subscribe(result => {
        expect(result).toBe('Avatar deleted successfully');
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/avatar');
      req.flush('Avatar deleted successfully');
    });

    it('should handle delete avatar error', () => {
      service.deleteAvatar().subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(404);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/avatar');
      req.flush('Avatar not found', { status: 404, statusText: 'Not Found' });
    });

    it('should handle unauthorized delete avatar attempt', () => {
      service.deleteAvatar().subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(401);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/avatar');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });
  });

  // ============ Update Avatar Tests ============
  describe('updateAvatar()', () => {
    it('should send POST request with FormData containing avatar file', () => {
      const mockFile = new File(['avatar-content'], 'avatar.jpg', { type: 'image/jpeg' });

      service.updateAvatar(mockFile).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/newAvatar');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockUpdatedUser);
    });

    it('should include avatarFile in FormData', () => {
      const mockFile = new File(['avatar-content'], 'avatar.jpg', { type: 'image/jpeg' });

      service.updateAvatar(mockFile).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/newAvatar');
      const formData = req.request.body as FormData;

      expect(formData.has('avatarFile')).toBe(true);

      req.flush(mockUpdatedUser);
    });

    it('should return updated user on successful upload', () => {
      const mockFile = new File(['avatar-content'], 'avatar.jpg');

      service.updateAvatar(mockFile).subscribe(result => {
        expect(result).toEqual(mockUpdatedUser);
        expect(result.avatarUrl).toBe('/uploads/avatar.jpg');
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/newAvatar');
      req.flush(mockUpdatedUser);
    });

    it('should handle avatar upload error', () => {
      const mockFile = new File(['avatar-content'], 'avatar.jpg');

      service.updateAvatar(mockFile).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(400);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/newAvatar');
      req.flush('Invalid file format', { status: 400, statusText: 'Bad Request' });
    });

    it('should handle file too large error', () => {
      const mockFile = new File(['large-avatar-content'], 'avatar.jpg');

      service.updateAvatar(mockFile).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(413);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/newAvatar');
      req.flush('File too large', { status: 413, statusText: 'Payload Too Large' });
    });
  });

  // ============ Update User Tests ============
  describe('updateUser()', () => {
    it('should send PUT request with user data', () => {
      service.updateUser(mockUpdateUserDTO).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(mockUpdateUserDTO);
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockUpdatedUser);
    });

    it('should return updated user data', () => {
      service.updateUser(mockUpdateUserDTO).subscribe(result => {
        expect(result).toEqual(mockUpdatedUser);
        expect(result.firstName).toBe('John');
        expect(result.lastName).toBe('Doe');
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUpdatedUser);
    });

    it('should call fetchCurrentUser after successful update', () => {
      service.updateUser(mockUpdateUserDTO).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUpdatedUser);

      // Verify fetchCurrentUser was called
      expect(authServiceMock.fetchCurrentUser).toHaveBeenCalled();
    });

    it('should update only provided fields', () => {
      const partialUpdate: UpdateUserDTO = {
        firstName: 'Jane'
      };

      service.updateUser(partialUpdate).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(req.request.body).toEqual(partialUpdate);

      req.flush(mockUpdatedUser);
    });

    it('should handle validation error', () => {
      const invalidUpdate: UpdateUserDTO = {
        email: 'invalid-email'
      };

      service.updateUser(invalidUpdate).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(400);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Invalid email format', { status: 400, statusText: 'Bad Request' });
    });

    it('should handle conflict error when email exists', () => {
      const updateWithExistingEmail: UpdateUserDTO = {
        email: 'existing@example.com'
      };

      service.updateUser(updateWithExistingEmail).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(409);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Email already exists', { status: 409, statusText: 'Conflict' });
    });

    it('should handle unauthorized update attempt', () => {
      service.updateUser(mockUpdateUserDTO).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(401);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle password change in update', () => {
      const passwordUpdate: UpdateUserDTO = {
        currentPassword: 'oldPassword',
        newPassword: 'newPassword'
      };

      service.updateUser(passwordUpdate).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(req.request.body).toEqual(passwordUpdate);

      req.flush(mockUpdatedUser);
    });

    it('should handle wrong current password error', () => {
      const passwordUpdate: UpdateUserDTO = {
        currentPassword: 'wrongPassword',
        newPassword: 'newPassword'
      };

      service.updateUser(passwordUpdate).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(401);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Current password is incorrect', { status: 401, statusText: 'Unauthorized' });
    });
  });
});

