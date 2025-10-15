import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authApiUrl = 'https://localhost:8080/api/auth';
  private usersApiUrl = 'https://localhost:8080/api/users'; // ✅ Use the correct URL for user data

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  public get currentUserRole(): string | null {
    return this.currentUserSubject.value?.role || null;
  }

  login(credentials: any): Observable<any> {
    // ✅ Add withCredentials: true to send/receive cookies
    return this.http.post(`${this.authApiUrl}/login`, credentials, { withCredentials: true }).pipe(
      tap(() => this.fetchCurrentUser().subscribe())
    );
  }

  fetchCurrentUser(): Observable<User> {
    // ✅ Use the /users/me endpoint and include credentials
    return this.http.get<User>(`${this.usersApiUrl}/me`, { withCredentials: true }).pipe(
      tap(user => this.currentUserSubject.next(user))
    );
  }

  logout(): Observable<any> {
    // ✅ Include credentials to allow the backend to clear the cookie
    return this.http.post(`${this.authApiUrl}/logout`, {}, { withCredentials: true }).pipe(
      tap(() => this.currentUserSubject.next(null))
    );
  }

  register(formData: FormData): Observable<any> {
    // Register does not need credentials
    return this.http.post(`${this.authApiUrl}/register`, formData);
  }
}
