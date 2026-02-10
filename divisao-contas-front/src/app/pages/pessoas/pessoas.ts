import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';

@Component({
  selector: 'app-pessoas',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule],
  templateUrl: './pessoas.html',
  styleUrl: './pessoas.scss',
})
export class PessoasComponent {}
