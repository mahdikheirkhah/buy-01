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
