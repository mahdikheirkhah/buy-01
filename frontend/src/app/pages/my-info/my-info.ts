import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth';
import { UserService } from '../../services/user'; // Adjust path
import { User } from '../../models/user.model'; // Adjust path
import { Router } from '@angular/router';

// Material Imports
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { PasswordConfirmDialog } from '../../components/password-confirm-dialog/password-confirm-dialog';

@Component({
  selector: 'app-my-info',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    PasswordConfirmDialog
  ],
  templateUrl: './my-info.html',
  styleUrls: ['./my-info.css']
})
export class MyInfo implements OnInit {
  currentUser: User | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private router: Router,
    public dialog: MatDialog
  ) {}

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

  getAvatarUrl(avatarPath: string): string {
    return `https://localhost:8443${avatarPath}`;
  }

  // --- STUBS for later ---
  onChangeAvatar(): void {
    console.log('Change Avatar clicked');
    // TODO: Open file picker, open cropper modal, call user service
  }

  onDeleteAvatar(): void {
    console.log('Delete Avatar clicked');
    // TODO: Open confirm dialog, call user service
  }

  onUpdateInfo(): void {
    console.log('Update Info clicked');
    // TODO: Open update info modal
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
}
