import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth.guard';
import { ShellComponent } from './layout/shell/shell.component';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },

  {
    path: '',
    component: ShellComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', component: HomeComponent },
      // próximas telas:
      // { path: 'lancamentos', component: LancamentosComponent },
      // { path: 'pagamentos', component: PagamentosComponent },
      // { path: 'saldos', component: SaldosComponent },
    ]
  },

  { path: '**', redirectTo: '' }
];
