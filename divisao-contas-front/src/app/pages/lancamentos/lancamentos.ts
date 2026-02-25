import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NavigationEnd } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { MaterialModule } from '../../material-module';
import {
  LancamentoService,
  LancamentoCreateRequest,
  LancamentoResponse,
} from './lancamento.service';
import { PessoaService, PessoaResponse } from '../pessoas/pessoa.service';
import { GrupoService, GrupoResponse } from '../grupo/grupo.service';

@Component({
  selector: 'app-lancamentos',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, ReactiveFormsModule],
  templateUrl: './lancamentos.html',
  styleUrl: './lancamentos.scss',
})
export class LancamentosComponent implements OnInit {
  form: FormGroup;

  salvando = false;
  submitted = false;
  modoEdicao = false;

  grupos: GrupoResponse[] = [];
  pessoas: PessoaResponse[] = [];

  lancamentoId: number | null = null;
  private lancamentoEdicaoState: LancamentoResponse | null = null;

  constructor(
    private fb: FormBuilder,
    private lancamentoService: LancamentoService,
    private pessoaService: PessoaService,
    private grupoService: GrupoService,
    private snackBar: MatSnackBar,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      descricao: ['', [Validators.required, Validators.maxLength(100)]],
      data: ['', [Validators.required]],
      valor: ['', [Validators.required]],
      pagadorId: [null, [Validators.required]],
      grupoId: [null],
      divide: [true],
      participantesIds: [[], [Validators.required]],
      devedores: [[]],
    });
  }


  ngOnInit(): void {
    // Carregar grupos e pessoas da API e só preencher edição após ambos carregados
    const pessoas$ = this.pessoaService.listar();
    const grupos$ = this.grupoService.listar();

    forkJoin([pessoas$, grupos$]).subscribe({
      next: ([pessoas, grupos]) => {
        this.pessoas = pessoas;
        this.grupos = grupos;

        // Detecta modo edição
        const idParam = this.route.snapshot.paramMap.get('id');
        if (idParam) {
          this.modoEdicao = true;
          this.lancamentoId = Number(idParam);
          // Tenta pegar do state (navegação), senão busca na API
          const nav = window.history.state;
          if (nav && nav.lancamento && nav.lancamento.id === this.lancamentoId) {
            this.preencherEdicao(nav.lancamento);
          } else {
            this.lancamentoService.listar().subscribe({
              next: (lista) => {
                const lanc = lista.find((l) => l.id === this.lancamentoId);
                if (lanc) this.preencherEdicao(lanc);
              },
            });
          }
        }
      },
      error: () => {
        this.pessoas = [];
        this.grupos = [];
      },
    });
  }

  private preencherEdicao(lanc: LancamentoResponse): void {
    this.lancamentoEdicaoState = lanc;
    // Converter data dd/MM/yyyy para yyyy-MM-dd (input date)
    let dataISO = '';
    if (lanc.data) {
      const [dia, mes, ano] = lanc.data.split('/');
      dataISO = `${ano}-${mes.padStart(2, '0')}-${dia.padStart(2, '0')}`;
    }

    // Deferir o patchValue para o próximo macrotask evita erros de
    // ExpressionChangedAfterItHasBeenCheckedError causados por atualizações de
    // estado dentro do ciclo de change detection.
    setTimeout(() => {
      this.form.patchValue({
        descricao: lanc.descricao,
        data: dataISO,
        valor: lanc.valor,
        pagadorId: lanc.pagador?.id ?? null,
        grupoId: lanc.grupoId ?? null,
        divide: lanc.divide ?? true,
        participantesIds: Array.isArray(lanc.participantes) ? lanc.participantes.map((p) => p.pessoaId).filter(Boolean) : [],
        devedores: Array.isArray(lanc.devedores) ? lanc.devedores.map((d) => d.pessoaId).filter(Boolean) : [],
      });

      this.form.markAsUntouched();
      this.submitted = false;
    });
  }

  salvar(): void {
    if (this.form.invalid || this.salvando) {
      this.submitted = true;
      this.form.markAllAsTouched();
      return;
    }

    this.salvando = true;
    // Construir explicitamente o payload para garantir formato esperado pela API.
    const fv = this.form.value as any;
    const req: any = {
      descricao: fv.descricao,
      data: fv.data,
      valor: fv.valor,
      pagadorId: fv.pagadorId,
      grupoId: fv.grupoId,
      divide: fv.divide,
      participantesIds: Array.isArray(fv.participantesIds) ? fv.participantesIds : [],
      // A API espera lista de objetos {pessoaId, valor} em `devedores` quando divide=false.
      // Para evitar 400 quando divide=true, sempre envie array vazio nesse caso.
      devedores: fv.divide ? [] : (Array.isArray(fv.devedores) ? fv.devedores : []),
    } as LancamentoCreateRequest;

    const acao$ = this.modoEdicao && this.lancamentoId
      ? this.lancamentoService.atualizar(this.lancamentoId, req)
      : this.lancamentoService.criar(req);

    acao$
      .pipe(finalize(() => (this.salvando = false)))
      .subscribe({
        next: () => {
          this.snackBar.open(
            this.modoEdicao ? 'Lançamento atualizado com sucesso!' : 'Lançamento salvo com sucesso!',
            'Fechar',
            { duration: 3000, panelClass: 'snackbar-success' }
          );
            if (!this.modoEdicao) {
              // Após navegação, resetar formulário para evitar obrigatoriedade visual
              const sub = this.router.events.subscribe(event => {
                if (event instanceof NavigationEnd) {
                  this.resetForm();
                  this.submitted = false;
                  sub.unsubscribe();
                }
              });
            }
            this.router.navigate(['/home/lancamentos/pesquisar']);
        },
        error: () => {
          this.snackBar.open(
            this.modoEdicao ? 'Erro ao atualizar lançamento.' : 'Erro ao salvar lançamento.',
            'Fechar',
            { duration: 4000, panelClass: 'snackbar-error' }
          );
        },
      });
  }

  limpar(): void {
    this.resetForm();
    this.submitted = false;
  }

  private resetForm(): void {
    this.form.reset({
      descricao: '',
      data: '',
      valor: '',
      pagadorId: null,
      grupoId: null,
      divide: true,
      participantesIds: [],
      devedores: [],
    });
    this.form.markAsUntouched();
    this.submitted = false;
  }

  trackById(_index: number, item: { id: number | string }): number | string {
    return item?.id;
  }
}