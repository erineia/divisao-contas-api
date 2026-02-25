import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NotificationService } from '../../shared/notification.service';
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
    private notification: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group({
      dataInicio: [''],
      dataFim: [''],
      pagador: ['', [Validators.maxLength(80)]],
      recebedor: ['', [Validators.maxLength(80)]],
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
      panelClass: 'confirm-dialog-panel',
      width: 'min(420px, 92vw)',
      maxWidth: '92vw',
      autoFocus: false,
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
    // validação de range: se ambas as datas preenchidas, garante fim >= inicio
    const dataInicioVal: any = this.form.value.dataInicio;
    const dataFimVal: any = this.form.value.dataFim;
    if (dataInicioVal && dataFimVal) {
      const ini = this.parseToDate(dataInicioVal);
      const fim = this.parseToDate(dataFimVal);
      if (ini && fim && fim.getTime() < ini.getTime()) {
        this.notification.warn('Data Final não pode ser menor que Data Início');
        return;
      }
    }

    this.carregar();
  }

  private parseToDate(value: any): Date | null {
    if (value instanceof Date && !isNaN(value.getTime())) return value;

    if (typeof value === 'string') {
      // tenta dd/MM/yyyy
      const matchBr = value.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
      if (matchBr) {
        const [, day, month, year] = matchBr;
        const date = new Date(Number(year), Number(month) - 1, Number(day));
        if (
          date.getDate() === Number(day) &&
          date.getMonth() === Number(month) - 1 &&
          date.getFullYear() === Number(year)
        ) {
          return date;
        }
        return null;
      }

      // tenta yyyy-MM-dd
      const matchIso = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
      if (matchIso) {
        const [, year, month, day] = matchIso;
        const date = new Date(Number(year), Number(month) - 1, Number(day));
        if (
          date.getDate() === Number(day) &&
          date.getMonth() === Number(month) - 1 &&
          date.getFullYear() === Number(year)
        ) {
          return date;
        }
        return null;
      }
    }

    return null;
  }

  private formatToApiDate(value: any): string {
    if (!value) return '';
    if (value instanceof Date && !isNaN(value.getTime())) {
      const y = value.getFullYear();
      const m = String(value.getMonth() + 1).padStart(2, '0');
      const d = String(value.getDate()).padStart(2, '0');
      return `${y}-${m}-${d}`;
    }
    if (typeof value === 'string') return value.trim();
    return '';
  }

  private carregar(): void {
    const dataInicio: string = this.formatToApiDate(this.form.value.dataInicio);
    const dataFim: string = this.formatToApiDate(this.form.value.dataFim);

    this.carregando = true;

    const fonte$ =
      dataInicio && dataFim
        ? this.pagamentoService.listarPorPeriodo(dataInicio, dataFim)
        : this.pagamentoService.listar();

    fonte$
      .pipe(
        finalize(() => {
          this.carregando = false;
          this.cdr.markForCheck();
        }),
      )
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
