import { NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing-module';
import { App } from './app';
import { MaterialModule } from './material-module';
import { LoginComponent } from './auth/login/login.component';
import { HomeComponent } from './home/home.component';
import { PessoasComponent } from './pages/pessoas/pessoas';
import { PessoasPesquisaComponent } from './pages/pessoas/pessoas-pesquisa';
import { FechamentosComponent } from './pages/fechamentos/fechamentos';
import { LancamentosComponent } from './pages/lancamentos/lancamentos';
import { PagamentosComponent } from './pages/pagamentos/pagamentos';
import { CategoriaComponent } from './pages/categoria/categoria';
import { RelatoriosComponent } from './pages/relatorios/relatorios';
import { AuthInterceptor } from './auth/auth.interceptor';

@NgModule({
  declarations: [
    App,
  ],
  imports: [
    BrowserModule,
    ReactiveFormsModule,
    BrowserAnimationsModule,
    HttpClientModule,
    AppRoutingModule,
    MaterialModule,
    // componentes standalone usados nas rotas
    LoginComponent,
    HomeComponent,
    PessoasComponent,
    PessoasPesquisaComponent,
    FechamentosComponent,
    LancamentosComponent,
    PagamentosComponent,
    CategoriaComponent,
    RelatoriosComponent,
  ],
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
  ],
  bootstrap: [App]
})
export class AppModule { }
