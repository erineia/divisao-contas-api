import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    try {
      const token = localStorage.getItem('token');

      // Não anexa token em chamadas de login/registro
      if (req.url.includes('/auth/login') || req.url.includes('/auth/register')) {
        console.debug('AuthInterceptor: skip auth for', req.url);
        return next.handle(req);
      }

      if (!token) {
        console.debug('AuthInterceptor: no token found for', req.url);
        return next.handle(req);
      }

      // Evita duplicar header caso já exista.
      if (req.headers.has('Authorization')) {
        console.debug('AuthInterceptor: Authorization header already present for', req.url);
        return next.handle(req);
      }

      // Apenas loga a URL, não o token em si
      console.debug('AuthInterceptor: attaching Authorization header to', req.url);

      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
      });

      return next.handle(authReq);
    } catch (e) {
      console.warn('AuthInterceptor error', e);
      return next.handle(req);
    }
  }
}
