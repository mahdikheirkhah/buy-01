// src/app/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { User } from '../models/user.model';
import { CookieService } from 'ngx-cookie-service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private authApiUrl = 'https://localhost:8443/api/auth';
  private usersApiUrl = 'https://localhost:8443/api/users';

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public readonly currentUser$ = this.currentUserSubject.asObservable();

  /** true after the first call to /me (even if it fails) */
  private userLoaded = false;

  constructor(
    private http: HttpClient,
    private cookieService: CookieService
  ) { }

  // ADD THIS GETTER BACK
  public get currentUserRole(): string | null {
    return this.currentUserSubject.value?.role || null;
  }
  /** Call this from an APP_INITIALIZER or a root guard */
  init(): Observable<User | null> {
    if (this.userLoaded) {
      return this.currentUser$;
    }
    this.userLoaded = true;
    return this.fetchCurrentUser().pipe(
      catchError(err => {
        this.currentUserSubject.next(null);
        return throwError(() => err);
      })
    );
  }

  login(credentials: any): Observable<any> {
    return this.http.post(`${this.authApiUrl}/login`, credentials, {
      withCredentials: true
    }).pipe(
      // after a successful login we **must** reload the user
      tap(() => this.fetchCurrentUser().subscribe())
    );
  }

  fetchCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.usersApiUrl}/me`, { withCredentials: true }).pipe(
      tap(user => this.currentUserSubject.next(user)),
      catchError((err: HttpErrorResponse) => {
        // 401 → not logged in → clear subject
        if (err.status === 401) {
          this.currentUserSubject.next(null);
        }
        return throwError(() => err);
      })
    );
  }

  logout(): Observable<any> {
    this.cookieService.delete('jwt');  // ✅ Delete cookie FIRST
    return this.http.post(`${this.usersApiUrl}/logout`, {}, { withCredentials: true }).pipe(
      tap(() => this.currentUserSubject.next(null))
    );
  }

  register(formData: FormData): Observable<any> {
    return this.http.post(`${this.authApiUrl}/register`, formData);
  }
}
