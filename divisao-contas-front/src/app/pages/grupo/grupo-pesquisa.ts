import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { GrupoResponse, GrupoService } from './grupo.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-grupos-pesquisa',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, ReactiveFormsModule],
  templateUrl: './grupo-pesquisa.html',
  styleUrl: './grupo-pesquisa.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GruposPesquisaComponent implements OnInit, AfterViewInit {
  form: FormGroup;
  carregando = false;

  grupos: GrupoResponse[] = [];
  gruposFiltrados: GrupoResponse[] = [];

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private grupoService: GrupoService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group({
      nome: [''],
    });
  }

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    this.carregar();
  }

  novo(): void {
    this.router.navigate(['/home/grupos/cadastrar']);
  }

  editar(grupo: GrupoResponse): void {
    if (!grupo?.id) return;
    this.router.navigate(['/home/grupos/editar', grupo.id], {
      state: { grupo },
    });
  }

  excluir(grupo: GrupoResponse): void {
    if (!grupo?.id) return;

    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '440px',
      data: {
        title: 'Confirmar exclusão',
        message: `Deseja realmente excluir o grupo "${grupo.nome}"?`,
        confirmText: 'Sim',
        cancelText: 'Não',
      },
    });

    ref.afterClosed().subscribe((confirmado) => {
      if (!confirmado) return;
      if (this.carregando) return;

      this.carregando = true;

      this.grupoService
        .excluir(grupo.id)
        .pipe(finalize(() => (this.carregando = false)))
        .subscribe({
          next: () => {
            this.snackBar.open('Grupo excluído com sucesso!', 'OK', { duration: 3000 });
            this.grupos = this.grupos.filter((g) => g.id !== grupo.id);
            this.aplicarFiltro();
            this.cdr.markForCheck();
          },
          error: (err: any) => {
            const apiMsg = err?.error?.mensagem || err?.error?.message;
            const msg = apiMsg ? String(apiMsg) : 'Erro ao excluir grupo.';
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
    this.carregar();
  }

  private carregar(): void {
    if (this.carregando) return;
    this.carregando = true;

    this.grupoService
      .listar()
      .pipe(finalize(() => (this.carregando = false)))
      .subscribe({
        next: (items) => {
          this.grupos = Array.isArray(items) ? items : [];
          this.aplicarFiltro();
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          const msg = apiMsg ? String(apiMsg) : 'Erro ao pesquisar grupos.';
          this.snackBar.open(msg, 'OK', { duration: 5000 });
          this.grupos = [];
          this.gruposFiltrados = [];
        },
      });
  }

  private aplicarFiltro(): void {
    const filtro = String(this.form.value?.nome ?? '').trim().toLowerCase();

    if (!filtro) {
      this.gruposFiltrados = [...this.grupos].sort((a, b) => a.nome.localeCompare(b.nome));
      return;
    }

    this.gruposFiltrados = this.grupos
      .filter((g) => String(g?.nome ?? '').toLowerCase().includes(filtro))
      .sort((a, b) => a.nome.localeCompare(b.nome));
  }
}
