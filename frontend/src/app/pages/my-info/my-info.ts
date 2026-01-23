import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth';
import { UserService } from '../../services/user';
import { User } from '../../models/user.model';
import { Router } from '@angular/router';
import { UpdateInfoForm } from '../../components/update-info-form/update-info-form';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
// ✅ Import BOTH dialog components
import { PasswordConfirmDialog } from '../../components/password-confirm-dialog/password-confirm-dialog';
import { ConfirmDialog } from '../../components/confirm-dialog/confirm-dialog';
// Import your cropper modal
import { ImageCropperModal } from '../../components/image-cropper-modal/image-cropper-modal';

@Component({
  selector: 'app-my-info',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatDividerModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    PasswordConfirmDialog,
    ConfirmDialog,
    ImageCropperModal,
    UpdateInfoForm
  ],
  template: `
    <div class="my-info-container">
      <h2>My Information</h2>

      <div *ngIf="isLoading" class="spinner-container">
        <mat-spinner></mat-spinner>
      </div>

      <div *ngIf="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>

      <mat-card *ngIf="currentUser && !isLoading">
        <ng-container *ngIf="!isEditingInfo">
          <div class="avatar-section">
            <h3>Avatar</h3>
            <img *ngIf="currentUser.avatarUrl"
                 [src]="getAvatarUrl(currentUser.avatarUrl)"
                 alt="Your Avatar"
                 class="avatar-image">
            <div *ngIf="!currentUser.avatarUrl" class="avatar-placeholder">
              <mat-icon>person</mat-icon>
            </div>

            <div class="avatar-actions">
              <button mat-stroked-button color="primary" (click)="avatarUploadInput.click()">
                <mat-icon>upload</mat-icon>
                Change Photo
              </button>

              <input
                type="file"
                hidden
                #avatarUploadInput
                id="avatar-upload-input"
                (change)="onFileSelect($event)"
                accept="image/*" />

              <button mat-stroked-button color="warn" *ngIf="currentUser.avatarUrl" (click)="onDeleteAvatar()">
                <mat-icon>delete</mat-icon>
                Delete Photo
              </button>
            </div>
          </div>

          <mat-divider></mat-divider>

          <mat-card-content class="details-section">
            <h3>Details</h3>
            <div class="info-row">
              <strong>First Name:</strong>
              <span>{{ currentUser.firstName }}</span>
            </div>
            <div class="info-row">
              <strong>Last Name:</strong>
              <span>{{ currentUser.lastName }}</span>
            </div>
            <div class="info-row">
              <strong>Email:</strong>
              <span>{{ currentUser.email }}</span>
            </div>
            <div class="info-row">
              <strong>Role:</strong>
              <span>{{ currentUser.role | titlecase }}</span>
            </div>
          </mat-card-content>

          <mat-divider></mat-divider>

          <mat-card-actions class="account-actions">
            <button mat-flat-button color="primary" (click)="isEditingInfo = true">
              <mat-icon>edit</mat-icon>
              Update Information
            </button>

            <button mat-flat-button color="warn" class="delete-me-btn" (click)="onDeleteMe()">
              <mat-icon>warning</mat-icon>
              Delete My Account
            </button>
          </mat-card-actions>
        </ng-container>

        <app-update-info-form
          *ngIf="isEditingInfo"
          [currentUser]="currentUser"
          (close)="onFormClosed($event)">
        </app-update-info-form>
      </mat-card>
    </div>

    <app-image-cropper-modal
      *ngIf="showCropper"
      [imageChangedEvent]="imageChangedEvent"
      (croppedImageBlob)="handleAvatarBlob($event)"
      (modalClosed)="handleModalClose()">
    </app-image-cropper-modal>
  `,
  styles: [`
    .my-info-container {
      max-width: 800px;
      margin: 80px auto 40px;
      padding: 24px;
    }

    h2 {
      color: var(--navy);
      text-align: center;
    }

    .spinner-container {
      display: flex;
      justify-content: center;
      padding: 40px;
    }

    mat-card {
      padding: 0;
    }

    mat-card h3 {
      color: var(--navy-light);
      font-size: 1.1rem;
      font-weight: 500;
      margin: 0 0 16px 0;
    }

    .avatar-section {
      padding: 24px;
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .avatar-image {
      width: 150px;
      height: 150px;
      border-radius: 50%;
      object-fit: cover;
      border: 4px solid #eee;
    }

    .avatar-placeholder {
      width: 150px;
      height: 150px;
      border-radius: 50%;
      background-color: #f0f0f0;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .avatar-placeholder mat-icon {
      font-size: 80px;
      width: 80px;
      height: 80px;
      color: #aaa;
    }

    .avatar-actions {
      display: flex;
      gap: 16px;
      margin-top: 20px;
    }

    .details-section {
      padding: 24px;
    }

    .info-row {
      display: flex;
      justify-content: space-between;
      padding: 12px 8px;
      border-bottom: 1px solid #f0f0f0;
    }

    .info-row:last-child {
      border-bottom: none;
    }

    .info-row strong {
      color: #333;
    }

    .info-row span {
      color: #555;
    }

    .account-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      padding: 24px;
      background-color: #f9f9f9;
    }

    .delete-me-btn {
      background-color: #d32f2f;
      color: white;
    }

    .error-message {
      background-color: #fff0f0;
      color: #c51111;
      border: 1px solid #fcc;
      border-radius: 8px;
      padding: 12px;
      margin-bottom: 16px;
      text-align: center;
    }
  `]
})
export class MyInfo implements OnInit { // ✅ FIX: Renamed to MyInfo
  currentUser: User | null = null;
  isLoading = true;
  errorMessage: string | null = null;
  isEditingInfo = false;
  // --- State for the cropper ---
  imageChangedEvent: any = '';
  showCropper = false;
  // -----------------------------

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private router: Router,
    public dialog: MatDialog
  ) {
  }

  ngOnInit(): void {
    this.authService.fetchCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Could not load user data.';
        this.isLoading = false;
      }
    });
  }

  // This is triggered by the hidden file input
  onFileSelect(event: any): void {
    this.imageChangedEvent = event;
    this.showCropper = true; // Show the modal
  }

  // This is called by the cropper modal
  handleAvatarBlob(blob: Blob) {
    if (!this.currentUser) return;
    const avatarFile = new File([blob], 'avatar.png', { type: 'image/png' });

    this.userService.updateAvatar(avatarFile).subscribe({
      next: (updatedUser: User) => {
        this.ngOnInit();
        this.authService.fetchCurrentUser().subscribe(); // Re-sync global state
      },
      error: (err) => console.error('Failed to update avatar', err)
    });
  }

  handleModalClose() {
    this.showCropper = false;
    const fileInput = document.getElementById('avatar-upload-input') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }
  getAvatarUrl(avatarPath: string): string {
    return `https://localhost:8443${avatarPath}`;
  }
  onDeleteAvatar(): void {
    if (!this.currentUser) return;

    const dialogRef = this.dialog.open(ConfirmDialog, {
      width: '350px',
      data: {
        title: 'Delete Avatar',
        message: 'Are you sure you want to delete your avatar? This action cannot be undone.'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true && this.currentUser) {
        this.userService.deleteAvatar().subscribe({
          next: (response) => {
            console.log(response); // "avatar deleted successfully"
            // Re-run ngOnInit to fetch the updated user (with null avatar)
            this.ngOnInit();
          },
          error: (err) => {
            console.error('Failed to delete avatar', err);
          }
        });
      }
    });
  }
  // --- Delete User Logic ---
  onDeleteMe(): void {
    // 1. Open the password dialog
    const dialogRef = this.dialog.open(PasswordConfirmDialog, {
      width: '400px',
      data: {
        title: 'Delete Account',
        message: 'This action is permanent. To confirm, please enter your password.'
      }
    });

    // 2. Listen for the dialog to close
    dialogRef.afterClosed().subscribe(password => {
      // 3. If the user provided a password
      if (password) {
        this.userService.deleteUser(password).subscribe({
          next: (response) => {
            console.log('User deleted:', response.message);
            // 4. Log the user out (which clears local state)
            this.authService.logout().subscribe(() => {
              // 5. Redirect to register page
              this.router.navigate(['/register']);
            });
          },
          error: (err) => {
            console.error('Failed to delete user:', err);
            // TODO: Show a snackbar error
            alert(`Error: ${err.error.message || 'Wrong password or server error.'}`);
          }
        });
      }
    });
  }
  onUpdateInfo(): void {
    this.isEditingInfo = true;
  }
  onFormClosed(isSuccess: boolean): void {
    this.isEditingInfo = false;
    if (isSuccess) {
      this.ngOnInit();
    }
  }
}
