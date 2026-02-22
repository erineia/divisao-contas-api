import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment';

export interface PagamentoCreateRequest {
  data: string; // yyyy-MM-dd
  valor: number;
  pagadorId: number;
  recebedorId: number;
  grupoId?: number | null;
  observacao?: string | null;
}

export interface PagamentoResponse {
  id: number;
  data: string; // dd/MM/yyyy
  valor: number;
  pagador: string;
  recebedor: string;
  observacao?: string | null;
  grupoId?: number | null;
  grupoNome?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class PagamentoService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  criar(req: PagamentoCreateRequest): Observable<PagamentoResponse> {
    return this.http.post<PagamentoResponse>(`${this.apiUrl}/api/pagamentos`, req);
  }

  listar(): Observable<PagamentoResponse[]> {
    return this.http.get<PagamentoResponse[]>(`${this.apiUrl}/api/pagamentos`);
  }

  listarPorPeriodo(dataInicio: string, dataFim: string): Observable<PagamentoResponse[]> {
    const params = new HttpParams().set('dataInicio', dataInicio).set('dataFim', dataFim);
    return this.http.get<PagamentoResponse[]>(`${this.apiUrl}/api/pagamentos/periodo`, { params });
  }

  atualizar(id: number, req: PagamentoCreateRequest): Observable<PagamentoResponse> {
    return this.http.put<PagamentoResponse>(`${this.apiUrl}/api/pagamentos/${id}`, req);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/pagamentos/${id}`);
  }
}
