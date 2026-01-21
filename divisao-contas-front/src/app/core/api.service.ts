import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  get<T>(path: string, params?: Record<string, any>) {
    return this.http.get<T>(this.baseUrl + path, { params: this.toParams(params) });
  }

  post<T>(path: string, body: any) {
    return this.http.post<T>(this.baseUrl + path, body);
  }

  put<T>(path: string, body: any) {
    return this.http.put<T>(this.baseUrl + path, body);
  }

  delete<T>(path: string) {
    return this.http.delete<T>(this.baseUrl + path);
  }

  private toParams(params?: Record<string, any>): HttpParams | undefined {
    if (!params) return undefined;
    let p = new HttpParams();
    for (const [k, v] of Object.entries(params)) {
      if (v === null || v === undefined) continue;
      p = p.set(k, String(v));
    }
    return p;
  }
}
