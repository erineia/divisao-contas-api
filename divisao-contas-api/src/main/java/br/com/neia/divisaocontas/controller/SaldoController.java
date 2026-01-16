package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.entity.Pagamento;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
  private final PagamentoRepository pagamentoRepository;

  public SaldoController(PessoaRepository pessoaRepository,
      LancamentoRepository lancamentoRepository,
      LancamentoRateioRepository rateioRepository,
      PagamentoRepository pagamentoRepository) {

    this.pessoaRepository = pessoaRepository;
    this.lancamentoRepository = lancamentoRepository;
    this.rateioRepository = rateioRepository;
    this.pagamentoRepository = pagamentoRepository;
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

    LocalDate inicio = parseDataBrOuIso(dataInicio);
    LocalDate fim = parseDataBrOuIso(dataFim);

    if (fim.isBefore(inicio)) {
      throw new IllegalArgumentException("dataFim não pode ser menor que dataInicio.");
    }

    // ...existing code...

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

  private LocalDate parseDataBrOuIso(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("Data é obrigatória.");
    }

    String value = raw.trim();

    try {
      // ISO: yyyy-MM-dd
      if (value.length() == 10 && value.charAt(4) == '-' && value.charAt(7) == '-') {
        return LocalDate.parse(value);
      }

      // BR: dd/MM/yyyy
      DateTimeFormatter br = DateTimeFormatter.ofPattern("dd/MM/yyyy");
      return LocalDate.parse(value, br);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(
          "Formato de data inválido. Use dd/MM/yyyy (ex: 16/01/2026) ou yyyy-MM-dd (ex: 2026-01-16).");
    }
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

  @GetMapping("/acumulado")
  public List<TransferenciaResponse> acumuladoAteMes(@RequestParam int ateAno, @RequestParam int ateMes) {

    if (ateMes < 1 || ateMes > 12) {
      throw new IllegalArgumentException("O mês deve estar entre 1 e 12.");
    }
    if (ateAno < 2000 || ateAno > 2100) {
      throw new IllegalArgumentException("Ano inválido.");
    }

    LocalDate fimMes = LocalDate.of(ateAno, ateMes, 1).plusMonths(1).minusDays(1);

    // Mapa de dívidas: devedorId -> (credorId -> valor)
    Map<Long, Map<Long, BigDecimal>> dividas = new HashMap<>();

    // 1) Soma dívidas geradas pelos rateios (até fimMes)
    List<LancamentoRateio> rateios = rateioRepository.findAll();
    for (LancamentoRateio r : rateios) {
      LocalDate data = r.getLancamento().getData();
      if (data.isAfter(fimMes))
        continue;

      Long devedorId = r.getPessoa().getId();
      Long credorId = r.getLancamento().getPagador().getId();

      // não cria “dívida para si mesmo”
      if (devedorId.equals(credorId))
        continue;

      adicionarDivida(dividas, devedorId, credorId, r.getValorDevido());
    }

    // 2) Abate pagamentos reais (até fimMes)
    List<Pagamento> pagamentos = pagamentoRepository.findAll();
    for (Pagamento p : pagamentos) {
      if (p.getData().isAfter(fimMes))
        continue;

      Long pagadorId = p.getPagador().getId();
      Long recebedorId = p.getRecebedor().getId();

      abaterPagamento(dividas, pagadorId, recebedorId, p.getValor());
    }

    // 3) Monta saída “quem deve pra quem”
    Map<Long, Pessoa> pessoas = pessoaRepository.findAll()
        .stream()
        .collect(java.util.stream.Collectors.toMap(Pessoa::getId, x -> x));

    List<TransferenciaResponse> resp = new ArrayList<>();

    for (Map.Entry<Long, Map<Long, BigDecimal>> e : dividas.entrySet()) {
      Long devedorId = e.getKey();
      for (Map.Entry<Long, BigDecimal> c : e.getValue().entrySet()) {
        Long credorId = c.getKey();
        BigDecimal valor = c.getValue();

        if (valor != null && valor.compareTo(BigDecimal.ZERO) > 0) {
          String devedor = pessoas.get(devedorId).getNome();
          String credor = pessoas.get(credorId).getNome();

          resp.add(new TransferenciaResponse(devedor, credor, valor.setScale(2, java.math.RoundingMode.HALF_UP)));
        }
      }
    }

    // ordena por valor desc
    resp.sort((a, b) -> b.getValor().compareTo(a.getValor()));
    return resp;
  }

  private void adicionarDivida(Map<Long, Map<Long, BigDecimal>> dividas,
      Long devedorId, Long credorId, BigDecimal valor) {

    if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0)
      return;

    dividas.computeIfAbsent(devedorId, k -> new HashMap<>());
    Map<Long, BigDecimal> inner = dividas.get(devedorId);

    inner.put(credorId, inner.getOrDefault(credorId, BigDecimal.ZERO).add(valor));
  }

  private void abaterPagamento(Map<Long, Map<Long, BigDecimal>> dividas,
      Long pagadorId, Long recebedorId, BigDecimal valorPago) {

    if (valorPago == null || valorPago.compareTo(BigDecimal.ZERO) <= 0)
      return;

    // dívida atual: pagador -> recebedor
    BigDecimal atual = dividas
        .getOrDefault(pagadorId, new HashMap<>())
        .getOrDefault(recebedorId, BigDecimal.ZERO);

    // Se pagou menos ou igual ao que devia: reduz a dívida
    if (atual.compareTo(valorPago) >= 0) {
      BigDecimal novo = atual.subtract(valorPago);

      dividas.computeIfAbsent(pagadorId, k -> new HashMap<>());
      if (novo.compareTo(BigDecimal.ZERO) == 0) {
        dividas.get(pagadorId).remove(recebedorId);
      } else {
        dividas.get(pagadorId).put(recebedorId, novo);
      }
      return;
    }

    // Se pagou mais do que devia: zera essa dívida e cria crédito invertido
    // (recebedor -> pagador)
    BigDecimal restante = valorPago.subtract(atual);

    if (dividas.containsKey(pagadorId)) {
      dividas.get(pagadorId).remove(recebedorId);
    }

    // agora o recebedor “fica devendo” para o pagador o excedente (crédito do
    // pagador)
    adicionarDivida(dividas, recebedorId, pagadorId, restante);
  }

}
