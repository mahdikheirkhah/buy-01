import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { MainLayout } from './main-layout';
import { AuthService } from '../../services/auth';

describe('MainLayout', () => {
  let component: MainLayout;

  beforeEach(async () => {
    const authServiceMock = jasmine.createSpyObj('AuthService', ['init']);
    authServiceMock.currentUser$ = of(null);
    authServiceMock.init.and.returnValue(of({}));

    await TestBed.configureTestingModule({
      imports: [MainLayout, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(MainLayout);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
