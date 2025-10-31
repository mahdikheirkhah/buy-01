import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  // We assume your user controller is at /api/users
  private apiUrl = 'https://localhost:8443/api/users';

  constructor(private http: HttpClient) { }

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

  // TODO: Add methods for update user, change avatar, delete avatar
}
