import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms'; // Import FormsModule
import { Router, RouterLink } from '@angular/router'; // Import RouterLink
import { CommonModule } from '@angular/common'; // Import CommonModule
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-register',
  standalone: true, // <-- Add this
  imports: [FormsModule, RouterLink, CommonModule], // <-- Add this
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {
  registerData = {
    firstName: '',
    lastName: '',
    email: '',
    password: ''

  };

  constructor(private authService: AuthService, private router: Router) { }

  onRegister() {
    this.authService.register(this.registerData).subscribe({
      next: (response) => {
        console.log('Registration successful', response);
        // Redirect to login page after successful registration
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Registration failed', err);
        // Handle registration error (e.g., show an error message)
      }
    });
  }
}
