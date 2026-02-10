import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';

@Component({
  selector: 'app-pagamentos',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule],
  templateUrl: './pagamentos.html',
  styleUrl: './pagamentos.scss',
})
export class PagamentosComponent {}
