import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment';

export interface LancamentoCreateRequest {
  data: string; // yyyy-MM-dd
  valor: number;
  descricao: string;
  grupoId?: number | null;
  observacao?: string | null;
}


import { PessoaResponse } from '../pessoas/pessoa.service';

export interface LancamentoPessoaValorResponse {
  pessoaId: number;
  nome: string;
  valor: number;
}

export interface LancamentoResponse {
  id: number;
  data: string; // dd/MM/yyyy
  valor: number;
  descricao: string;
  grupoId?: number | null;
  grupoNome?: string | null;
  observacao?: string | null;
  pagador?: PessoaResponse | null;
  divide?: boolean;
  participantes?: LancamentoPessoaValorResponse[];
  devedores?: LancamentoPessoaValorResponse[];
}

@Injectable({
  providedIn: 'root',
})
export class LancamentoService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  criar(req: LancamentoCreateRequest): Observable<LancamentoResponse> {
    return this.http.post<LancamentoResponse>(`${this.apiUrl}/api/lancamentos`, req);
  }

  listar(): Observable<LancamentoResponse[]> {
    return this.http.get<LancamentoResponse[]>(`${this.apiUrl}/api/lancamentos`);
  }

  atualizar(id: number, req: LancamentoCreateRequest): Observable<LancamentoResponse> {
    return this.http.put<LancamentoResponse>(`${this.apiUrl}/api/lancamentos/${id}`, req);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/lancamentos/${id}`);
  }
}
