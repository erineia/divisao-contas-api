import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { PagamentoResponse, PagamentoService } from './pagamento.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-pagamentos-pesquisa',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, ReactiveFormsModule],
  templateUrl: './pagamentos-pesquisa.html',
  styleUrl: './pagamentos-pesquisa.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PagamentosPesquisaComponent implements OnInit {
  form: FormGroup;
  carregando = false;

  pagamentos: PagamentoResponse[] = [];
  pagamentosFiltrados: PagamentoResponse[] = [];

  constructor(
    private fb: FormBuilder,
    private pagamentoService: PagamentoService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group({
      dataInicio: [''],
      dataFim: [''],
      pagador: [''],
      recebedor: [''],
    });
  }

  ngOnInit(): void {
    this.carregar();
  }

  novo(): void {
    this.router.navigate(['/home/pagamentos/cadastrar']);
  }

  editar(pagamento: PagamentoResponse): void {
    if (!pagamento?.id) return;
    this.router.navigate(['/home/pagamentos/editar', pagamento.id], { state: { pagamento } });
  }

  excluir(pagamento: PagamentoResponse): void {
    if (!pagamento?.id) return;

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '360px',
      data: {
        title: 'Confirmação',
        message: `Deseja realmente excluir o pagamento de ${pagamento.pagador} para ${pagamento.recebedor} em ${pagamento.data}?`,
        confirmLabel: 'Excluir',
        cancelLabel: 'Cancelar',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;

      this.pagamentoService.excluir(pagamento.id).subscribe({
        next: () => {
          this.pagamentos = this.pagamentos.filter((p) => p.id !== pagamento.id);
          this.aplicarFiltro();
          this.cdr.markForCheck();
          this.snackBar.open('Pagamento excluído com sucesso.', 'OK', { duration: 3000 });
        },
        error: (err: any) => {
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          const msg = apiMsg ? String(apiMsg) : 'Erro ao excluir pagamento.';
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
    const dataInicio: string = (this.form.value.dataInicio || '').trim();
    const dataFim: string = (this.form.value.dataFim || '').trim();

    this.carregando = true;

    const fonte$ = dataInicio && dataFim
      ? this.pagamentoService.listarPorPeriodo(dataInicio, dataFim)
      : this.pagamentoService.listar();

    fonte$
      .pipe(finalize(() => {
        this.carregando = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (lista: PagamentoResponse[]) => {
          this.pagamentos = lista || [];
          this.aplicarFiltro();
        },
        error: (err: any) => {
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          const msg = apiMsg ? String(apiMsg) : 'Erro ao carregar pagamentos.';
          this.snackBar.open(msg, 'OK', { duration: 5000 });
          this.pagamentos = [];
          this.pagamentosFiltrados = [];
        },
      });
  }

  private aplicarFiltro(): void {
    const { pagador, recebedor } = this.form.value;
    const filtroPagador = String(pagador || '').toLowerCase();
    const filtroRecebedor = String(recebedor || '').toLowerCase();

    let resultado = [...this.pagamentos];

    if (filtroPagador) {
      resultado = resultado.filter((p) => p.pagador.toLowerCase().includes(filtroPagador));
    }

    if (filtroRecebedor) {
      resultado = resultado.filter((p) => p.recebedor.toLowerCase().includes(filtroRecebedor));
    }

    resultado.sort((a, b) => (a.data || '').localeCompare(b.data || ''));

    this.pagamentosFiltrados = resultado;
  }
}
