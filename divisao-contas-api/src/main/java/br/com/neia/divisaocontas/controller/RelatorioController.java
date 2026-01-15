package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.SaldoPessoaResponse;
import br.com.neia.divisaocontas.dto.TransferenciaResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

  private final SaldoController saldoController;

  public RelatorioController(SaldoController saldoController) {
    this.saldoController = saldoController;
  }

  @GetMapping(value = "/saldos.csv", produces = "text/csv")
  public ResponseEntity<byte[]> saldosCsv(@RequestParam int ano, @RequestParam int mes) {

    // reaproveita sua lógica já pronta
    List<SaldoPessoaResponse> saldos = saldoController.saldoDoMes(ano, mes);
    List<TransferenciaResponse> quemDeve = saldoController.quemDeve(ano, mes);

    StringBuilder csv = new StringBuilder();

    // Cabeçalho do relatório
    csv.append("Relatorio de Saldos;").append(ano).append("-").append(String.format("%02d", mes)).append("\n");
    csv.append("\n");

    // Tabela de saldos
    csv.append("Pessoa;Total Pago;Valor Devido;Total a Receber\n");
    for (SaldoPessoaResponse s : saldos) {
      csv.append(escape(s.getNome())).append(";")
          .append(toPtBr(s.getTotalPago())).append(";")
          .append(toPtBr(s.getValorDevido())).append(";")
          .append(toPtBr(s.getTotalAReceber()))
          .append("\n");
    }

    // Separador
    csv.append("\n");
    csv.append("Quem deve pra quem\n");
    csv.append("Devedor;Credor;Valor\n");

    for (TransferenciaResponse t : quemDeve) {
      csv.append(escape(t.getDevedor())).append(";")
          .append(escape(t.getCredor())).append(";")
          .append(toPtBr(t.getValor()))
          .append("\n");
    }

    String filename = String.format("saldos-%d-%02d.csv", ano, mes);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  @GetMapping(value = "/saldos-periodo.csv", produces = "text/csv")
  public ResponseEntity<byte[]> saldosPeriodoCsv(@RequestParam String dataInicio,
      @RequestParam String dataFim) {

    // valida formato das datas
    LocalDate.parse(dataInicio);
    LocalDate.parse(dataFim);

    List<SaldoPessoaResponse> saldos = saldoController.saldoPorPeriodo(dataInicio, dataFim);
    List<TransferenciaResponse> quemDeve = saldoController.quemDevePeriodo(dataInicio, dataFim);

    StringBuilder csv = new StringBuilder();

    csv.append("Relatorio de Saldos;").append(dataInicio).append(" a ").append(dataFim).append("\n");
    csv.append("\n");

    csv.append("Pessoa;Total Pago;Valor Devido;Total a Receber\n");
    for (SaldoPessoaResponse s : saldos) {
      csv.append(escape(s.getNome())).append(";")
          .append(toPtBr(s.getTotalPago())).append(";")
          .append(toPtBr(s.getValorDevido())).append(";")
          .append(toPtBr(s.getTotalAReceber()))
          .append("\n");
    }

    csv.append("\n");
    csv.append("Quem deve pra quem\n");
    csv.append("Devedor;Credor;Valor\n");

    for (TransferenciaResponse t : quemDeve) {
      csv.append(escape(t.getDevedor())).append(";")
          .append(escape(t.getCredor())).append(";")
          .append(toPtBr(t.getValor()))
          .append("\n");
    }

    String filename = String.format("saldos-%s-a-%s.csv", dataInicio, dataFim);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  // CSV amigável: separador ; e números PT-BR (vírgula)
  private String toPtBr(java.math.BigDecimal v) {
    if (v == null)
      return "0,00";
    return v.setScale(2, java.math.RoundingMode.HALF_UP)
        .toString()
        .replace('.', ',');
  }

  private String escape(String s) {
    if (s == null)
      return "";
    // se tiver ; ou aspas, coloca entre aspas e escapa aspas
    if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
      return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    return s;
  }
}
