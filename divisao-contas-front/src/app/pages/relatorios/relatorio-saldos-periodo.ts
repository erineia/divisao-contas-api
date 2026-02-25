import { CommonModule } from '@angular/common';
import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { MaterialModule } from '../../material-module';
import { RelatorioService } from './relatorio.service';
import { GrupoService, GrupoResponse } from '../grupo/grupo.service';
import { LancamentoService, LancamentoResponse } from '../lancamentos/lancamento.service';

type SaldoPessoa = { nome: string; valor: number };
type Transferencia = { devedor: string; credor: string; valor: number };

// (opcional) estrutura de lançamento se você tiver isso no backend futuramente
type LancamentoLinha = {
  descricao: string;
  data: Date | string;
  valor: number;
  quemPagou: string;
  divididoCom: string[] | string;
  valorPorPessoa: number;
  obs?: string;
};

@Component({
  selector: 'app-relatorio-saldos-periodo',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, ReactiveFormsModule],
  templateUrl: './relatorio-saldos-periodo.html',
  styleUrls: ['./relatorio-saldos-periodo.scss'],
})
export class RelatorioSaldosPeriodoComponent implements OnInit {
  filtrosForm!: FormGroup;

  carregando = false;
  relatorioGerado = false;

  periodoLabel = '';

  saldos: SaldoPessoa[] = [];
  saldosDisplay: SaldoPessoa[] = [];
  transferencias: Transferencia[] = [];
  transferenciasAposAbatimento: Transferencia[] = [];
  totalPeriodo = 0;
  qtdLancamentos = 0;

  // Se você ainda não tem lançamentos vindo do backend, deixa vazio.
  // Quando tiver, é só preencher no "next" do gerarRelatorio.
  lancamentos: LancamentoLinha[] = [];

  grupos: GrupoResponse[] = [];

  constructor(
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private relatorioService: RelatorioService,
    private grupoService: GrupoService,
    private lancamentoService: LancamentoService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.filtrosForm = this.fb.group(
      {
        dataInicio: [null, Validators.required],
        dataFim: [null, Validators.required],
        grupo: [null],
      },
      { validators: [this.dateRangeValidator()] }, // ✅ validação de range no form
    );

    this.grupoService.listar().subscribe({
      next: (grupos) => {
        this.grupos = grupos || [];
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Erro ao carregar grupos:', err);
        this.snackBar.open('Erro ao carregar grupos.', 'Fechar', {
          duration: 3000,
          panelClass: 'snackbar-error',
        });
      },
    });

    // ✅ Revalida range quando qualquer data mudar
    this.filtrosForm.get('dataInicio')?.valueChanges.subscribe(() => {
      this.filtrosForm.updateValueAndValidity({ emitEvent: false });
    });
    this.filtrosForm.get('dataFim')?.valueChanges.subscribe(() => {
      this.filtrosForm.updateValueAndValidity({ emitEvent: false });
    });
  }

  limpar(): void {
    this.filtrosForm.reset();
    this.saldos = [];
    this.transferencias = [];
    this.totalPeriodo = 0;
    this.qtdLancamentos = 0;
    this.lancamentos = [];
    this.relatorioGerado = false;
    this.periodoLabel = '';
                // Se houver lançamentos, calcula saldos a partir dos dados reais (ids) e define saldosDisplay
  }

