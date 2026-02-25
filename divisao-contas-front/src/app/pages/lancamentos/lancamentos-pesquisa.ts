import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';
import { MatTableModule } from '@angular/material/table';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { LancamentoService, LancamentoResponse } from './lancamento.service';
import { GrupoService, GrupoResponse } from '../grupo/grupo.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-lancamentos-pesquisa',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, MatTableModule, MatProgressBarModule, ReactiveFormsModule],
  templateUrl: './lancamentos-pesquisa.html',
  styleUrl: './lancamentos-pesquisa.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LancamentosPesquisaComponent implements OnInit {
  form: FormGroup;
  carregando = false;
  lancamentos: LancamentoResponse[] = [];
  lancamentosFiltrados: LancamentoResponse[] = [];
  grupos: GrupoResponse[] = [];

  constructor(
    private fb: FormBuilder,
    private lancamentoService: LancamentoService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef,
    private router: Router,
    private grupoService: GrupoService,
  ) {
    this.form = this.fb.group({
      descricao: [''],
      grupoId: [null],
    });
  }
  novo(): void {
    this.router.navigate(['/home/lancamentos/cadastrar']);
  }

  editar(lancamento: LancamentoResponse): void {
    if (!lancamento?.id) return;
    this.router.navigate(['/home/lancamentos/editar', lancamento.id], { state: { lancamento } });
  }

  excluir(lancamento: LancamentoResponse): void {
    if (!lancamento?.id) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '440px',
      data: {
        title: 'Confirmar exclusão',
        message: `Deseja realmente excluir o lançamento "${lancamento.descricao}"?`,
        confirmText: 'Sim',
        cancelText: 'Não',
      },
    });

    ref.afterClosed().subscribe((confirmado) => {
      if (!confirmado) return;
      if (this.carregando) return;
      this.carregando = true;
      this.lancamentoService
        .excluir(lancamento.id)
        .pipe(finalize(() => {
          this.carregando = false;
          this.cdr.markForCheck();
        }))
        .subscribe({
          next: () => {
            this.snackBar.open('Lançamento excluído com sucesso!', 'OK', { duration: 3000 });
            this.lancamentos = this.lancamentos.filter((l) => l.id !== lancamento.id);
            this.aplicarFiltro();
          },
          error: (err: any) => {
            const apiMsg = err?.error?.mensagem || err?.error?.message;
            const msg = apiMsg ? String(apiMsg) : 'Erro ao excluir lançamento.';
            this.snackBar.open(msg, 'OK', { duration: 5000 });
          },
        });
    });
  }

  ngOnInit(): void {
    this.carregar();
    this.grupoService.listar().subscribe({
      next: (g: GrupoResponse[]) => {
        this.grupos = Array.isArray(g) ? g : [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.grupos = [];
      },
    });
  }

  pesquisar(): void {
    this.carregar();
  }

  limpar(): void {
    this.form.reset();
    this.aplicarFiltro();
  }

  private carregar(): void {
    if (this.carregando) return;
    this.carregando = true;
    this.lancamentoService
      .listar()
      .pipe(finalize(() => {
        this.carregando = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (dados) => {
          this.lancamentos = Array.isArray(dados) ? dados : [];
          this.aplicarFiltro();
        },
        error: (err: any) => {
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          const msg = apiMsg ? String(apiMsg) : 'Erro ao pesquisar lançamentos.';
          this.snackBar.open(msg, 'OK', { duration: 5000 });
          this.lancamentos = [];
          this.lancamentosFiltrados = [];
        },
      });
  }

  private aplicarFiltro(): void {
    const { descricao, grupoId } = this.form.value;
    const filtroDescricao = String(descricao || '').toLowerCase();

    let resultado = [...this.lancamentos];

    if (filtroDescricao) {
      resultado = resultado.filter((l) => (l.descricao || '').toLowerCase().includes(filtroDescricao));
    }
    if (grupoId != null) {
      resultado = resultado.filter((l) => (l.grupoId ?? null) === grupoId);
    }

    resultado.sort((a, b) => (a.data || '').localeCompare(b.data || ''));
    this.lancamentosFiltrados = resultado;
  }
  trackById(_index: number, item: { id: number | string }): number | string {
  return item?.id;
}
}
