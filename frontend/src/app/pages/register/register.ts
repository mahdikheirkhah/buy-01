import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth'; // Make sure path is correct

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {
  registerData = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'CLIENT' // Default role is CLIENT
  };

  selectedFile: File | null = null;

  constructor(private authService: AuthService, private router: Router) { }

  onFileSelected(event: any): void {
    this.selectedFile = event.target.files[0] ?? null;
  }

  onRegister(): void {
    // Always create a FormData object
    const formData = new FormData();

    // Append the user data as a JSON string Blob.
    // The backend's @RequestPart("userDto") will automatically deserialize this.
    formData.append('userDto', new Blob([JSON.stringify(this.registerData)], {
      type: 'application/json'
    }));

    // If the user is a seller and has selected a file, append the file.
    if (this.registerData.role === 'SELLER' && this.selectedFile) {
      formData.append('avatarFile', this.selectedFile, this.selectedFile.name);
    }

this.authService.register(formData).subscribe({
  next: (response: any) => { // <-- Add : any
    console.log('Registration successful', response);
    this.router.navigate(['/login']);
  },
  error: (err: any) => { // <-- Add : any
    console.error('Registration failed', err);
  }
});
  }
}
