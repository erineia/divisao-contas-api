import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { PessoaResponse, PessoaService } from './pessoa.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-pessoas-pesquisa',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, ReactiveFormsModule],
  templateUrl: './pessoas-pesquisa.html',
  styleUrl: './pessoas-pesquisa.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PessoasPesquisaComponent implements OnInit, AfterViewInit {
  form: FormGroup;
  carregando = false;

  pessoas: PessoaResponse[] = [];
  pessoasFiltradas: PessoaResponse[] = [];

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private pessoaService: PessoaService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group({
      nome: [''],
    });
  }

  ngOnInit(): void {
    // inicializa apenas o formulário aqui
  }

  ngAfterViewInit(): void {
    // após a view estar criada, dispara o carregamento inicial da lista
    this.carregar();
  }

  novo(): void {
    this.router.navigate(['/home/pessoas/cadastrar']);
  }

  editar(pessoa: PessoaResponse): void {
    if (!pessoa?.id) return;
    this.router.navigate(['/home/pessoas/editar', pessoa.id], {
      state: { pessoa },
    });
  }



  excluir(pessoa: PessoaResponse): void {
    if (!pessoa?.id) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '440px',
      data: {
        title: 'Confirmar exclusão',
        message: `Deseja realmente excluir a pessoa "${pessoa.nome}"?`,
        confirmText: 'Sim',
        cancelText: 'Não',
      },
    });

    ref.afterClosed().subscribe((confirmado) => {
      if (!confirmado) return;

      if (this.carregando) return;
      this.carregando = true;

      this.pessoaService
        .excluir(pessoa.id)
        .pipe(finalize(() => (this.carregando = false)))
        .subscribe({
          next: () => {
            this.snackBar.open('Pessoa excluída com sucesso!', 'OK', { duration: 3000 });

            // remove a pessoa excluída da lista atual em memória
            this.pessoas = this.pessoas.filter((p) => p.id !== pessoa.id);
            this.aplicarFiltro();
            this.cdr.markForCheck();
          },
          error: (err: any) => {
            const apiMsg = err?.error?.mensagem || err?.error?.message;
            const msg = apiMsg ? String(apiMsg) : 'Erro ao excluir pessoa.';
            this.snackBar.open(msg, 'OK', { duration: 5000 });
          },
        });
    });
  }

  limpar(): void {
    this.form.reset();
    this.aplicarFiltro();
  }

  pesquisar(): void {
    // executa novamente a consulta na API, respeitando o filtro digitado
    this.carregar();
  }

  private carregar(): void {
    if (this.carregando) {
      return;
    }
    this.carregando = true;

    this.pessoaService
      .listar()
      .pipe(finalize(() => (this.carregando = false)))
      .subscribe({
        next: (items) => {
          this.pessoas = Array.isArray(items) ? items : [];
          this.aplicarFiltro();
          // informa ao Angular que houve mudança de dados sob OnPush
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          // mantém tratamento de erro via snackbar
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          const msg = apiMsg ? String(apiMsg) : 'Erro ao pesquisar pessoas.';
          this.snackBar.open(msg, 'OK', { duration: 5000 });
          this.pessoas = [];
          this.pessoasFiltradas = [];
        },
      });
  }

  private aplicarFiltro(): void {
    const filtro = String(this.form.value?.nome ?? '').trim().toLowerCase();

    if (!filtro) {
      this.pessoasFiltradas = [...this.pessoas].sort((a, b) => a.nome.localeCompare(b.nome));
      return;
    }

    this.pessoasFiltradas = this.pessoas
      .filter((p) => String(p?.nome ?? '').toLowerCase().includes(filtro))
      .sort((a, b) => a.nome.localeCompare(b.nome));
  }
}
