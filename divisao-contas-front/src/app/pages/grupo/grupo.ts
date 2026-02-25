import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NotificationService } from '../../shared/notification.service';
import { finalize } from 'rxjs';
import { GrupoResponse, GrupoService } from './grupo.service';

@Component({
  selector: 'app-grupo',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, ReactiveFormsModule],
  templateUrl: './grupo.html',
  styleUrl: './grupo.scss',
})
export class GrupoComponent implements OnInit {
  form: FormGroup;
  salvando = false;
  modoEdicao = false;
  grupoId: number | null = null;
  submitted = false;

  constructor(
    private fb: FormBuilder,
    private grupoService: GrupoService,
    private snackBar: MatSnackBar,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    this.form = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    });
  }

  ngOnInit(): void {
    const stateGrupo = (history.state as { grupo?: GrupoResponse }).grupo;
    if (stateGrupo && stateGrupo.id) {
      this.modoEdicao = true;
      this.grupoId = stateGrupo.id;
      this.form.patchValue({ nome: stateGrupo.nome });
      return;
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam != null ? Number(idParam) : NaN;

    if (!Number.isNaN(id) && id > 0) {
      this.modoEdicao = true;
      this.grupoId = id;

      this.grupoService.buscarPorId(id).subscribe({
        next: (grupo: GrupoResponse) => {
          this.form.patchValue({ nome: grupo.nome });
        },
        error: (err: any) => {
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          const msg = apiMsg ? String(apiMsg) : 'Erro ao carregar dados do grupo.';
          this.snackBar.open(msg, 'OK', { duration: 5000 });
        },
      });
    }
  }

  limpar(): void {
    this.form.reset();
    this.submitted = false;
  }

  salvar(): void {
    if (this.salvando) return;

    this.submitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notification.warn('Por favor, preencha todos os campos obrigatórios.');
      return;
    }

    const nome = String(this.form.value?.nome ?? '').trim();
    if (!nome) {
      this.form.markAllAsTouched();
      this.notification.warn('Por favor, preencha todos os campos obrigatórios.');
      return;
    }

    this.salvando = true;

    const request = { nome };

    const acao$ = this.modoEdicao && this.grupoId != null
      ? this.grupoService.atualizar(this.grupoId, request)
      : this.grupoService.criar(request);

    acao$
      .pipe(finalize(() => (this.salvando = false)))
      .subscribe({
        next: () => {
          const msg = this.modoEdicao ? 'Grupo atualizado com sucesso!' : 'Grupo salvo com sucesso!';
          this.snackBar.open(msg, 'OK', { duration: 3000 });
          this.router.navigate(['/home/grupos/pesquisar']);
        },
        error: (err: any) => {
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          let msg: string;

          if (err?.status === 409) {
            msg = 'Já existe um grupo com esse nome.';
          } else {
            msg = apiMsg
              ? String(apiMsg)
              : this.modoEdicao
                ? 'Erro ao atualizar grupo.'
                : 'Erro ao salvar grupo.';
          }

          this.snackBar.open(msg, 'OK', { duration: 5000 });
        },
      });
  }
}
