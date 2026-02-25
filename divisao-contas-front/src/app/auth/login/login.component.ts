import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MaterialModule } from '../../material-module';
import { AuthService } from '../auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MaterialModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  form: FormGroup;

  loading = false;
  errorMessage: string | null = null;
  hide = true;
  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {
    this.form = this.fb.group({
      usuario: ['', Validators.required],
      senha: ['', Validators.required],
    });
  }

  onSubmit(): void {
    if (this.form.invalid || this.loading) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage = null;
    this.loading = true;

    this.authService.login(this.form.value).subscribe({
      next: (resp) => {
        this.loading = false;
        this.router.navigate(['/home']);
      },
      error: (err: any) => {
        this.loading = false;
        if (err?.status === 401 || err?.status === 403) {
          this.errorMessage = 'Usuário ou senha inválidos.';
        } else {
          this.errorMessage = 'Erro ao conectar com o servidor.';
        }
      },
    });
  }
}
