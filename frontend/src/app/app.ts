import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  // ✅ The imports array should only contain RouterOutlet
  imports: [RouterOutlet],
  templateUrl: './app.html', // Or app.component.html
  styleUrls: ['./app.css']  // Or app.component.css
})
export class App {
  title = 'frontend';
}