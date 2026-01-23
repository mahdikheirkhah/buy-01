import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth'; // Adjust path if needed

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container">
      <h2>Login</h2>
      <form (ngSubmit)="onLogin()">
        <div class="form-group">
          <label for="email">Email</label>
          <input type="email" id="email" name="email" [(ngModel)]="loginData.email" required>
        </div>
        <div class="form-group">
          <label for="password">Password</label>
          <input type="password" id="password" name="password" [(ngModel)]="loginData.password" required>
        </div>
        <button type="submit">Login</button>
      </form>
      <p>Don't have an account? <a routerLink="/auth/register">Register here</a></p>
    </div>
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
    .form-group input[type="password"] {
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

    .error-message {
      background-color: #fff0f0;
      color: #c51111;
      border: 1px solid #fcc;
      border-radius: 4px;
      padding: 12px;
      text-align: center;
      margin-bottom: 20px;
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
  `]
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
        // Fetch current user to determine role
        this.authService.fetchCurrentUser().subscribe({
          next: (user) => {
            console.log('User role:', user.role);
            // Route based on user role
            if (user.role === 'CLIENT') {
              this.router.navigate(['/home']);
            } else if (user.role === 'SELLER') {
              this.router.navigate(['/seller-dashboard']);
            } else {
              // Default fallback
              this.router.navigate(['/home']);
            }
          },
          error: (err) => {
            console.error('Failed to fetch user role', err);
            this.router.navigate(['/home']);
          }
        });
      },
      error: (err) => {
        console.error('Login failed', err);
        // Handle login error
      }
    });
  }
}
