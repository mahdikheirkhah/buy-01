import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable,tap } from 'rxjs';
import { UpdateUserDTO } from '../models/update-user.dto';
import { AuthService } from './auth';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  // We assume your user controller is at /api/users
  private apiUrl = 'https://localhost:8443/api/users';

  constructor(private http: HttpClient, private authService: AuthService) { }

  /**
   * Deletes the currently authenticated user.
   * @param password The user's current password for verification.
   */
  deleteUser(password: string): Observable<any> {
    // The backend expects the password as a RequestParam
    const params = new HttpParams().set('password', password);

    return this.http.delete(this.apiUrl, {
      withCredentials: true,
      params: params,
      responseType: 'json' // Your backend returns a JSON map
    });
  }
deleteAvatar(): Observable<string>{ // You can also change <any> to <string>
  return this.http.delete(`${this.apiUrl}/avatar`, {
        withCredentials: true,
        responseType: 'text' // ✅ ADD THIS LINE
      });
  }
updateAvatar(avatarFile: File): Observable<any> {
    const formData = new FormData();
    // The key "avatarFile" must match the @RequestParam name in your controller
    formData.append('avatarFile', avatarFile);

    // We use POST to send FormData. The backend returns the updated User object.
    return this.http.post(`${this.apiUrl}/newAvatar`, formData, {
      withCredentials: true,
      responseType: 'json'
    });
  }
updateUser(updateDto: UpdateUserDTO): Observable<any> {
    return this.http.put(`${this.apiUrl}/me`, updateDto, {
      withCredentials: true,
      responseType: 'json'
    }).pipe(
      tap(() => {
        this.authService.fetchCurrentUser().subscribe();
      })
    );
  }
}
