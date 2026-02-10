import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { HomeComponent } from './home/home.component';
import { FechamentosComponent } from './pages/fechamentos/fechamentos';
import { LancamentosComponent } from './pages/lancamentos/lancamentos';
import { PessoasComponent } from './pages/pessoas/pessoas';
import { PagamentosComponent } from './pages/pagamentos/pagamentos';
import { RelatoriosComponent } from './pages/relatorios/relatorios';

const routes: Routes = [
  { path: 'login', component: LoginComponent },

  // rota inicial pós-login
  { path: 'home', component: HomeComponent },

  // telas do sistema
  { path: 'fechamentos', component: FechamentosComponent },
  { path: 'lancamentos', component: LancamentosComponent },
  { path: 'pessoas', component: PessoasComponent },
  { path: 'pagamentos', component: PagamentosComponent },
  { path: 'relatorios', component: RelatoriosComponent },

  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
