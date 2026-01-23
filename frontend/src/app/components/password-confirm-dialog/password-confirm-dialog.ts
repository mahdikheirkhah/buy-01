import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms'; // <-- Import FormsModule

export interface PasswordDialogData {
  title: string;
  message: string;
}

@Component({
  selector: 'app-password-confirm-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <div mat-dialog-content>
      <p>{{ data.message }}</p>
      <mat-form-field appearance="fill">
        <mat-label>Password</mat-label>
        <input matInput type="password" [(ngModel)]="password" (keyup.enter)="onConfirm()">
      </mat-form-field>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-flat-button color="warn" [disabled]="!password" (click)="onConfirm()">
        Delete My Account
      </button>
    </div>
  `,
  styles: [`
    mat-form-field {
      width: 100%;
    }

    div[mat-dialog-actions] {
      padding: 0 24px 20px 24px;
    }
  `]
})
export class PasswordConfirmDialog {
  password = '';

  constructor(
    public dialogRef: MatDialogRef<PasswordConfirmDialog>,
    @Inject(MAT_DIALOG_DATA) public data: PasswordDialogData
  ) { }

  onCancel(): void {
    this.dialogRef.close(); // Close without any data
  }

  onConfirm(): void {
    this.dialogRef.close(this.password); // Close and return the password
  }
}
