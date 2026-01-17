package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.repository.FechamentoMesRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FechamentoMesService {

  private final FechamentoMesRepository fechamentoRepo;

  public FechamentoMesService(FechamentoMesRepository fechamentoRepo) {
    this.fechamentoRepo = fechamentoRepo;
  }

  public void validarAberto(LocalDate data) {
    if (data == null)
      return;
    int ano = data.getYear();
    int mes = data.getMonthValue();

    if (fechamentoRepo.existsByAnoAndMes(ano, mes)) {
      throw new IllegalArgumentException("Este mês está fechado. Reabra para alterar.");
    }
  }
}
