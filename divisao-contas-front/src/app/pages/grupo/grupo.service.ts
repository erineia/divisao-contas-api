import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment';

export interface GrupoCreateRequest {
  nome: string;
}

export interface GrupoResponse {
  id: number;
  nome: string;
}

@Injectable({
  providedIn: 'root',
})
export class GrupoService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  criar(req: GrupoCreateRequest): Observable<GrupoResponse> {
    return this.http.post<GrupoResponse>(`${this.apiUrl}/api/grupos`, req);
  }

  listar(): Observable<GrupoResponse[]> {
    return this.http.get<GrupoResponse[]>(`${this.apiUrl}/api/grupos`);
  }

  buscarPorId(id: number): Observable<GrupoResponse> {
    return this.http.get<GrupoResponse>(`${this.apiUrl}/api/grupos/${id}`);
  }

  atualizar(id: number, req: GrupoCreateRequest): Observable<GrupoResponse> {
    return this.http.put<GrupoResponse>(`${this.apiUrl}/api/grupos/${id}`, req);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/grupos/${id}`);
  }
}
