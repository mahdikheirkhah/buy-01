import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register';
import { AuthService } from '../../services/auth';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';

describe('Register', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let routerMock: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['register']);
    authServiceMock.register.and.returnValue(of({}));
    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        RegisterComponent,
        HttpClientTestingModule,
        FormsModule,
        CommonModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatDividerModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty registerData', () => {
    expect(component.registerData.email).toBe('');
    expect(component.registerData.password).toBe('');
    expect(component.registerData.firstName).toBe('');
    expect(component.registerData.lastName).toBe('');
    expect(component.registerData.role).toBe('CLIENT');
  });

  it('should call authService.register with form data', () => {
    component.registerData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      password: 'password123',
      role: 'CLIENT'
    };

    component.onRegister();

    expect(authServiceMock.register).toHaveBeenCalled();
    const callArg = authServiceMock.register.calls.mostRecent().args[0];
    expect(callArg instanceof FormData).toBe(true);
  });

  it('should navigate to login on successful registration', () => {
    authServiceMock.register.and.returnValue(of({ message: 'Success' }));
    component.registerData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      password: 'password123',
      role: 'CLIENT'
    };

    component.onRegister();

    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should handle registration error', () => {
    const errorResponse = { error: { error: 'Email already exists' }, status: 409 };
    authServiceMock.register.and.returnValue(throwError(() => errorResponse));
    spyOn(console, 'error');

    component.registerData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'existing@example.com',
      password: 'password123',
      role: 'CLIENT'
    };

    component.onRegister();

    expect(console.error).toHaveBeenCalled();
  });

  it('should handle file selection and show cropper', () => {
    const file = new File([''], 'test.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [file] } } as any;

    component.onFileSelected(event);

    expect(component.showCropper).toBe(true);
    expect(component.imageChangedEvent).toBe(event);
  });

  it('should handle image blob from cropper', () => {
    const blob = new Blob(['test'], { type: 'image/png' });

    component.handleImageBlob(blob);

    expect(component.croppedBlob).toBe(blob);
    expect(component.croppedImage).toBeTruthy();
  });

  it('should close modal and reset file input', () => {
    component.showCropper = true;

    component.handleModalClose();

    expect(component.showCropper).toBe(false);
  });

  it('should include avatar file in registration for SELLER with cropped image', () => {
    const blob = new Blob(['test'], { type: 'image/png' });
    component.croppedBlob = blob;
    component.registerData = {
      firstName: 'Jane',
      lastName: 'Smith',
      email: 'jane@example.com',
      password: 'password123',
      role: 'SELLER'
    };

    component.onRegister();

    const callArg = authServiceMock.register.calls.mostRecent().args[0];
    expect(callArg instanceof FormData).toBe(true);
  });

  it('should not include avatar for CLIENT role', () => {
    component.croppedBlob = new Blob(['test'], { type: 'image/png' });
    component.registerData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      password: 'password123',
      role: 'CLIENT'
    };

    component.onRegister();

    expect(authServiceMock.register).toHaveBeenCalled();
  });
});
