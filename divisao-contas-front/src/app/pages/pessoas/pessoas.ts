import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NotificationService } from '../../shared/notification.service';
import { PessoaResponse, PessoaService } from './pessoa.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-pessoas',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, ReactiveFormsModule],
  templateUrl: './pessoas.html',
  styleUrl: './pessoas.scss',
})
export class PessoasComponent implements OnInit {
  form: FormGroup;
  salvando = false;
  modoEdicao = false;
  pessoaId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private pessoaService: PessoaService,
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
    // 1) tenta recuperar a pessoa enviada na navegação (mais rápido, sem nova chamada)
    const statePessoa = (history.state as { pessoa?: PessoaResponse }).pessoa;
    if (statePessoa && statePessoa.id) {
      this.modoEdicao = true;
      this.pessoaId = statePessoa.id;
      this.form.patchValue({ nome: statePessoa.nome });
      return;
    }

    // 2) se não veio no state, carrega pela rota /pessoas/editar/:id
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam != null ? Number(idParam) : NaN;

    if (!Number.isNaN(id) && id > 0) {
      this.modoEdicao = true;
      this.pessoaId = id;

      this.pessoaService.buscarPorId(id).subscribe({
        next: (pessoa: PessoaResponse) => {
          this.form.patchValue({ nome: pessoa.nome });
        },
        error: (err: any) => {
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          const msg = apiMsg ? String(apiMsg) : 'Erro ao carregar dados da pessoa.';
          this.snackBar.open(msg, 'OK', { duration: 5000 });
        },
      });
    }
  }

  limpar(): void {
    this.form.reset();
  }

  salvar(): void {
    if (this.salvando) return;

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

    const acao$ = this.modoEdicao && this.pessoaId != null
      ? this.pessoaService.atualizar(this.pessoaId, request)
      : this.pessoaService.criar(request);

    acao$
      .pipe(finalize(() => (this.salvando = false)))
      .subscribe({
        next: () => {
          const msg = this.modoEdicao ? 'Pessoa atualizada com sucesso!' : 'Pessoa salva com sucesso!';
          this.snackBar.open(msg, 'OK', { duration: 3000 });

          // após salvar (inclusão ou edição), volta para a tela de pesquisa
          this.router.navigate(['/home/pessoas/pesquisar']);
        },
        error: (err: any) => {
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          let msg: string;

          if (err?.status === 409) {
            msg = 'Já existe uma pessoa cadastrada com esse nome.';
          } else {
            msg = apiMsg
              ? String(apiMsg)
              : this.modoEdicao
                ? 'Erro ao atualizar pessoa.'
                : 'Erro ao salvar pessoa.';
          }

          this.snackBar.open(msg, 'OK', { duration: 5000 });
        },
      });
  }
}
