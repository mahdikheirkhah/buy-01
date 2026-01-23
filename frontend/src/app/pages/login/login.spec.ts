import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login';
import { AuthService } from '../../services/auth';
import { FormsModule } from '@angular/forms';

describe('Login', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let routerMock: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['login', 'fetchCurrentUser']);
    authServiceMock.login.and.returnValue(of({}));
    authServiceMock.fetchCurrentUser.and.returnValue(of({ role: 'CLIENT', email: 'test@example.com' } as any));
    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, HttpClientTestingModule, FormsModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty loginData', () => {
    expect(component.loginData.email).toBe('');
    expect(component.loginData.password).toBe('');
  });

  it('should call authService.login with loginData', () => {
    component.loginData = {
      email: 'test@example.com',
      password: 'password123'
    };

    component.onLogin();

    expect(authServiceMock.login).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: 'password123'
    });
  });

  it('should navigate to /home for CLIENT role', () => {
    authServiceMock.fetchCurrentUser.and.returnValue(of({ role: 'CLIENT', email: 'test@example.com' } as any));
    component.loginData = { email: 'client@example.com', password: 'password' };

    component.onLogin();

    expect(routerMock.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('should navigate to /seller-dashboard for SELLER role', () => {
    authServiceMock.fetchCurrentUser.and.returnValue(of({ role: 'SELLER', email: 'seller@example.com' } as any));
    component.loginData = { email: 'seller@example.com', password: 'password' };

    component.onLogin();

    expect(routerMock.navigate).toHaveBeenCalledWith(['/seller-dashboard']);
  });

  it('should handle login error', () => {
    const errorResponse = { error: { error: 'Invalid credentials' }, status: 401 };
    authServiceMock.login.and.returnValue(throwError(() => errorResponse));
    spyOn(console, 'error');

    component.loginData = { email: 'wrong@example.com', password: 'wrongpass' };

    component.onLogin();

    expect(console.error).toHaveBeenCalledWith('Login failed', errorResponse);
  });

  it('should handle fetchCurrentUser error and navigate to /home', () => {
    authServiceMock.fetchCurrentUser.and.returnValue(throwError(() => ({ status: 500 })));
    spyOn(console, 'error');

    component.loginData = { email: 'test@example.com', password: 'password' };

    component.onLogin();

    expect(console.error).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('should navigate to /home for unknown role', () => {
    authServiceMock.fetchCurrentUser.and.returnValue(of({ role: 'UNKNOWN', email: 'test@example.com' } as any));
    component.loginData = { email: 'test@example.com', password: 'password' };

    component.onLogin();

    expect(routerMock.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('should call both login and fetchCurrentUser in sequence', () => {
    component.loginData = { email: 'test@example.com', password: 'password' };

    component.onLogin();

    expect(authServiceMock.login).toHaveBeenCalled();
    expect(authServiceMock.fetchCurrentUser).toHaveBeenCalled();
  });
});

