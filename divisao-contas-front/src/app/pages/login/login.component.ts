import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { finalize, timeout } from 'rxjs';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})

export class LoginComponent {
  usuario = 'admin';
  senha = '123456';

  loading = false;
  errorMessage: string | null = null;

  constructor(
    private auth: AuthService,
    private router: Router
  ) {}

  entrar(): void {
    if (this.loading) return;

    this.errorMessage = null;
    this.loading = true;

    this.auth
      .login({ usuario: this.usuario, senha: this.senha })
      .pipe(
        timeout({ first: 10000 }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/']);
        },
        error: (err: any) => {
          if (err?.name === 'TimeoutError' || err?.status === 0) {
            this.errorMessage = 'Não foi possível conectar ao servidor.';
            return;
          }
          this.errorMessage = 'Usuário e/ou senha inválidos.';
        }
      });

  }
}
