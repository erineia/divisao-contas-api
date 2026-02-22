import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { PagamentoCreateRequest, PagamentoResponse, PagamentoService } from './pagamento.service';
import { PessoaResponse, PessoaService } from '../pessoas/pessoa.service';
import { GrupoResponse, GrupoService } from '../grupo/grupo.service';

@Component({
  selector: 'app-pagamentos',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, ReactiveFormsModule],
  templateUrl: './pagamentos.html',
  styleUrl: './pagamentos.scss',
})
export class PagamentosComponent implements OnInit {
  form: FormGroup;
  salvando = false;
  modoEdicao = false;
  pagamentoId: number | null = null;

  pessoas: PessoaResponse[] = [];
  grupos: GrupoResponse[] = [];

  private pagamentoEdicaoState: PagamentoResponse | null = null;

  constructor(
    private fb: FormBuilder,
    private pagamentoService: PagamentoService,
    private pessoaService: PessoaService,
    private grupoService: GrupoService,
    private snackBar: MatSnackBar,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    this.form = this.fb.group({
      data: ['', [Validators.required]],
      valor: [null, [Validators.required, Validators.min(0.01)]],
      pagadorId: [null, [Validators.required]],
      recebedorId: [null, [Validators.required]],
      grupoId: [null],
      observacao: [''],
    });
  }

  ngOnInit(): void {
    const statePagamento = (history.state as { pagamento?: PagamentoResponse }).pagamento || null;
    if (statePagamento && statePagamento.id) {
      this.modoEdicao = true;
      this.pagamentoId = statePagamento.id;
      this.pagamentoEdicaoState = statePagamento;
    }

    this.carregarPessoasEGrupos();
  }

  private carregarPessoasEGrupos(): void {
    this.pessoaService.listar().subscribe({
      next: (pessoas) => {
        this.pessoas = pessoas;
        this.tentarPreencherFormularioEdicao();
      },
      error: () => {
        this.snackBar.open('Erro ao carregar pessoas para seleção.', 'OK', { duration: 5000 });
      },
    });

    this.grupoService.listar().subscribe({
      next: (grupos) => {
        this.grupos = grupos;
        this.tentarPreencherFormularioEdicao();
      },
      error: () => {
        this.snackBar.open('Erro ao carregar grupos para seleção.', 'OK', { duration: 5000 });
      },
    });
  }

  private tentarPreencherFormularioEdicao(): void {
    if (!this.pagamentoEdicaoState) return;

    const pagamento = this.pagamentoEdicaoState;

    const dataParts = (pagamento.data || '').split('/');
    const dataIso =
      dataParts.length === 3 ? `${dataParts[2]}-${dataParts[1].padStart(2, '0')}-${dataParts[0].padStart(2, '0')}` : '';

    const pagador = this.pessoas.find((p) => p.nome === pagamento.pagador);
    const recebedor = this.pessoas.find((p) => p.nome === pagamento.recebedor);

    this.form.patchValue({
      data: dataIso,
      valor: pagamento.valor,
      pagadorId: pagador?.id ?? null,
      recebedorId: recebedor?.id ?? null,
      grupoId: pagamento.grupoId ?? null,
      observacao: pagamento.observacao ?? '',
    });
  }

  limpar(): void {
    this.form.reset();
  }

  salvar(): void {
    if (this.salvando) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.snackBar.open('Preencha todos os campos obrigatórios do pagamento.', 'OK', { duration: 3000 });
      return;
    }

    const pagadorId = this.form.value.pagadorId as number | null;
    const recebedorId = this.form.value.recebedorId as number | null;

    if (pagadorId != null && recebedorId != null && pagadorId === recebedorId) {
      this.snackBar.open('Pagador e recebedor devem ser pessoas diferentes.', 'OK', { duration: 4000 });
      return;
    }

    const data = String(this.form.value.data ?? '').trim();
    const valor = Number(this.form.value.valor ?? 0);

    if (!data || !valor || valor <= 0) {
      this.snackBar.open('Informe uma data e um valor válidos.', 'OK', { duration: 4000 });
      return;
    }

    const req: PagamentoCreateRequest = {
      data,
      valor,
      pagadorId: pagadorId!,
      recebedorId: recebedorId!,
      grupoId: (this.form.value.grupoId as number | null) ?? null,
      observacao: (this.form.value.observacao as string | null)?.trim() || null,
    };

    this.salvando = true;

    const acao$ = this.modoEdicao && this.pagamentoId != null
      ? this.pagamentoService.atualizar(this.pagamentoId, req)
      : this.pagamentoService.criar(req);

    acao$
      .pipe(finalize(() => (this.salvando = false)))
      .subscribe({
        next: () => {
          const msg = this.modoEdicao ? 'Pagamento atualizado com sucesso!' : 'Pagamento salvo com sucesso!';
          this.snackBar.open(msg, 'OK', { duration: 3000 });
          this.router.navigate(['/home/pagamentos/pesquisar']);
        },
        error: (err: any) => {
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          const msg = apiMsg
            ? String(apiMsg)
            : this.modoEdicao
              ? 'Erro ao atualizar pagamento.'
              : 'Erro ao salvar pagamento.';
          this.snackBar.open(msg, 'OK', { duration: 5000 });
        },
      });
  }
}
