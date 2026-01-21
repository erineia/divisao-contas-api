package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.dto.SaldoPessoaResponse;
import br.com.neia.divisaocontas.dto.TransferenciaResponse;
import br.com.neia.divisaocontas.entity.Lancamento;
import br.com.neia.divisaocontas.entity.LancamentoRateio;
import br.com.neia.divisaocontas.entity.Pagamento;
import br.com.neia.divisaocontas.repository.LancamentoRateioRepository;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Service
public class RelatorioService {

  public record CsvResult(String filename, byte[] content) {
  }

  private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final SaldoService saldoService;
  private final CategoriaService categoriaService;
  private final LancamentoRepository lancamentoRepository;
  private final LancamentoRateioRepository rateioRepository;
  private final PagamentoRepository pagamentoRepository;

  public RelatorioService(SaldoService saldoService,
      CategoriaService categoriaService,
      LancamentoRepository lancamentoRepository,
      LancamentoRateioRepository rateioRepository,
      PagamentoRepository pagamentoRepository) {

    this.saldoService = saldoService;
    this.categoriaService = categoriaService;
    this.lancamentoRepository = lancamentoRepository;
    this.rateioRepository = rateioRepository;
    this.pagamentoRepository = pagamentoRepository;
  }

  @Transactional(readOnly = true)
  public CsvResult relatorioMensalCsv(int ano, int mes, Long categoriaId) {
    validarMesAno(ano, mes);

    String mesAno = String.format("%02d/%d", mes, ano);

    LocalDate inicio = LocalDate.of(ano, mes, 1);
    LocalDate fim = inicio.plusMonths(1).minusDays(1);

    final Long categoriaEfetivaId = (categoriaId != null)
        ? categoriaId
        : categoriaService.getOrCreateMes(mes).getId();

    List<Lancamento> lancamentosMes = lancamentoRepository.findAll()
        .stream()
        .filter(l -> !l.getData().isBefore(inicio) && !l.getData().isAfter(fim))
        .filter(l -> l.getCategoria() != null && categoriaEfetivaId.equals(l.getCategoria().getId()))
        .sorted(Comparator.comparing(Lancamento::getData))
        .toList();

    List<Pagamento> pagamentosMes = pagamentoRepository.findAll()
        .stream()
        .filter(p -> !p.getData().isBefore(inicio) && !p.getData().isAfter(fim))
        .filter(p -> p.getCategoria() != null && categoriaEfetivaId.equals(p.getCategoria().getId()))
        .sorted(Comparator.comparing(Pagamento::getData))
        .toList();

    List<TransferenciaResponse> quemDeveAcumulado = saldoService.acumuladoAteMes(ano, mes, categoriaEfetivaId);

    BigDecimal totalPagoMes = pagamentosMes.stream()
        .map(Pagamento::getValor)
        .filter(v -> v != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    StringBuilder csv = new StringBuilder();

    csv.append("Relatorio;").append(mesAno).append("\n");
    csv.append("\n");

    csv.append("Lancamentos");
    csv.append("Descricao;Data;Valor;Pagador;Dividido com;Valor por pessoa;Obs\n");

    for (Lancamento l : lancamentosMes) {
      List<LancamentoRateio> rateios = rateioRepository.findByLancamentoId(l.getId());

      List<String> nomes = rateios.stream()
          .map(r -> r.getPessoa().getNome())
          .distinct()
          .sorted(String.CASE_INSENSITIVE_ORDER)
          .toList();

      String divididoCom = nomes.stream()
          .filter(n -> l.getPagador() == null || !n.equalsIgnoreCase(l.getPagador().getNome()))
          .reduce((a, b) -> a + ", " + b)
          .orElse("");

      BigDecimal valorPorPessoa = BigDecimal.ZERO;
      if (!rateios.isEmpty() && rateios.get(0).getValorDevido() != null) {
        valorPorPessoa = rateios.get(0).getValorDevido();
      } else if (!nomes.isEmpty() && l.getValor() != null) {
        valorPorPessoa = l.getValor().divide(BigDecimal.valueOf(nomes.size()), 2, RoundingMode.HALF_UP);
      }

      csv.append(escape(l.getDescricao())).append(";")
          .append(l.getData() != null ? l.getData().format(DATA_BR) : "").append(";")
          .append(toPtBr(l.getValor())).append(";")
          .append(escape(l.getPagador() != null ? l.getPagador().getNome() : "")).append(";")
          .append(escape(divididoCom)).append(";")
          .append(toPtBr(valorPorPessoa)).append(";")
          .append("")
          .append("\n");
    }

    csv.append("\n");

    csv.append("Pagamentos do mes (abatimentos)\n");
    csv.append("Data;Valor;Pagador;Recebedor;Obs\n");

    for (Pagamento p : pagamentosMes) {
      csv.append(p.getData() != null ? p.getData().format(DATA_BR) : "").append(";")
          .append(toPtBr(p.getValor())).append(";")
          .append(escape(p.getPagador() != null ? p.getPagador().getNome() : "")).append(";")
          .append(escape(p.getRecebedor() != null ? p.getRecebedor().getNome() : "")).append(";")
          .append(escape(p.getObservacao()))
          .append("\n");
    }

    csv.append("Total pagos no mes;").append(toPtBr(totalPagoMes)).append("\n");
    csv.append("\n");

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

    return new CsvResult(filename, csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  @Transactional(readOnly = true)
  public CsvResult saldosPeriodoCsv(String dataInicio, String dataFim) {
    validarDataBr(dataInicio);
    validarDataBr(dataFim);

    List<SaldoPessoaResponse> saldos = saldoService.saldoPorPeriodo(dataInicio, dataFim, null)
        .stream()
        .filter(s -> s.getTotalPago().compareTo(BigDecimal.ZERO) != 0
            || s.getValorDevido().compareTo(BigDecimal.ZERO) != 0
            || s.getTotalAReceber().compareTo(BigDecimal.ZERO) != 0)
        .toList();
    List<TransferenciaResponse> quemDeve = saldoService.quemDevePeriodo(dataInicio, dataFim, null);

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

    return new CsvResult(filename, csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private void validarMesAno(int ano, int mes) {
    if (mes < 1 || mes > 12) {
      throw new IllegalArgumentException("Mês deve ser entre 1 e 12.");
    }
    if (ano < 2000 || ano > 2100) {
      throw new IllegalArgumentException("Ano inválido.");
    }
  }

  private void validarDataBr(String data) {
    try {
      LocalDate.parse(data, DATA_BR);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Formato de data inválido. Use dd/MM/yyyy.");
    }
  }

  private String toPtBr(BigDecimal v) {
    if (v == null) {
      return "0,00";
    }
    return v.setScale(2, RoundingMode.HALF_UP)
        .toString()
        .replace('.', ',');
  }

  private String escape(String s) {
    if (s == null) {
      return "";
    }
    if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
      return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    return s;
  }
}
