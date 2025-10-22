import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authApiUrl = 'https://localhost:8443/api/auth';
  private usersApiUrl = 'https://localhost:8443/api/users';

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

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
    // ✅ Use the /users/me endpoint and include credentials
    return this.http.get<User>(`${this.usersApiUrl}/me`, { withCredentials: true }).pipe(
      tap(user => this.currentUserSubject.next(user))
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
