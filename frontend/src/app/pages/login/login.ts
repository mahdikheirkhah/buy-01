import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth'; // Adjust path if needed

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  loginData = {
    email: '',
    password: ''
  };
  constructor(private authService: AuthService, private router: Router) { }
  onLogin() {
    this.authService.login(this.loginData).subscribe({
      next: (response) => {
        console.log('Login successful', response);
        // Get current user role to navigate appropriately
        this.authService.fetchCurrentUser().subscribe({
          next: (user) => {
            console.log('User role:', user.role);
            // Route based on user role
            if (user.role === 'SELLER') {
              this.router.navigate(['/seller-dashboard']);
            } else {
              // Default fallback for CLIENT and other roles
              this.router.navigate(['/home']);
            }
          },
          error: (err) => {
            console.error('Failed to fetch user role', err);
            // Even if fetchCurrentUser fails, navigate to home
            this.router.navigate(['/home']);
          }
        });
      },
      error: (err) => {
        console.error('Login failed', err);
        // Handle login error - don't navigate
      }
    });
  }
}