  /**
   * gerarRelatorio()
   * - Se exportarAposGerar = true, ao finalizar já baixa o CSV formatado.
   */
  gerarRelatorio(exportarAposGerar = false): void {
    if (this.carregando) return;

    // marca tudo como tocado pra exibir erros no HTML
    this.filtrosForm.markAllAsTouched();

    const periodo = this.getPeriodoOrShowError();
    if (!periodo) return;

    const { dataInicio, dataFim, dataInicioStr, dataFimStr } = periodo;
    const inicio = dataInicio;
    const fim = dataFim;

    // grupo agora armazena o `id` do grupo (number) ou null
    const grupoId: number | null = this.filtrosForm.value.grupo ?? null;

    this.periodoLabel = `${dataInicioStr} a ${dataFimStr}`;

    this.carregando = true;

    this.relatorioService
      .baixarRelatorioSaldosPeriodoCSVTextoComGrupo(dataInicioStr, dataFimStr, grupoId)
      .pipe(
        finalize(() => {
          this.carregando = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (csvText: string) => {
          const saldos = this.parseCsvToSaldos(csvText);

          // Se houver dados do backend (CSV), mantemos como fallback
          const parsedSaldos = saldos || [];

          // Busca lançamentos via API para calcular saldos a partir dos lançamentos (preferível)
          this.lancamentoService.listar().subscribe({
            next: (lista) => {
              try {
                const rawLanc = (lista || []).filter((it: LancamentoResponse) => {
                  const d = this.parseToDate(it.data);
                  if (!d) return false;
                  if (d.getTime() < inicio.getTime() || d.getTime() > fim.getTime()) return false;
                  if (grupoId !== null && (it as any).grupoId !== grupoId) return false;
                  return true;
                }) as LancamentoResponse[];

                // mapeia para exibição (CSV) e também mantém rawLanc para cálculos
                const itens = rawLanc.map((it: LancamentoResponse) => {
                  const divididoCom = (it.participantes || []).map((p: any) => p.nome);
                  const valorPorPessoa = (it.participantes && it.participantes.length) ? (it.participantes[0].valor ?? 0) : (it.valor ?? 0);
                  return {
                    descricao: it.descricao,
                    data: this.parseToDate(it.data) || it.data,
                    valor: it.valor ?? 0,
                    quemPagou: it.pagador?.nome ?? '',
                    divididoCom,
                    valorPorPessoa,
                    obs: it.observacao ?? '',
                  } as LancamentoLinha;
                });

                this.lancamentos = itens;
                this.qtdLancamentos = itens.length;

                // Se houver lançamentos, calcula saldos a partir dos dados reais (ids)
                if (rawLanc.length > 0) {
                  const computed = this.computeSaldosFromLancamentos(rawLanc);
                  this.saldos = computed.net;
                  this.saldosDisplay = computed.paid;
                } else {
                  // fallback para saldos parseados do CSV
                  this.saldos = parsedSaldos;
                  this.saldosDisplay = parsedSaldos;
                }

                // Em vez de apenas fazer o settlement por saldos líquidos, gerar
                // as transferências agregadas por par (devedor -> credor) a
                // partir dos lançamentos. Isso mostra todas as dívidas
                // individuais mesmo que a pessoa tenha saldo positivo.
                this.transferencias = this.computePairwiseDebtsFromLancamentos(rawLanc);
                this.totalPeriodo = this.saldos.filter((s) => s.valor > 0).reduce((acc, s) => acc + s.valor, 0);

                // valores calculados (logs de depuração removidos)

                // Calcula transferências após abatimento a partir das transferencias agregadas
                this.transferenciasAposAbatimento = this.computeRemainingAfterAbatimento(this.transferencias);

                this.relatorioGerado = this.saldos.length > 0;
                this.cdr.markForCheck();

                if (exportarAposGerar && this.relatorioGerado) {
                  this.exportarCSV(true);
                }
              } catch (e) {
                console.error('Erro ao filtrar lançamentos:', e);
              }
            },
            error: (e) => {
              console.error('Erro ao buscar lançamentos:', e);
              // em caso de erro ao buscar lançamentos, usa saldos parseados do CSV
              if (parsedSaldos && parsedSaldos.length) {
                this.saldos = parsedSaldos;
                this.transferencias = this.calcularTransferencias(this.saldos);
                this.totalPeriodo = this.saldos.filter((s) => s.valor > 0).reduce((acc, s) => acc + s.valor, 0);
                this.relatorioGerado = true;
                this.cdr.markForCheck();
              } else {
                this.snackBar.open('Relatório vazio para o período selecionado.', 'Fechar', { duration: 3000 });
              }
            },
          });
        },
        error: (err: any) => {
          console.error('Erro ao gerar relatório:', err);
          const apiMsg = err?.error?.mensagem || err?.error?.message;
          const msg = apiMsg ? String(apiMsg) : 'Erro ao gerar relatório.';
          this.snackBar.open(msg, 'Fechar', { duration: 4000, panelClass: 'snackbar-error' });

          this.relatorioGerado = false;
          this.saldos = [];
          this.transferencias = [];
          this.totalPeriodo = 0;
          this.qtdLancamentos = 0;
          this.lancamentos = [];
          this.cdr.detectChanges();
        },
      });
  }

  exportarCSV(exportarSemGerar = false): void {
    if (!this.relatorioGerado) {
      if (exportarSemGerar) return;
      this.gerarRelatorio(true);
      return;
    }

    const csv = this.buildCsvNoFormatoDoPrint();

    const csvExcel = '\ufeff' + csv.replace(/\n/g, '\r\n');

    const blob = new Blob([csvExcel], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = `relatorio-saldos-${this.slugPeriodo()}.csv`;
    a.click();

    window.URL.revokeObjectURL(url);
  }
  // =========================
  // Validação de Range no FormGroup
  // =========================

  private dateRangeValidator() {
    return (group: AbstractControl): ValidationErrors | null => {
      const ini = this.parseToDate(group.get('dataInicio')?.value);
      const fim = this.parseToDate(group.get('dataFim')?.value);

      // não valida range se ainda falta alguma data válida (required já cuida disso)
      if (!ini || !fim) return null;

      return ini.getTime() <= fim.getTime() ? null : { dateRangeInvalid: true };
    };
  }

  // =========================
  // Helpers (período / datas)
  // =========================

  private getPeriodoOrShowError(): {
    dataInicio: Date;
    dataFim: Date;
    dataInicioStr: string;
    dataFimStr: string;
  } | null {
    if (this.filtrosForm.invalid) {
      const rangeInvalid = this.filtrosForm.hasError('dateRangeInvalid');
      const msg = rangeInvalid
        ? 'Data final deve ser maior ou igual à inicial.'
        : 'Selecione o período! Certifique-se de digitar datas válidas (dd/MM/yyyy).';

      this.snackBar.open(msg, 'Fechar', { duration: 3500, panelClass: 'snackbar-error' });
      return null;
    }

    const dataInicio = this.parseToDate(this.filtrosForm.value.dataInicio);
    const dataFim = this.parseToDate(this.filtrosForm.value.dataFim);
    if (!dataInicio || !dataFim) return null;

    return {
      dataInicio,
      dataFim,
      dataInicioStr: this.formatDateBr(dataInicio),
      dataFimStr: this.formatDateBr(dataFim),
    };
  }

  private parseToDate(value: any): Date | null {
    if (value instanceof Date && !isNaN(value.getTime())) return value;

    if (typeof value === 'string') {
      // dd/MM/yyyy
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

      // yyyy-MM-dd
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

  private formatDateBr(date: Date): string {
    const d = new Date(date);
    const day = d.getDate().toString().padStart(2, '0');
    const month = (d.getMonth() + 1).toString().padStart(2, '0');
    const year = d.getFullYear();
    return `${day}/${month}/${year}`;
  }

  private slugPeriodo(): string {
    // ex: 16-02-2026_a_23-02-2026
    return (this.periodoLabel || 'periodo')
      .replace(/\s+a\s+/g, '_a_')
      .replace(/\//g, '-')
      .replace(/\s+/g, '_');
  }

  // =========================
  // CSV no formato do print
  // =========================

  private buildCsvNoFormatoDoPrint(): string {
    const sep = ';';
    const linhas: string[] = [];

    // Cabeçalho
    linhas.push(['Relatorio', this.periodoLabel || ''].map(this.escCsv.bind(this)).join(sep));
    linhas.push('');

    // Seção de lançamentos (se você tiver)
    linhas.push(
      [
        'LancamentosDescricao',
        'Data',
        'Valor',
        'Quem Pagou',
        'Dividido com',
        'Valor por pessoa',
        'Obs',
      ]
        .map(this.escCsv.bind(this))
        .join(sep),
    );

    if (this.lancamentos?.length) {
      for (const l of this.lancamentos) {
        const valorNum = Number(l.valor) || 0;
        const valorPorPessoaNum = Number(l.valorPorPessoa) || 0;
        linhas.push(
          [
            l.descricao,
            this.toPtDate(l.data),
            this.toPtMoney(valorNum),
            l.quemPagou,
            Array.isArray(l.divididoCom) ? l.divididoCom.join(', ') : l.divididoCom,
            this.toPtMoney(valorPorPessoaNum),
            l.obs ?? '',
          ]
            .map(this.escCsv.bind(this))
            .join(sep),
        );
      }
    }

    linhas.push('');

    // Seção: Total Gasto por Pessoa (usar os valores exibidos na tela)
    linhas.push(this.escCsv('Total Gasto por Pessoa'));
    linhas.push(['Nome', 'Total Gasto'].map(this.escCsv.bind(this)).join(sep));
    const saldosParaCsv = (this.saldosDisplay && this.saldosDisplay.length) ? this.saldosDisplay : this.saldos;
    if (saldosParaCsv?.length) {
      for (const s of saldosParaCsv) {
        linhas.push([s.nome, this.toPtMoney(s.valor)].map(this.escCsv.bind(this)).join(sep));
      }
    } else {
      linhas.push(this.escCsv('Nenhum saldo para o período'));
    }

    linhas.push('');
    linhas.push(this.escCsv('Quem deve (acumulado)'));
    linhas.push(['Deve', 'Recebe', 'Valor'].map(this.escCsv.bind(this)).join(sep));

    // Transferências calculadas a partir dos saldos
    if (this.transferencias?.length) {
      for (const t of this.transferencias) {
        linhas.push([t.devedor, t.credor, this.toPtMoney(t.valor)].map(this.escCsv.bind(this)).join(sep));
      }
    } else {
      linhas.push(this.escCsv('Nenhuma transferencia necessaria (tudo quitado)'));
    }

    linhas.push('');
    linhas.push(this.escCsv('Quem deve para quem (abatimentos)'));
    linhas.push(['Deve', 'Recebe', 'Valor'].map(this.escCsv.bind(this)).join(sep));
    if (this.transferenciasAposAbatimento?.length) {
      for (const t of this.transferenciasAposAbatimento) {
        linhas.push([t.devedor, t.credor, this.toPtMoney(t.valor)].map(this.escCsv.bind(this)).join(sep));
      }
    } else {
      linhas.push(this.escCsv('Nenhuma transferencia necessaria (tudo quitado)'));
    }

    // Normaliza qualquer célula numérica que tenha sobrado sem formatação
    const normalized = linhas.map((line) => {
      if (!line) return line;
      // divide pelas colunas do CSV (sep)
      const cols = line.split(sep);
      const mapped = cols.map((c) => {
        const raw = (c || '').trim();

        // se estiver vazio ou for texto (contendo letras) não altera
        if (!raw) return c;
        // remove aspas externas para analisar o conteúdo
        const unquoted = raw.startsWith('"') && raw.endsWith('"') ? raw.slice(1, -1).replace(/""/g, '"') : raw;

        // corresponde numeros com ponto decimal (ex: 1322 or 17.5)
        if (/^-?\d+(?:\.\d+)?$/.test(unquoted)) {
          const n = Number(unquoted);
          return this.escCsv(this.toPtMoney(n));
        }

        // corresponde numeros com vírgula decimal (ex: 17,5) - normaliza para duas casas
        if (/^-?\d+(?:,\d+)?$/.test(unquoted)) {
          const n = this.parseNumberPtBr(unquoted);
          if (n !== null) return this.escCsv(this.toPtMoney(n));
        }

        return c;
      });
      return mapped.join(sep);
    });

    return normalized.join('\n');
  }


  public toPtMoney(n: number): string {
    return (n ?? 0).toFixed(2).replace('.', ',');
  }

  private toPtDate(d: Date | string): string {
    const dt = d instanceof Date ? d : new Date(d);
    if (!(dt instanceof Date) || isNaN(dt.getTime())) return '';
    const dd = String(dt.getDate()).padStart(2, '0');
    const mm = String(dt.getMonth() + 1).padStart(2, '0');
    const yyyy = dt.getFullYear();
    return `${dd}/${mm}/${yyyy}`;
  }

  private escCsv(v: any): string {
    const s = String(v ?? '');
    if (/[;"\n\r]/.test(s)) return `"${s.replace(/"/g, '""')}"`;
    return s;
  }

  // =========================
  // CSV -> Saldos -> Transferências (mantido)
  // =========================

  private parseCsvToSaldos(csv: string): SaldoPessoa[] {
    if (!csv?.trim()) return [];

    // preserva linhas vazias para detectar seções
    const lines = csv.split(/\r?\n/).map((l) => l.trim());

    // procura a linha de cabeçalho da seção "Saldos" (ou similar)
    let headerIndex = -1;
    let detectedSep = ';';

    for (let i = 0; i < lines.length; i++) {
      const l = lines[i];
      if (!l) continue;
      const sep = l.includes(';') ? ';' : ',';
      const cols = l.split(sep).map((h) => this.normalizeHeader(h));
      // se a linha de cabeçalho contém nome/valor/saldo/total a receber etc, assumimos que é o header
      const hasNome = cols.includes('nome') || cols.includes('pessoa') || cols.includes('participante');
      const hasValor = cols.includes('valor') || cols.includes('saldo') || cols.includes('total') || cols.includes('totalareceber') || cols.includes('valordevido');
      if (hasNome && hasValor) {
        headerIndex = i;
        detectedSep = sep;
        break;
      }
    }

    if (headerIndex < 0) {
      // fallback para a primeira linha não vazia
      headerIndex = lines.findIndex((l) => !!l);
      if (headerIndex < 0) return [];
      detectedSep = lines[headerIndex].includes(';') ? ';' : ',';
    }

    const headerLine = lines[headerIndex];
    const sep = detectedSep;
    const headers = headerLine.split(sep).map((h) => this.normalizeHeader(h));

    const idxNome = this.findIndex(headers, ['pessoa', 'nome', 'usuario', 'participante']);
    const idxSaldo = this.findIndex(headers, ['saldo', 'valor', 'total', 'resultado']);

    const idxTotalPago = this.findIndex(headers, ['totalpago', 'totalpaid']);
    const idxValorDevido = this.findIndex(headers, ['valordevido', 'valordevendo', 'devido']);
    const idxTotalAReceber = this.findIndex(headers, ['totalareceber', 'totalareceber', 'totalareceber']);

    const nomeCol = idxNome >= 0 ? idxNome : 0;
    const saldoCol = idxSaldo >= 0 ? idxSaldo : 1;

    const out: SaldoPessoa[] = [];

    // percorre linhas logo após o header até encontrar uma linha vazia ou o próximo título de seção
    for (let i = headerIndex + 1; i < lines.length; i++) {
      const row = lines[i];
      if (!row) break; // fim da seção
      const lower = row.toLowerCase();
      if (lower.startsWith('quem deve') || lower.startsWith('quemdeve') || lower.startsWith('saldos')) break;

      const cols = row.split(sep).map((c) => c.trim());
      if (!cols.length) continue;

      const nome = (cols[nomeCol] ?? '').trim();
      if (!nome) continue;

      // se backend trouxe Total a Receber / Valor Devido, calcula saldo = totalAReceber - valorDevido
      let valorStr = (cols[saldoCol] ?? '').trim();
      if (idxTotalAReceber >= 0 || idxValorDevido >= 0) {
        const aReceberStr = idxTotalAReceber >= 0 ? (cols[idxTotalAReceber] ?? '').trim() : '';
        const devidoStr = idxValorDevido >= 0 ? (cols[idxValorDevido] ?? '').trim() : '';
        const aReceber = this.parseNumberPtBr(aReceberStr) ?? 0;
        const devido = this.parseNumberPtBr(devidoStr) ?? 0;
        const calc = aReceber - devido;
        valorStr = String(calc);
      }

      let valor = this.parseNumberPtBr(valorStr);
      // heurística: se o backend enviou valores em centavos (ex: "23215")
      // e o campo bruto não contém separador decimal, converte para reais dividindo por 100
      if (
        valor !== null &&
        Math.abs(valor) >= 1000 &&
        !/[.,]/.test(valorStr) &&
        /^\d{4,}$/.test(valorStr.replace(/[^0-9]/g, ''))
      ) {
        valor = valor / 100;
      }
      if (valor === null) continue;

      out.push({ nome, valor });
    }

    out.sort((a, b) => b.valor - a.valor);
    return out;
  }

  private calcularTransferencias(saldos: SaldoPessoa[]): Transferencia[] {
    const credores = saldos
      .filter((s) => s.valor > 0)
      .map((s) => ({ nome: s.nome, valor: s.valor }));

    const devedores = saldos
      .filter((s) => s.valor < 0)
      .map((s) => ({ nome: s.nome, valor: Math.abs(s.valor) }));

    const transferencias: Transferencia[] = [];

    let i = 0;
    let j = 0;

    while (i < devedores.length && j < credores.length) {
      const d = devedores[i];
      const c = credores[j];

      const pago = Math.min(d.valor, c.valor);

      transferencias.push({
        devedor: d.nome,
        credor: c.nome,
        valor: this.round2(pago),
      });

      d.valor -= pago;
      c.valor -= pago;

      if (d.valor <= 0.000001) i++;
      if (c.valor <= 0.000001) j++;
    }

    return transferencias;
  }

  private normalizeHeader(h: string): string {
    return (h || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/\s+/g, '')
      .trim();
  }

  private findIndex(headers: string[], candidates: string[]): number {
    for (const c of candidates) {
      const idx = headers.indexOf(c);
      if (idx >= 0) return idx;
    }
    return -1;
  }

  private parseNumberPtBr(input: string): number | null {
    if (!input) return null;

    let s = input.replace(/[^\d,.-]/g, '').trim();
    if (!s) return null;

    s = s.replace(/\./g, '');
    s = s.replace(/,/g, '.');

    const n = Number(s);
    return Number.isFinite(n) ? n : null;
  }

  private round2(n: number): number {
    return Math.round((n + Number.EPSILON) * 100) / 100;
  }

  // Calcula saldos a partir da lista de lançamentos (usa ids dos participantes e pagador)
  // Retorna { net, paid } - net para transferências (paid - due), paid para exibição sem abatimentos
  private computeSaldosFromLancamentos(lancamentos: LancamentoResponse[]): { net: SaldoPessoa[]; paid: SaldoPessoa[] } {
    const map: Record<number, { id: number; nome: string; paid: number; due: number }> = {};

    const ensure = (id: number, nome?: string) => {
      if (!map[id]) map[id] = { id, nome: nome ?? String(id), paid: 0, due: 0 };
      if (nome) map[id].nome = nome;
      return map[id];
    };

    for (const l of lancamentos) {
      const payerId = (l.pagador as any)?.id ?? -1;
      const payerNome = (l.pagador as any)?.nome ?? '';
      const total = Number(l.valor || 0) || 0;

      const participants = (l.participantes as any[]) || [];

      // Regra: apenas os participantes selecionados participam da divisão.
      // O pagador só entra na divisão se estiver explicitamente presente em participantes.
      if (!participants.length) {
        if (payerId !== -1) ensure(payerId, payerNome).paid += total;
        continue;
      }

      const participantIds = participants.map((p) => p.pessoaId);
      const payerIncludedInParticipants = participantIds.includes(payerId);

      const participantsForDivision = participants; // já representa os selecionados

      const explicit = participantsForDivision.every((p) => p.valor !== undefined && p.valor !== null && !isNaN(Number(p.valor)));

      if (explicit) {
        if (payerId !== -1) ensure(payerId, payerNome).paid += total;
        for (const p of participantsForDivision) {
          ensure(p.pessoaId, p.nome).due += Number(p.valor || 0);
        }
      } else {
        const per = total / participantsForDivision.length;
        if (payerId !== -1) ensure(payerId, payerNome).paid += total;
        for (const p of participantsForDivision) {
          ensure(p.pessoaId, p.nome).due += per;
        }
      }
    }

    const paid: SaldoPessoa[] = Object.values(map).map((v) => ({ nome: v.nome, valor: this.round2(v.paid) }));
    const net: SaldoPessoa[] = Object.values(map).map((v) => ({ nome: v.nome, valor: this.round2(v.paid - v.due) }));

    paid.sort((a, b) => b.valor - a.valor);
    net.sort((a, b) => b.valor - a.valor);
    return { net, paid };
  }

  // Agrupa dívidas por par (devedor -> credor) baseado nos lançamentos:
  // para cada lançamento, cada participante (ou cada devedor quando divide=false)
  // gera uma dívida em direção ao pagador pelo valor devido naquele lançamento.
  private computePairwiseDebtsFromLancamentos(lancamentos: LancamentoResponse[]): Transferencia[] {
    const { agg, mapNames } = this.computePairwiseAggFromLancamentos(lancamentos);
    const out: Transferencia[] = Object.keys(agg).map((k) => {
      const [fromIdStr, toIdStr] = k.split('-');
      const fromId = Number(fromIdStr);
      const toId = Number(toIdStr);
      return {
        devedor: mapNames[fromId] ?? String(fromId),
        credor: mapNames[toId] ?? String(toId),
        valor: this.round2(agg[k] || 0),
      } as Transferencia;
    });

    // ordena por devedor para exibição previsível
    out.sort((a, b) => a.devedor.localeCompare(b.devedor) || b.valor - a.valor);
    return out;
  }

  // Retorna um mapa agregando dívidas por par e o mapa de nomes usado
  private computePairwiseAggFromLancamentos(lancamentos: LancamentoResponse[]): { agg: Record<string, number>; mapNames: Record<number, string> } {
    const mapNames: Record<number, string> = {};
    const agg: Record<string, number> = {}; // key: `${devedorId}-${credorId}`

    const add = (fromId: number, toId: number, amount: number) => {
      if (!amount || amount <= 0) return;
      const key = `${fromId}-${toId}`;
      agg[key] = (agg[key] || 0) + amount;
    };

    for (const l of lancamentos) {
      const payerId = (l.pagador as any)?.id ?? null;
      const payerName = (l.pagador as any)?.nome ?? '';
      if (payerId != null) mapNames[payerId] = payerName;

      const participants = (l.participantes as any[]) || [];

      if (participants && participants.length) {
        const explicit = participants.every((p) => p.valor !== undefined && p.valor !== null && !isNaN(Number(p.valor)));
        if (explicit) {
          for (const p of participants) {
            mapNames[p.pessoaId] = p.nome ?? mapNames[p.pessoaId] ?? String(p.pessoaId);
            if (p.pessoaId !== payerId) add(p.pessoaId, payerId!, Number(p.valor || 0));
          }
        } else {
          const total = Number(l.valor || 0) || 0;
          const per = total / participants.length;
          for (const p of participants) {
            mapNames[p.pessoaId] = p.nome ?? mapNames[p.pessoaId] ?? String(p.pessoaId);
            if (p.pessoaId !== payerId) add(p.pessoaId, payerId!, per);
          }
        }
      } else {
        const devedores = (l as any).devedores || [];
        if (devedores && devedores.length) {
          for (const d of devedores) {
            mapNames[d.pessoaId] = d.nome ?? mapNames[d.pessoaId] ?? String(d.pessoaId);
            if (d.pessoaId !== payerId) add(d.pessoaId, payerId!, Number(d.valor || 0));
          }
        }
      }
    }

    return { agg, mapNames };
  }

  // Calcula abatimentos (mutualizações) a partir dos lançamentos e retorna
  // both: lista de abatimentos e lista de transferências remanescentes após abatimento
  // computeAbatimentosFromLancamentos removed — abatimentos not exposed separately

  // Calcula abatimentos a partir da lista de transferências (por nome)
  private computeRemainingAfterAbatimento(transferencias: Transferencia[]): Transferencia[] {
    const agg: Record<string, number> = {};
    const processed = new Set<string>();

    for (const t of transferencias || []) {
      const key = `${t.devedor}|${t.credor}`;
      agg[key] = (agg[key] || 0) + (t.valor || 0);
    }

    const rem: Transferencia[] = [];

    for (const key of Object.keys(agg)) {
      if (processed.has(key)) continue;
      const [a, b] = key.split('|');
      const kAB = `${a}|${b}`;
      const kBA = `${b}|${a}`;
      const vAB = agg[kAB] || 0;
      const vBA = agg[kBA] || 0;
      const abatValue = Math.min(vAB, vBA);

      const remAB = Math.max(0, vAB - abatValue);
      const remBA = Math.max(0, vBA - abatValue);
      if (remAB > 0) rem.push({ devedor: a, credor: b, valor: this.round2(remAB) });
      if (remBA > 0) rem.push({ devedor: b, credor: a, valor: this.round2(remBA) });

      processed.add(kAB);
      processed.add(kBA);
    }

    rem.sort((x, y) => x.devedor.localeCompare(y.devedor) || y.valor - x.valor);
    return rem;
  }
}
