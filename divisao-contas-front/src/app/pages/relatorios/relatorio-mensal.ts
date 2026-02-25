import { CommonModule } from '@angular/common';

import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MaterialModule } from '../../material-module';
import { MatTableModule } from '@angular/material/table';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { GrupoService, GrupoResponse } from '../grupo/grupo.service';
import { RelatorioService } from './relatorio.service';

@Component({
  selector: 'app-relatorio-mensal',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, ReactiveFormsModule, MatTableModule],
  templateUrl: './relatorio-mensal.html',
  styleUrl: './relatorio-mensal.scss',
})
export class RelatorioMensalComponent implements OnInit {
  filtrosForm: FormGroup;
  grupos: GrupoResponse[] = [];

  constructor(
    private fb: FormBuilder,
    private grupoService: GrupoService,
    private relatorioService: RelatorioService
  ) {
    this.filtrosForm = this.fb.group({
      dataInicio: [null],
      dataFim: [null],
      grupoId: [null],
    });
  }

  ngOnInit(): void {
    this.grupoService.listar().subscribe(grupos => (this.grupos = grupos));
  }

  exportarCSV(): void {
    const dataInicio: Date = this.filtrosForm.value.dataInicio;
    const ano = dataInicio ? new Date(dataInicio).getFullYear() : new Date().getFullYear();
    const mes = dataInicio ? new Date(dataInicio).getMonth() + 1 : new Date().getMonth() + 1;
    const grupoId = this.filtrosForm.value.grupoId;
    this.relatorioService.baixarRelatorioMensalCSV(ano, mes, grupoId).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `relatorio-mensal-${ano}-${mes}.csv`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }
}
