package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.dto.FechamentoMesResponse;
import br.com.neia.divisaocontas.entity.FechamentoMes;
import br.com.neia.divisaocontas.exception.DuplicateException;
import br.com.neia.divisaocontas.exception.NotFoundException;
import br.com.neia.divisaocontas.repository.FechamentoMesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class FechamentoMesService {

  private final FechamentoMesRepository fechamentoRepo;
  private final CategoriaService categoriaService;

  private static final DateTimeFormatter DATA_HORA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  public FechamentoMesService(FechamentoMesRepository fechamentoRepo, CategoriaService categoriaService) {
    this.fechamentoRepo = fechamentoRepo;
    this.categoriaService = categoriaService;
  }

  @Transactional
  public FechamentoMesResponse fechar(int ano, int mes, Long categoriaId, String observacao) {
    validarMesAno(ano, mes);

    var categoria = (categoriaId == null)
        ? categoriaService.getOrCreateMes(mes)
        : categoriaService.buscarPorId(categoriaId);

    if (fechamentoRepo.existsByAnoAndMesAndCategoriaId(ano, mes, categoria.getId())) {
      throw new DuplicateException("Este mês já está fechado.");
    }

    FechamentoMes salvo = fechamentoRepo.save(new FechamentoMes(ano, mes, categoria, observacao));
    return toResponse(salvo);
  }

  @Transactional(readOnly = true)
  public List<FechamentoMesResponse> listar(Long categoriaId) {
    return fechamentoRepo.findAll().stream()
        .filter(f -> categoriaId == null || (f.getCategoria() != null && categoriaId.equals(f.getCategoria().getId())))
        .sorted(Comparator.comparing((FechamentoMes f) -> f.getCategoria() == null ? "" : f.getCategoria().getNome())
            .thenComparing(FechamentoMes::getAno)
            .thenComparing(FechamentoMes::getMes))
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public void reabrir(int ano, int mes, Long categoriaId) {
    validarMesAno(ano, mes);

    var categoria = (categoriaId == null)
        ? categoriaService.getOrCreateMes(mes)
        : categoriaService.buscarPorId(categoriaId);

    fechamentoRepo.findByAnoAndMesAndCategoriaId(ano, mes, categoria.getId())
        .orElseThrow(() -> new NotFoundException("Fechamento não encontrado."));

    fechamentoRepo.deleteByAnoAndMesAndCategoriaId(ano, mes, categoria.getId());
  }

  public void validarAberto(LocalDate data) {
    validarAberto(data, null);
  }

  public void validarAberto(LocalDate data, Long categoriaId) {
    if (data == null)
      return;

    int ano = data.getYear();
    int mes = data.getMonthValue();

    Long catId = categoriaId;
    if (catId == null) {
      catId = categoriaService.getOrCreateMes(mes).getId();
    }

    if (fechamentoRepo.existsByAnoAndMesAndCategoriaId(ano, mes, catId)) {
      throw new IllegalArgumentException("Este mês está fechado. Reabra para alterar.");
    }
  }

  private void validarMesAno(int ano, int mes) {
    if (mes < 1 || mes > 12)
      throw new IllegalArgumentException("Mês deve ser entre 1 e 12.");
    if (ano < 2000 || ano > 2100)
      throw new IllegalArgumentException("Ano inválido.");
  }

  private FechamentoMesResponse toResponse(FechamentoMes f) {
    String dataFechamento = f.getDataFechamento() == null ? null : f.getDataFechamento().format(DATA_HORA_BR);
    Long categoriaId = f.getCategoria() == null ? null : f.getCategoria().getId();
    String categoriaNome = f.getCategoria() == null ? null : f.getCategoria().getNome();
    return new FechamentoMesResponse(f.getId(), f.getAno(), f.getMes(), categoriaId, categoriaNome, dataFechamento,
        f.getObservacao());
  }
}
