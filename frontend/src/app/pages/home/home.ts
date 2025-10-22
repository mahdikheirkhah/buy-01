// src/app/components/home/home.ts (or wherever you land after login)
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth'; // Adjust path
import { User } from '../../models/user.model'; // Adjust path
import { CommonModule } from '@angular/common'; // For *ngIf

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule], // Add CommonModule
  template: `
    <h2>Home</h2>
    <div *ngIf="currentUser">
      <p>Welcome, {{ currentUser.firstName }} {{ currentUser.lastName }}!</p>
      <p>Email: {{ currentUser.email }}</p>
      <p>Role: {{ currentUser.role }}</p>
    </div>
    <div *ngIf="errorMessage">
        <p style="color: red;">{{ errorMessage }}</p>
    </div>
  `
  // Add styleUrls if needed
})
export class HomeComponent implements OnInit {
  currentUser: User | null = null;
  errorMessage: string | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    console.log('HomeComponent initialized, fetching current user...');
    this.authService.fetchCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        console.log('Current user fetched successfully:', user);
      },
      error: (err) => {
         console.error('Failed to fetch current user:', err);
         this.errorMessage = 'Could not load user data. Please try logging in again.';
         // Optionally navigate back to login or handle error
      }
    });
  }
}
