import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { AuthLayout } from './auth-layout';

describe('AuthLayout', () => {
  let component: AuthLayout;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthLayout],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(AuthLayout);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
