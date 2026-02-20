import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { HomeComponent } from './home/home.component';
import { FechamentosComponent } from './pages/fechamentos/fechamentos';
import { LancamentosComponent } from './pages/lancamentos/lancamentos';
import { PessoasComponent } from './pages/pessoas/pessoas';
import { PagamentosComponent } from './pages/pagamentos/pagamentos';
import { CategoriaComponent } from './pages/categoria/categoria';
import { RelatoriosComponent } from './pages/relatorios/relatorios';
import { PessoasPesquisaComponent } from './pages/pessoas/pessoas-pesquisa';

const routes: Routes = [
  { path: 'login', component: LoginComponent },

  // rota inicial pós-login, com Home como layout contendo o menu lateral
  {
    path: 'home',
    component: HomeComponent,
    children: [
      { path: '', redirectTo: 'pessoas/cadastrar', pathMatch: 'full' },

      // telas do sistema que devem manter o menu lateral
      { path: 'pessoas/cadastrar', component: PessoasComponent },
      { path: 'pessoas/editar/:id', component: PessoasComponent },
      { path: 'pessoas/pesquisar', component: PessoasPesquisaComponent },

      // demais telas podem ser encaixadas aqui depois, quando tiverem layout definido
      { path: 'fechamentos', component: FechamentosComponent },
      { path: 'lancamentos', component: LancamentosComponent },
      { path: 'pagamentos', component: PagamentosComponent },
      { path: 'categorias', component: CategoriaComponent },
      { path: 'relatorios', component: RelatoriosComponent },
    ],
  },

  // redireciona acessos diretos para manter o layout de Home
  { path: 'pessoas/cadastrar', redirectTo: 'home/pessoas/cadastrar', pathMatch: 'full' },
  { path: 'pessoas/pesquisar', redirectTo: 'home/pessoas/pesquisar', pathMatch: 'full' },

  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
