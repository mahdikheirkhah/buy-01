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

  templateUrl: './my-info.html',
  styleUrls: ['./my-info.css']
  // -----------------------------
})
export class MyInfo implements OnInit {
  currentUser: User | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  // Avatar cropper state
  imageChangedEvent: any = '';
  showCropper = false;

  isEditingInfo = false;
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
