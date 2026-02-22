package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.dto.FechamentoMesResponse;
import br.com.neia.divisaocontas.entity.FechamentoMes;
import br.com.neia.divisaocontas.entity.Grupo;
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
  private final GrupoService grupoService;

  private static final DateTimeFormatter DATA_HORA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  public FechamentoMesService(FechamentoMesRepository fechamentoRepo, GrupoService grupoService) {
    this.fechamentoRepo = fechamentoRepo;
    this.grupoService = grupoService;
  }

  @Transactional
  public FechamentoMesResponse fechar(int ano, int mes, Long grupoId, String observacao) {
    validarMesAno(ano, mes);

    Grupo grupo = (grupoId == null)
        ? grupoService.getOrCreateMes(mes)
        : grupoService.buscarPorId(grupoId);

    if (fechamentoRepo.existsByAnoAndMesAndGrupoId(ano, mes, grupo.getId())) {
      throw new DuplicateException("Este mês já está fechado.");
    }

    FechamentoMes salvo = fechamentoRepo.save(new FechamentoMes(ano, mes, grupo, observacao));
    return toResponse(salvo);
  }

  @Transactional(readOnly = true)
  public List<FechamentoMesResponse> listar(Long grupoId) {
    return fechamentoRepo.findAll().stream()
        .filter(f -> grupoId == null || (f.getGrupo() != null && grupoId.equals(f.getGrupo().getId())))
        .sorted(Comparator.comparing((FechamentoMes f) -> f.getGrupo() == null ? "" : f.getGrupo().getNome())
            .thenComparing(FechamentoMes::getAno)
            .thenComparing(FechamentoMes::getMes))
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public void reabrir(int ano, int mes, Long grupoId) {
    validarMesAno(ano, mes);

    Grupo grupo = (grupoId == null)
        ? grupoService.getOrCreateMes(mes)
        : grupoService.buscarPorId(grupoId);

    fechamentoRepo.findByAnoAndMesAndGrupoId(ano, mes, grupo.getId())
        .orElseThrow(() -> new NotFoundException("Fechamento não encontrado."));

    fechamentoRepo.deleteByAnoAndMesAndGrupoId(ano, mes, grupo.getId());
  }

  public void validarAberto(LocalDate data) {
    validarAberto(data, null);
  }

  public void validarAberto(LocalDate data, Long grupoId) {
    if (data == null)
      return;

    int ano = data.getYear();
    int mes = data.getMonthValue();

    Long grupoIdEfetivo = grupoId;
    if (grupoIdEfetivo == null) {
      grupoIdEfetivo = grupoService.getOrCreateMes(mes).getId();
    }

    if (fechamentoRepo.existsByAnoAndMesAndGrupoId(ano, mes, grupoIdEfetivo)) {
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
    Long grupoId = f.getGrupo() == null ? null : f.getGrupo().getId();
    String grupoNome = f.getGrupo() == null ? null : f.getGrupo().getNome();
    return new FechamentoMesResponse(f.getId(), f.getAno(), f.getMes(), grupoId, grupoNome, dataFechamento,
        f.getObservacao());
  }
}
