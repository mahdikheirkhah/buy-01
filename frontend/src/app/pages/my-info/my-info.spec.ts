import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { MyInfo } from './my-info';
import { AuthService } from '../../services/auth';
import { UserService } from '../../services/user';

describe('MyInfo', () => {
  let component: MyInfo;

  beforeEach(async () => {
    const authServiceMock = jasmine.createSpyObj('AuthService', ['logout']);
    authServiceMock.currentUser$ = of(null);

    const userServiceMock = jasmine.createSpyObj('UserService', ['updateUser', 'updateAvatar', 'deleteAvatar', 'deleteUser']);

    await TestBed.configureTestingModule({
      imports: [MyInfo, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: UserService, useValue: userServiceMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(MyInfo);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
