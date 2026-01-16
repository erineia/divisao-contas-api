package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.entity.Lancamento;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import java.util.Comparator;
import br.com.neia.divisaocontas.dto.SaldoPessoaResponse;
import br.com.neia.divisaocontas.dto.TransferenciaResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

  private final SaldoController saldoController;
  private final LancamentoRepository lancamentoRepository;

  public RelatorioController(SaldoController saldoController, LancamentoRepository lancamentoRepository) {
    this.saldoController = saldoController;
    this.lancamentoRepository = lancamentoRepository;
  }

  @GetMapping(value = "/saldos.csv", produces = "text/csv")
  public ResponseEntity<byte[]> saldosCsv(@RequestParam int ano, @RequestParam int mes) {

    // Formato do título: 01/2026
    String mesAno = String.format("%02d/%d", mes, ano);

    // Período do mês
    LocalDate inicio = LocalDate.of(ano, mes, 1);
    LocalDate fim = inicio.plusMonths(1).minusDays(1);

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Lançamentos do mês
    List<Lancamento> lancamentosMes = lancamentoRepository.findAll()
        .stream()
        .filter(l -> !l.getData().isBefore(inicio) && !l.getData().isAfter(fim))
        .sorted(Comparator.comparing(Lancamento::getData)
            .thenComparing(Lancamento::getDescricao, String.CASE_INSENSITIVE_ORDER))
        .toList();

    // Quem deve pra quem
    List<TransferenciaResponse> quemDeve = saldoController.quemDeve(ano, mes);

    StringBuilder csv = new StringBuilder();

    // Cabeçalho
    csv.append("Relatorio;").append(mesAno).append("\n\n");

    // 1) Seção: Lançamentos
    csv.append("Lancamentos do mes\n");
    csv.append("Descricao;Data;Valor;Pagador\n");

    for (Lancamento l : lancamentosMes) {
      csv.append(escape(l.getDescricao())).append(";")
          .append(l.getData().format(fmt)).append(";")
          .append(toPtBr(l.getValor())).append(";")
          .append(escape(l.getPagador().getNome()))
          .append("\n");
    }

    csv.append("\n");

    // 2) Seção: Quem deve pra quem
    csv.append("Quem deve pra quem\n");
    csv.append("Devedor;Credor;Valor\n");

    for (TransferenciaResponse t : quemDeve) {
      csv.append(escape(t.getDevedor())).append(";")
          .append(escape(t.getCredor())).append(";")
          .append(toPtBr(t.getValor()))
          .append("\n");
    }

    String filename = String.format("saldos-%02d-%d.csv", mes, ano);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  @GetMapping(value = "/saldos-periodo.csv", produces = "text/csv")
  public ResponseEntity<byte[]> saldosPeriodoCsv(@RequestParam String dataInicio,
      @RequestParam String dataFim) {

    // aceita datas no formato dd/MM/yyyy
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    LocalDate inicio = LocalDate.parse(dataInicio, formatter);
    LocalDate fim = LocalDate.parse(dataFim, formatter);

    List<SaldoPessoaResponse> saldos = saldoController.saldoPorPeriodo(dataInicio, dataFim)
        .stream()
        .filter(s -> s.getTotalPago().compareTo(java.math.BigDecimal.ZERO) != 0
            || s.getValorDevido().compareTo(java.math.BigDecimal.ZERO) != 0
            || s.getTotalAReceber().compareTo(java.math.BigDecimal.ZERO) != 0)
        .toList();
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

  private LocalDate parseData(String s) {
    if (s == null || s.isBlank()) {
      throw new IllegalArgumentException("Data é obrigatória.");
    }

    // 1) ISO: 2026-01-15
    try {
      return LocalDate.parse(s);
    } catch (DateTimeParseException ignored) {
    }

    // 2) BR: 15/01/2026
    try {
      DateTimeFormatter br = DateTimeFormatter.ofPattern("dd/MM/yyyy");
      return LocalDate.parse(s, br);
    } catch (DateTimeParseException ignored) {
    }

    throw new IllegalArgumentException("Formato de data inválido. Use yyyy-MM-dd ou dd/MM/yyyy.");
  }
}
