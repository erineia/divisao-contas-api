import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../environment';

export interface LoginRequest {
  usuario: string;
  senha: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  login(data: LoginRequest): Observable<any> {
    // Endpoint real da API: POST /auth/login
    return this.http.post<any>(`${this.apiUrl}/auth/login`, data).pipe(
      tap((response) => {
        // Se a API devolver token ou usuário, salve aqui se precisar
        if (response?.token) {
          localStorage.setItem('token', response.token);
        }
      })
    );
  }
}
