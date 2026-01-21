import { Injectable } from '@angular/core';
import { tap } from 'rxjs/operators';
import { ApiService } from './api.service';

type LoginRequest = { usuario: string; senha: string };
type LoginResponse = { token: string }; // ajuste se sua API retorna outro nome

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'token';

  constructor(private api: ApiService) {}

  login(payload: LoginRequest) {
    return this.api.post<LoginResponse>('/auth/login', payload).pipe(
      tap(res => this.setToken(res.token))
    );
  }

  logout(): void {
    this.storage?.removeItem(this.tokenKey);
  }

  setToken(token: string): void {
    this.storage?.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return this.storage?.getItem(this.tokenKey) ?? null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private get storage(): Storage | null {
    return (globalThis as any)?.localStorage ?? null;
  }
}
