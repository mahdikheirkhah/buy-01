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
    FormsModule, // <-- Add FormsModule for [(ngModel)]
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './password-confirm-dialog.html',
  styleUrls: ['./password-confirm-dialog.css']
})
export class PasswordConfirmDialog {
  password = '';

  constructor(
    public dialogRef: MatDialogRef<PasswordConfirmDialog>,
    @Inject(MAT_DIALOG_DATA) public data: PasswordDialogData
  ) {}

  onCancel(): void {
    this.dialogRef.close(); // Close without any data
  }

  onConfirm(): void {
    this.dialogRef.close(this.password); // Close and return the password
  }
}
