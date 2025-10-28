import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth'; // Corrected path
import { ImageCropperComponent, ImageCroppedEvent } from 'ngx-image-cropper'; // Corrected import

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    ImageCropperComponent // Corrected import
  ],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
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

  // 1. File is selected
  onFileSelected(event: any): void {
    this.imageChangedEvent = event;
    this.showCropper = true; // Show the modal
  }

  // 2. User moves the cropper
imageCropped(event: ImageCroppedEvent) {
    this.croppedImage = event.base64; // For the preview

    // ✅ THE FIX:
    // Use the nullish coalescing operator (??) to convert
    // undefined to null, which matches your variable's type.
    this.croppedBlob = event.blob ?? null;
  }

  // 3. User clicks "Save Avatar" in the modal
  saveCrop() {
    this.showCropper = false; // Hide the modal
    // The this.croppedBlob is now ready to be uploaded
  }

  // 4. User clicks "Cancel" in the modal
  cancelCrop() {
    this.showCropper = false;
    this.imageChangedEvent = '';
    this.croppedImage = '';
    this.croppedBlob = null;
    // We also need to clear the file input
    const fileInput = document.getElementById('avatar') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  loadImageFailed() {
    console.error('Image failed to load');
    this.cancelCrop();
  }

  // 5. User clicks "Register"
  onRegister(): void {
    const formData = new FormData();
    formData.append('userDto', new Blob([JSON.stringify(this.registerData)], {
      type: 'application/json'
    }));

    // Check if we have a cropped blob to send
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
