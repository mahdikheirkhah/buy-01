import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { User } from '../../models/user.model';
import { UpdateUserDTO } from '../../models/update-user.dto';
import { UserService } from '../../services/user';

// Import Material Modules
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MatDividerModule } from '@angular/material/divider';
@Component({
  selector: 'app-update-info-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  template: `
    <form [formGroup]="updateForm" (ngSubmit)="onSubmit()" class="update-form">
      <h3>Update Your Information</h3>
      <p>Only fill in the fields you wish to change.</p>

      <mat-form-field appearance="fill">
        <mat-label>First Name</mat-label>
        <input matInput formControlName="firstName">
      </mat-form-field>

      <mat-form-field appearance="fill">
        <mat-label>Last Name</mat-label>
        <input matInput formControlName="lastName">
      </mat-form-field>

      <mat-form-field appearance="fill">
        <mat-label>Email</mat-label>
        <input matInput type="email" formControlName="email">
      </mat-form-field>

      <mat-divider></mat-divider>

      <p class="password-note">
        To change your <strong>Email</strong> or <strong>Password</strong>,
        you must provide your Current Password.
      </p>

      <mat-form-field appearance="fill">
        <mat-label>New Password</mat-label>
        <input matInput type="password" formControlName="newPassword" placeholder="Fill to change password">
      </mat-form-field>

      <mat-form-field appearance="fill">
        <mat-label>Current Password</mat-label>
        <input matInput type="password" formControlName="currentPassword">
        <mat-error *ngIf="updateForm.controls['currentPassword'].hasError('required')">
          Current Password is required for this change
        </mat-error>
      </mat-form-field>

      <div *ngIf="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
      <div *ngIf="successMessage" class="success-message">
        {{ successMessage }}
      </div>

      <div class="form-actions" *ngIf="!isLoading">
        <button mat-button type="button" (click)="onCancel()">Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="updateForm.pristine">
          Save Changes
        </button>
      </div>

      <div *ngIf="isLoading" class="spinner-container">
        <mat-spinner diameter="40"></mat-spinner>
        <span>Updating...</span>
      </div>
    </form>
  `,
  styles: [`
    .update-form {
      padding: 24px;
      background-color: #f9f9f9;
      border-radius: 8px;
    }

    h3 {
      margin-top: 0;
      color: var(--navy);
    }

    mat-form-field {
      width: 100%;
    }

    .password-note {
      font-size: 0.9rem;
      color: #555;
      background-color: #f0f0f0;
      padding: 10px;
      border-radius: 4px;
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      margin-top: 20px;
    }

    .error-message, .success-message {
      padding: 12px;
      border-radius: 4px;
      margin: 16px 0;
      text-align: center;
    }

    .error-message {
      background-color: #fff0f0;
      color: #c51111;
      border: 1px solid #fcc;
    }

    .success-message {
      background-color: #f0fff0;
      color: #0c6e0c;
      border: 1px solid #cfc;
    }

    .spinner-container {
      display: flex;
      gap: 20px;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }
  `]
})
export class UpdateInfoForm implements OnInit {
  @Input() currentUser!: User; // We receive the current user
  @Output() close = new EventEmitter<boolean>(); // Emits 'true' on success

  updateForm: FormGroup;
  isLoading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserService
  ) {
    this.updateForm = this.fb.group({
      firstName: ['', Validators.minLength(2)],
      lastName: ['', Validators.minLength(2)],
      email: ['', Validators.email],
      currentPassword: [''], // Will add validator conditionally
      newPassword: ['', Validators.minLength(5)]
    });
  }

  ngOnInit(): void {
    // Pre-fill the form with the user's current data
    if (this.currentUser) {
      this.updateForm.patchValue({
        firstName: this.currentUser.firstName,
        lastName: this.currentUser.lastName,
        email: this.currentUser.email
      });
    }
  }

  onSubmit(): void {
    if (this.updateForm.invalid) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const formValues = this.updateForm.value;
    const dto: UpdateUserDTO = {};

    // Only add fields to the DTO if they were actually changed
    if (formValues.firstName !== this.currentUser.firstName) {
      dto.firstName = formValues.firstName;
    }
    if (formValues.lastName !== this.currentUser.lastName) {
      dto.lastName = formValues.lastName;
    }
    if (formValues.email !== this.currentUser.email) {
      dto.email = formValues.email;
    }
    if (formValues.newPassword) {
      dto.newPassword = formValues.newPassword;
    }

    // If they are changing email or password, they MUST provide currentPassword
    if (dto.email || dto.newPassword) {
      if (!formValues.currentPassword) {
        this.updateForm.controls['currentPassword'].setErrors({ required: true });
        this.errorMessage = 'Current Password is required to change Email or New Password.';
        this.isLoading = false;
        return;
      }
      dto.currentPassword = formValues.currentPassword;
    }

    this.userService.updateUser(dto).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.successMessage = 'Profile updated successfully!';
        // Wait 2 seconds, then emit success and close
        setTimeout(() => this.close.emit(true), 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'An unknown error occurred.';
      }
    });
  }

  onCancel(): void {
    this.close.emit(false); // Emit 'false' on cancel
  }
}
