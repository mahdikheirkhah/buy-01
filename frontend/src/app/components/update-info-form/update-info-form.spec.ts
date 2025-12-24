import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UpdateInfoForm } from './update-info-form';
import { UserService } from '../../services/user';

describe('UpdateInfoForm', () => {
  let component: UpdateInfoForm;

  beforeEach(async () => {
    const userServiceMock = jasmine.createSpyObj('UserService', ['updateUser']);

    await TestBed.configureTestingModule({
      imports: [UpdateInfoForm, HttpClientTestingModule],
      providers: [
        { provide: UserService, useValue: userServiceMock },
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: {} }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(UpdateInfoForm);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
