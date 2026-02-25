import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class RelatorioService {

  constructor(private http: HttpClient) {}

  // 🔹 Para download do CSV
  baixarRelatorioSaldosPeriodoCSV(dataInicio: string, dataFim: string) {
    return this.baixarRelatorioSaldosPeriodoCSVComGrupo(dataInicio, dataFim, null);
  }

  // 🔹 Para ler o CSV como texto e montar o relatório na tela
  baixarRelatorioSaldosPeriodoCSVTexto(dataInicio: string, dataFim: string) {
    return this.baixarRelatorioSaldosPeriodoCSVTextoComGrupo(dataInicio, dataFim, null);
  }
  
  // overloads that accept optional grupoId
  baixarRelatorioSaldosPeriodoCSVComGrupo(dataInicio: string, dataFim: string, grupoId?: number | null) {
    const params = new URLSearchParams();
    params.set('dataInicio', dataInicio);
    params.set('dataFim', dataFim);
    if (grupoId !== null && grupoId !== undefined) params.set('grupoId', String(grupoId));

    return this.http.get(`/api/relatorios/saldos-periodo.csv?${params.toString()}`, { responseType: 'blob' });
  }

  baixarRelatorioSaldosPeriodoCSVTextoComGrupo(dataInicio: string, dataFim: string, grupoId?: number | null) {
    const params = new URLSearchParams();
    params.set('dataInicio', dataInicio);
    params.set('dataFim', dataFim);
    if (grupoId !== null && grupoId !== undefined) params.set('grupoId', String(grupoId));

    return this.http.get(`/api/relatorios/saldos-periodo.csv?${params.toString()}`, { responseType: 'text' });
  }
  baixarRelatorioMensalCSV(ano: number, mes: number, grupoId?: number | null) {
  const params = new URLSearchParams();
  params.set('ano', String(ano));
  params.set('mes', String(mes));

  if (grupoId !== null && grupoId !== undefined) {
    params.set('grupoId', String(grupoId));
  }

  return this.http.get(`/api/relatorios/mensal.csv?${params.toString()}`, {
    responseType: 'blob',
  });
}
}