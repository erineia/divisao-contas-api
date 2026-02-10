import { CommonModule } from '@angular/common';
import { Component, OnDestroy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { MaterialModule } from '../material-module';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnDestroy {
  isSmallScreen = false;

  private destroy$ = new Subject<void>();

  constructor(
    private breakpointObserver: BreakpointObserver,
    private authService: AuthService,
    private router: Router,
  ) {
    this.breakpointObserver
      .observe([Breakpoints.Handset])
      .pipe(takeUntil(this.destroy$))
      .subscribe(result => (this.isSmallScreen = result.matches));
  }

  closeIfSmall(sidenav: any) {
    if (this.isSmallScreen) sidenav.close();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  logout(): void {
    // limpa dados de autenticação
    localStorage.removeItem('token');

    // se depois você tiver um método logout na API, pode chamar aqui
    // this.authService.logout().subscribe(() => { ... });

    // navega para a tela de login
    this.router.navigate(['/login']);
  }
}
