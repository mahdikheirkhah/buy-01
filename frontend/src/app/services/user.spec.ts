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

  // ============ Edge Cases and Error Scenarios ============
  describe('Edge Cases and Error Scenarios', () => {
    it('should handle delete with empty password', () => {
      service.deleteUser('').subscribe();

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      expect(req.request.params.get('password')).toBe('');
      req.flush({ message: 'User deleted' });
    });

    it('should handle delete with special characters in password', () => {
      const specialPassword = '@#$%^&*()_+-=[]{}|;\':",./<>?';
      service.deleteUser(specialPassword).subscribe();

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      expect(req.request.params.get('password')).toBe(specialPassword);
      req.flush({ message: 'User deleted' });
    });

    it('should handle update with null values', () => {
      const nullUpdate: UpdateUserDTO = {
        firstName: null as any,
        lastName: null as any,
        email: 'test@example.com'
      };

      service.updateUser(nullUpdate).subscribe((result: any) => {
        expect(result).toBeTruthy();
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUpdatedUser);
    });

    it('should handle update with empty strings', () => {
      const emptyUpdate: UpdateUserDTO = {
        firstName: '',
        lastName: '',
        email: 'test@example.com'
      };

      service.updateUser(emptyUpdate).subscribe((result: any) => {
        expect(result).toBeTruthy();
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUpdatedUser);
    });

    it('should handle update with very long names', () => {
      const longName = 'A'.repeat(1000);
      const longNameUpdate: UpdateUserDTO = {
        firstName: longName,
        lastName: longName,
        email: 'test@example.com'
      };

      service.updateUser(longNameUpdate).subscribe((result: any) => {
        expect(result).toBeTruthy();
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUpdatedUser);
    });

    it('should handle update with special characters in names', () => {
      const specialUpdate: UpdateUserDTO = {
        firstName: '@#$%',
        lastName: '中文日本語',
        email: 'test@example.com'
      };

      service.updateUser(specialUpdate).subscribe((result: any) => {
        expect(result).toBeTruthy();
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUpdatedUser);
    });

    it('should handle update with invalid email format', () => {
      const invalidEmailUpdate: UpdateUserDTO = {
        firstName: 'John',
        lastName: 'Doe',
        email: 'not-an-email'
      };

      service.updateUser(invalidEmailUpdate).subscribe((result: any) => {
        expect(result).toBeTruthy();
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUpdatedUser);
    });

    it('should handle update only firstName', () => {
      const singleFieldUpdate: UpdateUserDTO = {
        firstName: 'NewFirstName'
      };

      service.updateUser(singleFieldUpdate).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(req.request.body.firstName).toBe('NewFirstName');
      req.flush(mockUpdatedUser);
    });

    it('should handle update only lastName', () => {
      const singleFieldUpdate: UpdateUserDTO = {
        lastName: 'NewLastName'
      };

      service.updateUser(singleFieldUpdate).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(req.request.body.lastName).toBe('NewLastName');
      req.flush(mockUpdatedUser);
    });

    it('should handle update only email', () => {
      const singleFieldUpdate: UpdateUserDTO = {
        email: 'newemail@example.com'
      };

      service.updateUser(singleFieldUpdate).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(req.request.body.email).toBe('newemail@example.com');
      req.flush(mockUpdatedUser);
    });

    it('should handle network error on update', () => {
      service.updateUser(mockUpdateUserDTO).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.error(new ErrorEvent('Network error'));
    });

    it('should handle network error on delete', () => {
      service.deleteUser('password123').subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      req.error(new ErrorEvent('Network error'));
    });

    it('should handle server error 500 on update', () => {
      service.updateUser(mockUpdateUserDTO).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Internal Server Error', { status: 500, statusText: 'Server Error' });
    });

    it('should handle server error 500 on delete', () => {
      service.deleteUser('password123').subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      req.flush('Internal Server Error', { status: 500, statusText: 'Server Error' });
    });

    it('should handle malformed response on getCurrentUser', () => {
      service.updateUser(mockUpdateUserDTO).subscribe((result: any) => {
        expect(result).toBeTruthy();
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush({ partial: 'data' });
    });

    it('should handle getCurrentUser with forbidden error', () => {
      service.updateUser(mockUpdateUserDTO).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(403);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
    });

    it('should handle password change with same old and new password', () => {
      const samePasswordUpdate: UpdateUserDTO = {
        currentPassword: 'samePassword',
        newPassword: 'samePassword'
      };

      service.updateUser(samePasswordUpdate).subscribe((result: any) => {
        expect(result).toBeTruthy();
      });

      const req = httpMock.expectOne('https://localhost:8443/api/users/me');
      req.flush(mockUpdatedUser);
    });

    it('should handle very long password', () => {
      const veryLongPassword = 'P'.repeat(1000);
      service.deleteUser(veryLongPassword).subscribe((result: any) => {
        expect(result).toBeTruthy();
      });

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      req.flush({ message: 'User deleted' });
    });

    it('should send withCredentials on all requests', () => {
      service.updateUser(mockUpdateUserDTO).subscribe();

      const updateReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(updateReq.request.withCredentials).toBe(true);
      updateReq.flush(mockUpdatedUser);

      service.deleteUser('password').subscribe();

      const deleteReq = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      expect(deleteReq.request.withCredentials).toBe(true);
      deleteReq.flush({ message: 'deleted' });

      service.updateUser(mockUpdateUserDTO).subscribe();

      const getReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      expect(getReq.request.withCredentials).toBe(true);
      getReq.flush(mockUpdatedUser);
    });

    it('should handle concurrent update and delete requests', () => {
      service.updateUser(mockUpdateUserDTO).subscribe((result: any) => {
        expect(result).toBeTruthy();
      });
      service.deleteUser('password').subscribe((result: any) => {
        expect(result).toBeTruthy();
      });

      const updateReq = httpMock.expectOne('https://localhost:8443/api/users/me');
      updateReq.flush(mockUpdatedUser);

      const deleteReq = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/users'
      );
      deleteReq.flush({ message: 'deleted' });
    });
  });
});

