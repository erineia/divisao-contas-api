package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.SaldoPessoaResponse;
import br.com.neia.divisaocontas.entity.Lancamento;
import br.com.neia.divisaocontas.entity.LancamentoRateio;
import br.com.neia.divisaocontas.entity.Pessoa;
import br.com.neia.divisaocontas.repository.LancamentoRateioRepository;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import org.springframework.web.bind.annotation.*;
import br.com.neia.divisaocontas.dto.TransferenciaResponse;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/saldos")
public class SaldoController {

  private final PessoaRepository pessoaRepository;
  private final LancamentoRepository lancamentoRepository;
  private final LancamentoRateioRepository rateioRepository;

  public SaldoController(PessoaRepository pessoaRepository,
      LancamentoRepository lancamentoRepository,
      LancamentoRateioRepository rateioRepository) {
    this.pessoaRepository = pessoaRepository;
    this.lancamentoRepository = lancamentoRepository;
    this.rateioRepository = rateioRepository;
  }

  @GetMapping
  public List<SaldoPessoaResponse> saldoDoMes(@RequestParam int ano, @RequestParam int mes) {

    LocalDate inicio = LocalDate.of(ano, mes, 1);
    LocalDate fim = inicio.plusMonths(1).minusDays(1);

    List<Pessoa> pessoas = pessoaRepository.findAll();

    Map<Long, BigDecimal> totalPago = new HashMap<>();
    Map<Long, BigDecimal> totalDevido = new HashMap<>();

    for (Pessoa p : pessoas) {
      totalPago.put(p.getId(), BigDecimal.ZERO);
      totalDevido.put(p.getId(), BigDecimal.ZERO);
    }

    // 1) Soma o que cada um pagou no mês
    List<Lancamento> lancamentos = lancamentoRepository.findAll();
    for (Lancamento l : lancamentos) {
      if (l.getData().isBefore(inicio) || l.getData().isAfter(fim))
        continue;

      Long pagadorId = l.getPagador().getId();
      totalPago.put(pagadorId, totalPago.get(pagadorId).add(l.getValor()));
    }

    // 2) Soma o que cada um deve (rateios) no mês
    List<LancamentoRateio> rateios = rateioRepository.findAll();
    for (LancamentoRateio r : rateios) {
      LocalDate dataLanc = r.getLancamento().getData();
      if (dataLanc.isBefore(inicio) || dataLanc.isAfter(fim))
        continue;

      Long pessoaId = r.getPessoa().getId();
      totalDevido.put(pessoaId, totalDevido.get(pessoaId).add(r.getValorDevido()));
    }

    // 3) Monta a resposta
    List<SaldoPessoaResponse> resp = new ArrayList<>();
    for (Pessoa p : pessoas) {
      BigDecimal pago = totalPago.get(p.getId());
      BigDecimal devido = totalDevido.get(p.getId());

      BigDecimal saldo = pago.subtract(devido);

      BigDecimal valorDevido = BigDecimal.ZERO;
      BigDecimal totalAReceber = BigDecimal.ZERO;

      if (saldo.compareTo(BigDecimal.ZERO) > 0) {
        totalAReceber = saldo;
      } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
        valorDevido = saldo.abs();
      }

      resp.add(new SaldoPessoaResponse(
          p.getId(),
          p.getNome(),
          pago,
          valorDevido,
          totalAReceber));
    }
    resp.sort((a, b) -> b.getTotalAReceber().compareTo(a.getTotalAReceber()));
    return resp;
  }

  @GetMapping("/quem-deve")
  public List<TransferenciaResponse> quemDeve(@RequestParam int ano, @RequestParam int mes) {
    if (mes < 1 || mes > 12) {
      throw new IllegalArgumentException("O mês deve estar entre 1 e 12.");
    }

    if (ano < 2000 || ano > 2100) {
      throw new IllegalArgumentException("Ano inválido.");
    }

    List<SaldoPessoaResponse> saldos = saldoDoMes(ano, mes);

    // devedores: valorDevido > 0
    List<SaldoPessoaResponse> devedores = saldos.stream()
        .filter(s -> s.getValorDevido().compareTo(BigDecimal.ZERO) > 0)
        .collect(Collectors.toList());

    // credores: totalAReceber > 0
    List<SaldoPessoaResponse> credores = saldos.stream()
        .filter(s -> s.getTotalAReceber().compareTo(BigDecimal.ZERO) > 0)
        .collect(Collectors.toList());

    List<TransferenciaResponse> resultado = new ArrayList<>();

    int i = 0, j = 0;
    while (i < devedores.size() && j < credores.size()) {
      SaldoPessoaResponse dev = devedores.get(i);
      SaldoPessoaResponse cre = credores.get(j);

      BigDecimal deve = dev.getValorDevido();
      BigDecimal recebe = cre.getTotalAReceber();

      BigDecimal valor = deve.min(recebe);

      resultado.add(new TransferenciaResponse(dev.getNome(), cre.getNome(), valor));

      // reduz valores "temporariamente" para continuar distribuindo
      BigDecimal novoDeve = deve.subtract(valor);
      BigDecimal novoRecebe = recebe.subtract(valor);

      devedores.set(i,
          new SaldoPessoaResponse(dev.getPessoaId(), dev.getNome(), dev.getTotalPago(), novoDeve, BigDecimal.ZERO));
      credores.set(j,
          new SaldoPessoaResponse(cre.getPessoaId(), cre.getNome(), cre.getTotalPago(), BigDecimal.ZERO, novoRecebe));

      if (novoDeve.compareTo(BigDecimal.ZERO) == 0)
        i++;
      if (novoRecebe.compareTo(BigDecimal.ZERO) == 0)
        j++;
    }

    return resultado;
  }

  @GetMapping("/periodo")
  public List<SaldoPessoaResponse> saldoPorPeriodo(@RequestParam String dataInicio,
      @RequestParam String dataFim) {

    LocalDate inicio = LocalDate.parse(dataInicio);
    LocalDate fim = LocalDate.parse(dataFim);

    if (fim.isBefore(inicio)) {
      throw new IllegalArgumentException("dataFim não pode ser menor que dataInicio.");
    }

    // 1) Carrega pessoas
    List<Pessoa> pessoas = pessoaRepository.findAll();

    // 2) Inicializa mapas
    Map<Long, BigDecimal> totalPago = new HashMap<>();
    Map<Long, BigDecimal> totalDevido = new HashMap<>();

    for (Pessoa p : pessoas) {
      totalPago.put(p.getId(), BigDecimal.ZERO);
      totalDevido.put(p.getId(), BigDecimal.ZERO);
    }

    // 3) Soma o que cada um pagou no período
    List<Lancamento> lancamentos = lancamentoRepository.findAll();
    for (Lancamento l : lancamentos) {
      LocalDate data = l.getData();
      if (data.isBefore(inicio) || data.isAfter(fim))
        continue;

      Long pagadorId = l.getPagador().getId();
      totalPago.put(pagadorId, totalPago.get(pagadorId).add(l.getValor()));
    }

    // 4) Soma o que cada um deve (rateios) no período
    List<LancamentoRateio> rateios = rateioRepository.findAll();
    for (LancamentoRateio r : rateios) {
      LocalDate dataLanc = r.getLancamento().getData();
      if (dataLanc.isBefore(inicio) || dataLanc.isAfter(fim))
        continue;

      Long pessoaId = r.getPessoa().getId();
      totalDevido.put(pessoaId, totalDevido.get(pessoaId).add(r.getValorDevido()));
    }

    // 5) Monta resposta (formato humano: totalPago, valorDevido, totalAReceber)
    List<SaldoPessoaResponse> resp = new ArrayList<>();

    for (Pessoa p : pessoas) {
      BigDecimal pago = totalPago.get(p.getId());
      BigDecimal devido = totalDevido.get(p.getId());

      BigDecimal saldo = pago.subtract(devido);

      BigDecimal valorDevido = BigDecimal.ZERO;
      BigDecimal totalAReceber = BigDecimal.ZERO;

      if (saldo.compareTo(BigDecimal.ZERO) > 0) {
        totalAReceber = saldo;
      } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
        valorDevido = saldo.abs();
      }

      resp.add(new SaldoPessoaResponse(
          p.getId(),
          p.getNome(),
          pago,
          valorDevido,
          totalAReceber));
    }

    // opcional: ordenar (quem tem a receber primeiro)
    resp.sort((a, b) -> b.getTotalAReceber().compareTo(a.getTotalAReceber()));

    return resp;
  }

  @GetMapping("/quem-deve/periodo")
  public List<TransferenciaResponse> quemDevePeriodo(@RequestParam String dataInicio,
      @RequestParam String dataFim) {

    List<SaldoPessoaResponse> saldos = saldoPorPeriodo(dataInicio, dataFim);

    List<SaldoPessoaResponse> devedores = saldos.stream()
        .filter(s -> s.getValorDevido().compareTo(BigDecimal.ZERO) > 0)
        .collect(Collectors.toList());

    List<SaldoPessoaResponse> credores = saldos.stream()
        .filter(s -> s.getTotalAReceber().compareTo(BigDecimal.ZERO) > 0)
        .collect(Collectors.toList());

    List<TransferenciaResponse> resultado = new ArrayList<>();

    int i = 0, j = 0;
    while (i < devedores.size() && j < credores.size()) {
      SaldoPessoaResponse dev = devedores.get(i);
      SaldoPessoaResponse cre = credores.get(j);

      BigDecimal deve = dev.getValorDevido();
      BigDecimal recebe = cre.getTotalAReceber();

      BigDecimal valor = deve.min(recebe);

      resultado.add(new TransferenciaResponse(dev.getNome(), cre.getNome(), valor));

      BigDecimal novoDeve = deve.subtract(valor);
      BigDecimal novoRecebe = recebe.subtract(valor);

      devedores.set(i,
          new SaldoPessoaResponse(dev.getPessoaId(), dev.getNome(), dev.getTotalPago(), novoDeve, BigDecimal.ZERO));
      credores.set(j,
          new SaldoPessoaResponse(cre.getPessoaId(), cre.getNome(), cre.getTotalPago(), BigDecimal.ZERO, novoRecebe));

      if (novoDeve.compareTo(BigDecimal.ZERO) == 0)
        i++;
      if (novoRecebe.compareTo(BigDecimal.ZERO) == 0)
        j++;
    }

    return resultado;
  }
}
