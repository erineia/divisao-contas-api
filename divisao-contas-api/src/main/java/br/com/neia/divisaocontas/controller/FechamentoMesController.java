package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.entity.FechamentoMes;
import br.com.neia.divisaocontas.exception.DuplicateException;
import br.com.neia.divisaocontas.exception.NotFoundException;
import br.com.neia.divisaocontas.repository.FechamentoMesRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fechamentos")
public class FechamentoMesController {

  private final FechamentoMesRepository fechamentoRepo;

  public FechamentoMesController(FechamentoMesRepository fechamentoRepo) {
    this.fechamentoRepo = fechamentoRepo;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FechamentoMes fechar(@RequestParam int ano,
      @RequestParam int mes,
      @RequestParam(required = false) String observacao) {

    validarMesAno(ano, mes);

    if (fechamentoRepo.existsByAnoAndMes(ano, mes)) {
      throw new DuplicateException("Este mês já está fechado.");
    }

    return fechamentoRepo.save(new FechamentoMes(ano, mes, observacao));
  }

  @GetMapping
  public List<FechamentoMes> listar() {
    return fechamentoRepo.findAll().stream()
        .sorted(Comparator.comparing(FechamentoMes::getAno).thenComparing(FechamentoMes::getMes))
        .toList();
  }

  // Opcional (bom para DEV): reabrir mês
  @DeleteMapping
  @Transactional
  public ResponseEntity<Map<String, String>> reabrir(@RequestParam int ano, @RequestParam int mes) {
    validarMesAno(ano, mes);

    fechamentoRepo.findByAnoAndMes(ano, mes)
        .orElseThrow(() -> new NotFoundException("Fechamento não encontrado."));

    fechamentoRepo.deleteByAnoAndMes(ano, mes);

    return ResponseEntity.ok(Map.of("mensagem", "Mês reaberto com sucesso!"));
  }

  private void validarMesAno(int ano, int mes) {
    if (mes < 1 || mes > 12)
      throw new IllegalArgumentException("Mês deve ser entre 1 e 12.");
    if (ano < 2000 || ano > 2100)
      throw new IllegalArgumentException("Ano inválido.");
  }
}
