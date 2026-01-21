package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.service.RelatorioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatórios", description = "Exportação de relatórios CSV.")
public class RelatorioController {

  private final RelatorioService relatorioService;

  public RelatorioController(RelatorioService relatorioService) {
    this.relatorioService = relatorioService;
  }

  @GetMapping(value = "/mensal.csv", produces = "text/csv")
  public ResponseEntity<byte[]> relatorioMensalCsv(@RequestParam int ano, @RequestParam int mes,
      @RequestParam(required = false) Long categoriaId) {
    RelatorioService.CsvResult result = relatorioService.relatorioMensalCsv(ano, mes, categoriaId);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(result.content());
  }

  @GetMapping(value = "/saldos-periodo.csv", produces = "text/csv")
  public ResponseEntity<byte[]> saldosPeriodoCsv(@RequestParam String dataInicio,
      @RequestParam String dataFim) {
    RelatorioService.CsvResult result = relatorioService.saldosPeriodoCsv(dataInicio, dataFim);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(result.content());
  }
}
