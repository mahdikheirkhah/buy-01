import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth'; // Corrected path
import { ImageCropperModal } from '../../components/image-cropper-modal/image-cropper-modal';


@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    ImageCropperModal
  ],
  template: `
    <div class="container">
      <h2>Register</h2>
      <form (ngSubmit)="onRegister()">
        <div class="form-group">
          <label for="firstName">First Name</label>
          <input type="text" id="firstName" name="firstName" [(ngModel)]="registerData.firstName" required>
        </div>

        <div class="form-group">
          <label for="lastName">Last Name</label>
          <input type="text" id="lastName" name="lastName" [(ngModel)]="registerData.lastName" required>
        </div>

        <div class="form-group">
          <label for="email">Email</label>
          <input type="email" id="email" name="email" [(ngModel)]="registerData.email" required>
        </div>

        <div class="form-group">
          <label for="password">Password</label>
          <input type="password" id="password" name="password" [(ngModel)]="registerData.password" required>
        </div>

        <div class="form-group">
          <label for="role">Register as</label>
          <select id="role" name="role" [(ngModel)]="registerData.role">
            <option value="CLIENT">Client</option>
            <option value="SELLER">Seller</option>
          </select>
        </div>

        <div class="form-group" *ngIf="registerData.role === 'SELLER'">
          <label for="avatar">Avatar</label>
          <input type="file" id="avatar" accept="image/*" (change)="onFileSelected($event)">

          <div class="preview-container" *ngIf="croppedImage">
            <p>Avatar Preview:</p>
            <img [src]="croppedImage" class="avatar-preview" alt="Avatar image preview" />
          </div>
        </div>

        <button type="submit">Register</button>
      </form>
      <p>Already have an account? <a routerLink="/login">Login here</a></p>
    </div>

    <app-image-cropper-modal
      *ngIf="showCropper"
      [imageChangedEvent]="imageChangedEvent"
      (croppedImageBlob)="handleImageBlob($event)"
      (modalClosed)="handleModalClose()">
    </app-image-cropper-modal>
  `,
  styles: [`
    .container {
      max-width: 500px;
      margin: 80px auto 40px;
      padding: 24px;
      background-color: var(--white);
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }

    h2 {
      text-align: center;
      color: var(--navy);
      margin-top: 0;
    }

    .form-group {
      margin-bottom: 20px;
    }

    .form-group label {
      display: block;
      margin-bottom: 8px;
      font-weight: 500;
      color: #555;
    }

    .form-group input[type="text"],
    .form-group input[type="email"],
    .form-group input[type="password"],
    .form-group select {
      width: 100%;
      padding: 12px;
      border: 1px solid #ccc;
      border-radius: 4px;
      box-sizing: border-box;
    }

    button[type="submit"] {
      width: 100%;
      padding: 14px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 1rem;
      font-weight: 600;
      background-color: var(--green);
      color: var(--white);
      transition: background-color 0.2s ease;
    }

    button[type="submit"]:hover {
      background-color: var(--green-dark);
    }

    p {
      text-align: center;
      margin-top: 20px;
    }

    p a {
      color: var(--navy);
      text-decoration: none;
      font-weight: 600;
    }

    p a:hover {
      text-decoration: underline;
    }

    .cropper-modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.7);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
    }

    .cropper-modal-content {
      background-color: white;
      padding: 20px;
      border-radius: 8px;
      width: 90%;
      max-width: 500px;
      text-align: center;
    }

    .cropper-modal-content h3 {
      margin-top: 0;
    }

    image-cropper {
      display: block;
      height: 300px;
      width: 100%;
    }

    .cropper-buttons {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
      gap: 10px;
    }

    .cropper-buttons button {
      padding: 10px 20px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }

    .cropper-buttons .save-btn {
      background-color: #3f51b5;
      color: white;
    }

    .cropper-buttons .cancel-btn {
      background-color: #f44336;
      color: white;
    }

    .preview-container {
      text-align: center;
      margin-top: 15px;
    }

    .avatar-preview {
      width: 100px;
      height: 100px;
      border-radius: 50%;
      object-fit: cover;
      border: 2px solid #ddd;
    }
  `]
})
export class RegisterComponent {
  registerData = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'CLIENT'
  };

  // --- Properties for image cropping ---
  imageChangedEvent: any = '';       // Holds the file event
  showCropper = false;               // Controls the modal
  croppedImage: any = '';            // Holds the preview URL (base64)
  croppedBlob: Blob | null = null;   // Holds the final file blob to upload

  constructor(private authService: AuthService, private router: Router) { }

  onFileSelected(event: any): void {
    this.imageChangedEvent = event;
    this.showCropper = true; // Show the modal
  }

  // --- New Handlers for the Modal ---
  handleImageBlob(blob: Blob) {
    this.croppedBlob = blob;
    // Create a URL for the preview image
    this.croppedImage = URL.createObjectURL(blob);
  }

  handleModalClose() {
    this.showCropper = false;
    // Clear the file input so you can select the same file again
    const fileInput = document.getElementById('avatar') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }
  // ---------------------------------

  onRegister(): void {
    const formData = new FormData();
    formData.append('userDto', new Blob([JSON.stringify(this.registerData)], {
      type: 'application/json'
    }));

    if (this.registerData.role === 'SELLER' && this.croppedBlob) {
      const avatarFile = new File([this.croppedBlob], 'avatar.png', { type: 'image/png' });
      formData.append('avatarFile', avatarFile);
    }
    this.authService.register(formData).subscribe({
      next: (response: any) => {
        console.log('Registration successful', response);
        this.router.navigate(['/login']);
      },
      error: (err: any) => {
        console.error('Registration failed', err);
      }
    });
  }
}
