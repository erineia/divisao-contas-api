import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';

@Component({
  selector: 'app-fechamentos',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule],
  templateUrl: './fechamentos.html',
  styleUrl: './fechamentos.scss',
})
export class FechamentosComponent {}
