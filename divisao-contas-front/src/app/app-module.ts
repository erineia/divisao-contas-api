import { NgModule, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';

import { MAT_DATE_LOCALE } from '@angular/material/core';

import { AppRoutingModule } from './app-routing-module';
import { App } from './app';
import { MaterialModule } from './material-module';

import { LoginComponent } from './auth/login/login.component';
import { HomeComponent } from './home/home.component';
import { PessoasComponent } from './pages/pessoas/pessoas';
import { PessoasPesquisaComponent } from './pages/pessoas/pessoas-pesquisa';
import { FechamentosComponent } from './pages/fechamentos/fechamentos';
import { PagamentosComponent } from './pages/pagamentos/pagamentos';
import { GrupoComponent } from './pages/grupo/grupo';
import { RelatoriosComponent } from './pages/relatorios/relatorios';

import { AuthInterceptor } from './auth/auth.interceptor';

registerLocaleData(localePt);

@NgModule({
  declarations: [App],
  imports: [
    BrowserModule,
    ReactiveFormsModule,
    BrowserAnimationsModule,
    HttpClientModule,
    AppRoutingModule,
    MaterialModule,

    // standalone nas rotas
    LoginComponent,
    HomeComponent,
    PessoasComponent,
    PessoasPesquisaComponent,
    FechamentosComponent,
    PagamentosComponent,
    GrupoComponent,
    RelatoriosComponent,
  ],
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },

    // ✅ deixa o datepicker em português
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    { provide: MAT_DATE_LOCALE, useValue: 'pt-BR' },
  ],
  bootstrap: [App],
})
export class AppModule {}