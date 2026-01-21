import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent {
  constructor(
    private auth: AuthService,
    private router: Router
  ) {}

  sair(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
