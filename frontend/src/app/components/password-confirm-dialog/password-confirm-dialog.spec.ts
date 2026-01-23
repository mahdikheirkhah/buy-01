import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { PasswordConfirmDialog, PasswordDialogData } from './password-confirm-dialog';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

describe('PasswordConfirmDialog', () => {
  let component: PasswordConfirmDialog;
  let fixture: ComponentFixture<PasswordConfirmDialog>;
  let dialogRefMock: jasmine.SpyObj<MatDialogRef<PasswordConfirmDialog>>;
  const mockData: PasswordDialogData = {
    title: 'Enter Password',
    message: 'Please enter your password to continue'
  };

  beforeEach(async () => {
    dialogRefMock = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [
        PasswordConfirmDialog,
        FormsModule,
        CommonModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule
      ],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: mockData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PasswordConfirmDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty password', () => {
    expect(component.password).toBe('');
  });

  it('should display provided title and message', () => {
    expect(component.data.title).toBe('Enter Password');
    expect(component.data.message).toBe('Please enter your password to continue');
  });

  it('should close dialog without data on cancel', () => {
    component.onCancel();

    expect(dialogRefMock.close).toHaveBeenCalledWith();
  });

  it('should close dialog with password on confirm', () => {
    component.password = 'mySecurePassword123';

    component.onConfirm();

    expect(dialogRefMock.close).toHaveBeenCalledWith('mySecurePassword123');
  });

  it('should close with empty string if password is empty', () => {
    component.password = '';

    component.onConfirm();

    expect(dialogRefMock.close).toHaveBeenCalledWith('');
  });

  it('should update password value', () => {
    component.password = 'newPassword';

    expect(component.password).toBe('newPassword');
  });
});
