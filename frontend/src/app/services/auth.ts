import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { User } from '../models/user.model';
import { CookieService } from 'ngx-cookie-service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authApiUrl = 'https://localhost:8443/api/auth';
  private usersApiUrl = 'https://localhost:8443/api/users';

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient,
    private cookieService: CookieService
    ) {}

  public get currentUserRole(): string | null {
    return this.currentUserSubject.value?.role || null;
  }

login(credentials: any): Observable<any> {
    // ✅ Remove the tap operator calling fetchCurrentUser
    return this.http.post(`${this.authApiUrl}/login`, credentials, { withCredentials: true });
    // If you needed other side effects on login success *not* involving another HTTP call,
    // you could keep tap: .pipe(tap(response => console.log('Login API call successful')));
  }

fetchCurrentUser(): Observable<User> {
    // Check and log the cookie value BEFORE sending the request
    const jwtToken = this.cookieService.get('jwt'); // Get the cookie value by name
    if (jwtToken) {
      console.log('JWT Cookie value before sending request:', jwtToken);
    } else {
      console.log('JWT Cookie not found before sending request.');
    }

    // Now make the HTTP request
    return this.http.get<User>(`${this.usersApiUrl}/me`, { withCredentials: true }).pipe(
      tap(user => {
        console.log('Received user data:', user);
        this.currentUserSubject.next(user);
      })
    );
  }
  logout(): Observable<any> {
    // ✅ Include credentials to allow the backend to clear the cookie
    return this.http.post(`${this.usersApiUrl}/logout`, {}, { withCredentials: true }).pipe(
      tap(() => this.currentUserSubject.next(null))
    );
  }

  register(formData: FormData): Observable<any> {
    // Register does not need credentials
    return this.http.post(`${this.authApiUrl}/register`, formData);
  }
}
