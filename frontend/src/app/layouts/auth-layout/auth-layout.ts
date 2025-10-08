import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router'; // Import RouterOutlet

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet], // ✅ Add RouterOutlet here
  templateUrl: './auth-layout.html',
  styleUrls: ['./auth-layout.css']
})
export class AuthLayout {}