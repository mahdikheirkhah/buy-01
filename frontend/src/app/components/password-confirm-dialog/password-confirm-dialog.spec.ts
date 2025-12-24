import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { PasswordConfirmDialog } from './password-confirm-dialog';

describe('PasswordConfirmDialog', () => {
  let component: PasswordConfirmDialog;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordConfirmDialog],
      providers: [
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: {} }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(PasswordConfirmDialog);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
