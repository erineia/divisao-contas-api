package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.repository.LancamentoRateioRepository;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
import br.com.neia.divisaocontas.entity.Lancamento;
import br.com.neia.divisaocontas.entity.LancamentoRateio;
import br.com.neia.divisaocontas.entity.Pagamento;
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
  private final LancamentoRateioRepository rateioRepository;
  private final PagamentoRepository pagamentoRepository;

  public RelatorioController(SaldoController saldoController,
      LancamentoRepository lancamentoRepository,
      LancamentoRateioRepository rateioRepository,
      PagamentoRepository pagamentoRepository) {
    this.saldoController = saldoController;
    this.lancamentoRepository = lancamentoRepository;
    this.rateioRepository = rateioRepository;
    this.pagamentoRepository = pagamentoRepository;
  }

  @GetMapping(value = "/mensal.csv", produces = "text/csv")
  public ResponseEntity<byte[]> relatorioMensalCsv(@RequestParam int ano, @RequestParam int mes) {

    String mesAno = String.format("%02d/%d", mes, ano);

    LocalDate inicio = LocalDate.of(ano, mes, 1);
    LocalDate fim = inicio.plusMonths(1).minusDays(1);

    DateTimeFormatter dataFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Lançamentos do mês
    List<Lancamento> lancamentosMes = lancamentoRepository.findAll()
        .stream()
        .filter(l -> !l.getData().isBefore(inicio) && !l.getData().isAfter(fim))
        .sorted(Comparator.comparing(Lancamento::getData))
        .toList();

    // Pagamentos do mês
    List<Pagamento> pagamentosMes = pagamentoRepository.findAll()
        .stream()
        .filter(p -> !p.getData().isBefore(inicio) && !p.getData().isAfter(fim))
        .sorted(Comparator.comparing(Pagamento::getData))
        .toList();

    // Quem deve (mês): use período ISO
    String iniIso = inicio.toString();
    String fimIso = fim.toString();
    List<TransferenciaResponse> quemDeveMes = saldoController.quemDevePeriodo(iniIso, fimIso);

    // Quem deve (acumulado até o mês)
    List<TransferenciaResponse> quemDeveAcumulado = saldoController.acumuladoAteMes(ano, mes);

    // Totais
    java.math.BigDecimal totalLancadoMes = lancamentosMes.stream()
        .map(Lancamento::getValor)
        .filter(v -> v != null)
        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

    java.math.BigDecimal totalPagoMes = pagamentosMes.stream()
        .map(Pagamento::getValor)
        .filter(v -> v != null)
        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

    StringBuilder csv = new StringBuilder();

    // 1) Cabeçalho + Resumo
    csv.append("Relatorio;").append(mesAno).append("\n");
    csv.append("\n");

    // 2) Lançamentos do mês
    csv.append("Lancamentos");
    csv.append("Descricao;Data;Valor;Pagador;Dividido com;Valor por pessoa;Obs\n");

    for (Lancamento l : lancamentosMes) {
      List<LancamentoRateio> rateios = rateioRepository.findByLancamentoId(l.getId());

      // Participantes (nomes únicos)
      List<String> nomes = rateios.stream()
          .map(r -> r.getPessoa().getNome())
          .distinct()
          .sorted(String.CASE_INSENSITIVE_ORDER)
          .toList();

      // “Dividido com”: mostrar todo mundo menos o pagador (fica mais natural)
      String divididoCom = nomes.stream()
          .filter(n -> l.getPagador() == null || !n.equalsIgnoreCase(l.getPagador().getNome()))
          .reduce((a, b) -> a + ", " + b)
          .orElse("");

      // Valor por pessoa: se tiver rateio, pega do primeiro; senão calcula por qtd se
      // tiver
      java.math.BigDecimal valorPorPessoa = java.math.BigDecimal.ZERO;
      if (!rateios.isEmpty() && rateios.get(0).getValorDevido() != null) {
        valorPorPessoa = rateios.get(0).getValorDevido();
      } else if (!nomes.isEmpty() && l.getValor() != null) {
        valorPorPessoa = l.getValor().divide(java.math.BigDecimal.valueOf(nomes.size()), 2,
            java.math.RoundingMode.HALF_UP);
      }

      csv.append(escape(l.getDescricao())).append(";")
          .append(l.getData() != null ? l.getData().format(dataFmt) : "").append(";")
          .append(toPtBr(l.getValor())).append(";")
          .append(escape(l.getPagador() != null ? l.getPagador().getNome() : "")).append(";")
          .append(escape(divididoCom)).append(";")
          .append(toPtBr(valorPorPessoa)).append(";")
          .append("") // Obs: se você tiver observação no Lancamento, coloca aqui
          .append("\n");
    }

    csv.append("\n");

    // 3) Pagamentos do mês
    csv.append("Pagamentos do mes (abatimentos)\n");
    csv.append("Data;Valor;Pagador;Recebedor;Obs\n");

    for (Pagamento p : pagamentosMes) {
      csv.append(p.getData() != null ? p.getData().format(dataFmt) : "").append(";")
          .append(toPtBr(p.getValor())).append(";")
          .append(escape(p.getPagador() != null ? p.getPagador().getNome() : "")).append(";")
          .append(escape(p.getRecebedor() != null ? p.getRecebedor().getNome() : "")).append(";")
          .append(escape(p.getObservacao()))
          .append("\n");
    }

    csv.append("Total pagos no mes;").append(toPtBr(totalPagoMes)).append("\n");
    csv.append("\n");

    // 5) Quem deve (acumulado até o mês)
    csv.append("Quem deve (acumulado)\n");
    csv.append("Devedor;Credor;Valor\n");
    for (TransferenciaResponse t : quemDeveAcumulado) {
      csv.append(escape(t.getDevedor())).append(";")
          .append(escape(t.getCredor())).append(";")
          .append(toPtBr(t.getValor()))
          .append("\n");
    }

    String filename = String.format("relatorio-%02d-%d.csv", mes, ano);
    csv.append("\n");
    csv.append("\n");

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
