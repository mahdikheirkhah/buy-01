import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { RegisterComponent } from './register';
import { AuthService } from '../../services/auth';

describe('Register', () => {
  let component: RegisterComponent;

  beforeEach(async () => {
    const authServiceMock = jasmine.createSpyObj('AuthService', ['register']);
    authServiceMock.register.and.returnValue(of({}));
    const routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
