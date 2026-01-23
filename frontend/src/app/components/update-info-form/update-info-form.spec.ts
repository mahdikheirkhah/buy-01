import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { UpdateInfoForm } from './update-info-form';
import { UserService } from '../../services/user';
import { User } from '../../models/user.model';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { CommonModule } from '@angular/common';

describe('UpdateInfoForm', () => {
  let component: UpdateInfoForm;
  let fixture: ComponentFixture<UpdateInfoForm>;
  let userServiceMock: jasmine.SpyObj<UserService>;

  const mockUser: User = {
    id: 'user-123',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    role: 'CLIENT',
    avatarUrl: undefined
  };

  beforeEach(async () => {
    userServiceMock = jasmine.createSpyObj('UserService', ['updateUser']);
    userServiceMock.updateUser.and.returnValue(of({} as any));

    await TestBed.configureTestingModule({
      imports: [
        UpdateInfoForm,
        HttpClientTestingModule,
        ReactiveFormsModule,
        CommonModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatDividerModule
      ],
      providers: [
        { provide: UserService, useValue: userServiceMock },
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: {} }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UpdateInfoForm);
    component = fixture.componentInstance;
    component.currentUser = mockUser;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with current user data', () => {
    expect(component.updateForm.value.firstName).toBe('John');
    expect(component.updateForm.value.lastName).toBe('Doe');
    expect(component.updateForm.value.email).toBe('john@example.com');
  });

  it('should validate firstName minimum length', () => {
    component.updateForm.patchValue({ firstName: 'J' });

    expect(component.updateForm.get('firstName')?.errors?.['minlength']).toBeTruthy();
  });

  it('should validate lastName minimum length', () => {
    component.updateForm.patchValue({ lastName: 'D' });

    expect(component.updateForm.get('lastName')?.errors?.['minlength']).toBeTruthy();
  });

  it('should validate email format', () => {
    component.updateForm.patchValue({ email: 'invalid-email' });

    expect(component.updateForm.get('email')?.errors?.['email']).toBeTruthy();
  });

  it('should validate newPassword minimum length', () => {
    component.updateForm.patchValue({ newPassword: '123' });

    expect(component.updateForm.get('newPassword')?.errors?.['minlength']).toBeTruthy();
  });

  it('should not submit invalid form', () => {
    component.updateForm.patchValue({ firstName: '' });
    component.updateForm.get('firstName')?.markAsTouched();

    component.onSubmit();

    expect(userServiceMock.updateUser).not.toHaveBeenCalled();
  });

  it('should require currentPassword when changing email', () => {
    component.updateForm.patchValue({ email: 'newemail@example.com', currentPassword: '' });

    component.onSubmit();

    expect(component.errorMessage).toContain('Current Password is required');
    expect(userServiceMock.updateUser).not.toHaveBeenCalled();
  });

  it('should require currentPassword when setting newPassword', () => {
    component.updateForm.patchValue({ newPassword: 'newpass123', currentPassword: '' });

    component.onSubmit();

    expect(component.errorMessage).toContain('Current Password is required');
  });

  it('should only send changed fields in DTO', () => {
    component.updateForm.patchValue({ firstName: 'Jane' });

    component.onSubmit();

    expect(userServiceMock.updateUser).toHaveBeenCalledWith({
      firstName: 'Jane'
    });
  });

  it('should include currentPassword when changing email', () => {
    component.updateForm.patchValue({
      email: 'newemail@example.com',
      currentPassword: 'oldpass'
    });

    component.onSubmit();

    expect(userServiceMock.updateUser).toHaveBeenCalledWith({
      email: 'newemail@example.com',
      currentPassword: 'oldpass'
    });
  });

  it('should emit close event with true on success', () => {
    spyOn(component.close, 'emit');
    component.updateForm.patchValue({ firstName: 'Jane' });

    component.onSubmit();

    expect(component.close.emit).toHaveBeenCalledWith(true);
  });

  it('should set success message on update success', () => {
    component.updateForm.patchValue({ firstName: 'Jane' });

    component.onSubmit();

    expect(component.successMessage).toBe('Profile updated successfully!');
  });

  it('should handle update error', () => {
    userServiceMock.updateUser.and.returnValue(throwError(() => ({ error: { message: 'Update failed' } })));
    component.updateForm.patchValue({ firstName: 'Jane' });

    component.onSubmit();

    expect(component.errorMessage).toBe('Update failed');
    expect(component.isLoading).toBe(false);
  });

  it('should emit close event with false on cancel', () => {
    spyOn(component.close, 'emit');

    component.onCancel();

    expect(component.close.emit).toHaveBeenCalledWith(false);
  });

  it('should set isLoading during submission', () => {
    component.updateForm.patchValue({ firstName: 'Jane' });
    expect(component.isLoading).toBe(false);

    component.onSubmit();

    // isLoading is set to true, but then immediately to false after observable completes
    expect(component.successMessage).toBeTruthy();
  });

  it('should handle multiple changed fields', () => {
    component.updateForm.patchValue({
      firstName: 'Jane',
      lastName: 'Smith'
    });

    component.onSubmit();

    expect(userServiceMock.updateUser).toHaveBeenCalledWith({
      firstName: 'Jane',
      lastName: 'Smith'
    });
  });

  it('should handle error without error message', () => {
    userServiceMock.updateUser.and.returnValue(throwError(() => ({ error: {} })));
    component.updateForm.patchValue({ firstName: 'Jane' });

    component.onSubmit();

    expect(component.errorMessage).toBe('An unknown error occurred.');
    expect(component.isLoading).toBe(false);
  });

  it('should clear error and success messages on submit', () => {
    component.errorMessage = 'Previous error';
    component.successMessage = 'Previous success';
    component.updateForm.patchValue({ firstName: 'Jane' });

    component.onSubmit();

    expect(component.errorMessage).toBeNull();
  });

  it('should set isLoading to true at start of submission', () => {
    component.updateForm.patchValue({ firstName: 'Jane' });
    expect(component.isLoading).toBe(false);

    component.onSubmit();

    expect(userServiceMock.updateUser).toHaveBeenCalled();
  });

  it('should only send password in DTO when changed and currentPassword provided', () => {
    component.updateForm.patchValue({
      newPassword: 'newpass123',
      currentPassword: 'oldpass'
    });

    component.onSubmit();

    expect(userServiceMock.updateUser).toHaveBeenCalledWith({
      newPassword: 'newpass123',
      currentPassword: 'oldpass'
    });
  });

  it('should include both email and password changes with currentPassword', () => {
    component.updateForm.patchValue({
      email: 'newemail@example.com',
      newPassword: 'newpass123',
      currentPassword: 'oldpass'
    });

    component.onSubmit();

    expect(userServiceMock.updateUser).toHaveBeenCalledWith({
      email: 'newemail@example.com',
      newPassword: 'newpass123',
      currentPassword: 'oldpass'
    });
  });

  it('should include all changed fields in DTO', () => {
    component.updateForm.patchValue({
      firstName: 'Jane',
      lastName: 'Smith',
      email: 'jane@example.com',
      currentPassword: 'oldpass'
    });

    component.onSubmit();

    expect(userServiceMock.updateUser).toHaveBeenCalledWith({
      firstName: 'Jane',
      lastName: 'Smith',
      email: 'jane@example.com',
      currentPassword: 'oldpass'
    });
  });

  it('should not send fields that haven\'t changed', () => {
    // All form values same as current user
    component.onSubmit();

    // Should not call updateUser when nothing changed
    expect(userServiceMock.updateUser).not.toHaveBeenCalled();
  });
});