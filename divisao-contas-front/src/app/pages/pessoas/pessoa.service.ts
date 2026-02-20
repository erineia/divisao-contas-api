import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment';

export interface PessoaCreateRequest {
  nome: string;
}

export interface PessoaResponse {
  id: number;
  nome: string;
}

@Injectable({
  providedIn: 'root',
})
export class PessoaService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  criar(req: PessoaCreateRequest): Observable<PessoaResponse> {
    return this.http.post<PessoaResponse>(`${this.apiUrl}/api/pessoas`, req);
  }

  listar(): Observable<PessoaResponse[]> {
    return this.http.get<PessoaResponse[]>(`${this.apiUrl}/api/pessoas`);
  }

  buscarPorId(id: number): Observable<PessoaResponse> {
    return this.http.get<PessoaResponse>(`${this.apiUrl}/api/pessoas/${id}`);
  }

  atualizar(id: number, req: PessoaCreateRequest): Observable<PessoaResponse> {
    return this.http.put<PessoaResponse>(`${this.apiUrl}/api/pessoas/${id}`, req);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/pessoas/${id}`);
  }
}
