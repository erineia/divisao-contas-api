package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.LancamentoCreateRequest;
import br.com.neia.divisaocontas.dto.LancamentoResponse;
import br.com.neia.divisaocontas.dto.LancamentoPessoaValorResponse;
import br.com.neia.divisaocontas.dto.PessoaResponse;
import br.com.neia.divisaocontas.entity.Lancamento;
import br.com.neia.divisaocontas.entity.LancamentoRateio;
import br.com.neia.divisaocontas.entity.Pessoa;
import br.com.neia.divisaocontas.exception.DuplicateException;
import br.com.neia.divisaocontas.exception.NotFoundException;
import br.com.neia.divisaocontas.repository.LancamentoRateioRepository;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import br.com.neia.divisaocontas.service.FechamentoMesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import br.com.neia.divisaocontas.dto.DevedorItemRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {

  private final LancamentoRepository lancamentoRepository;
  private final PessoaRepository pessoaRepository;
  private final LancamentoRateioRepository rateioRepository;
  private final FechamentoMesService fechamentoMesService;

  public LancamentoController(LancamentoRepository lancamentoRepository,
      PessoaRepository pessoaRepository,
      LancamentoRateioRepository rateioRepository,
      FechamentoMesService fechamentoMesService) {
    this.lancamentoRepository = lancamentoRepository;
    this.pessoaRepository = pessoaRepository;
    this.rateioRepository = rateioRepository;
    this.fechamentoMesService = fechamentoMesService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public LancamentoResponse criar(@RequestBody LancamentoCreateRequest req) {
    if (req.getDescricao() == null || req.getDescricao().trim().isEmpty()) {
      throw new IllegalArgumentException("descricao é obrigatória");
    }
    if (req.getData() == null) {
      throw new IllegalArgumentException("data é obrigatória");
    }
    if (req.getValor() == null || req.getValor().signum() <= 0) {
      throw new IllegalArgumentException("valor deve ser maior que zero.");
    }
    if (req.getPagadorId() == null) {
      throw new IllegalArgumentException("pagadorId é obrigatório");
    }

    fechamentoMesService.validarAberto(req.getData());

    Pessoa pagador = pessoaRepository.findById(req.getPagadorId())
        .orElseThrow(() -> new IllegalArgumentException("Pagador não encontrado"));

    // Verificação de duplicidade
    boolean existe = lancamentoRepository.existsByDescricaoAndDataAndValorAndPagador(
        req.getDescricao(), req.getData(), req.getValor(), pagador);
    if (existe) {
      throw new DuplicateException("Lançamento já existe.");
    }

    Lancamento l = new Lancamento();
    l.setDescricao(req.getDescricao());
    l.setData(req.getData());
    l.setValor(req.getValor());
    l.setPagador(pagador);

    // 1) Salva o lançamento
    Lancamento salvo = lancamentoRepository.save(l);

    // limpa devedores/participantes inválidos
    if (req.isDivide()) {
      // ===== DIVIDIDO =====
      List<Long> ids = req.getParticipantesIds();

      if (ids == null || ids.isEmpty()) {
        throw new IllegalArgumentException("Para divide=true, informe participantesIds.");
      }

      if (ids.stream().anyMatch(i -> i == null)) {
        throw new IllegalArgumentException("participantesIds não pode conter null.");
      }

      // evita duplicados
      Set<Long> uniq = new HashSet<>(ids);
      ids = new ArrayList<>(uniq);

      // garante o pagador na lista
      if (!ids.contains(req.getPagadorId())) {
        ids.add(req.getPagadorId());
      }

      if (ids.size() < 2) {
        throw new IllegalArgumentException("Para dividir, é necessário pelo menos 2 pessoas (incluindo o pagador).");
      }

      List<Pessoa> participantes = pessoaRepository.findAllById(ids);
      if (participantes.size() != ids.size()) {
        throw new IllegalArgumentException("Um ou mais participantes não foram encontrados.");
      }

      participantes.sort((a, b) -> a.getId().compareTo(b.getId()));

      BigDecimal total = salvo.getValor().setScale(2, RoundingMode.HALF_UP);
      long totalCentavos = total.movePointRight(2).longValueExact();
      int n = participantes.size();
      long base = totalCentavos / n;
      long resto = totalCentavos % n;

      for (int i = 0; i < participantes.size(); i++) {
        Pessoa p = participantes.get(i);
        long centavos = base + (i < resto ? 1 : 0);
        BigDecimal valorPorPessoa = BigDecimal.valueOf(centavos).movePointLeft(2);
        LancamentoRateio r = new LancamentoRateio();
        r.setLancamento(salvo);
        r.setPessoa(p);
        r.setValorDevido(valorPorPessoa);
        rateioRepository.save(r);
      }

    } else {
      // ===== NÃO DIVIDIDO (EMPRÉSTIMO / ADIANTAMENTO) =====
      List<DevedorItemRequest> itens = req.getDevedores();

      if (itens == null || itens.isEmpty()) {
        throw new IllegalArgumentException("Para divide=false, informe a lista de devedores.");
      }

      BigDecimal soma = BigDecimal.ZERO;
      Set<Long> uniqDevedores = new HashSet<>();

      for (DevedorItemRequest item : itens) {
        if (item.getPessoaId() == null || item.getValor() == null) {
          throw new IllegalArgumentException("Cada devedor deve ter pessoaId e valor.");
        }
        if (item.getValor().compareTo(BigDecimal.ZERO) <= 0) {
          throw new IllegalArgumentException("Valor do devedor deve ser maior que zero.");
        }
        if (item.getPessoaId().equals(req.getPagadorId())) {
          throw new IllegalArgumentException("O pagador não pode ser devedor.");
        }
        if (!uniqDevedores.add(item.getPessoaId())) {
          throw new IllegalArgumentException("Devedor repetido: pessoaId=" + item.getPessoaId());
        }

        Pessoa devedor = pessoaRepository.findById(item.getPessoaId())
            .orElseThrow(() -> new IllegalArgumentException("Devedor não encontrado: pessoaId=" + item.getPessoaId()));

        LancamentoRateio r = new LancamentoRateio();
        r.setLancamento(salvo);
        r.setPessoa(devedor);
        r.setValorDevido(item.getValor());
        rateioRepository.save(r);

        soma = soma.add(item.getValor());
      }

      // soma precisa bater com o valor total do lançamento
      if (soma.compareTo(salvo.getValor()) != 0) {
        throw new IllegalArgumentException("A soma dos devedores deve ser igual ao valor do lançamento.");
      }
    }

    // 3) Retorna DTO “bonito”
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    LancamentoResponse resp = new LancamentoResponse();
    resp.setId(salvo.getId());
    resp.setDescricao(salvo.getDescricao());
    resp.setData(salvo.getData().format(fmt));
    resp.setValor(salvo.getValor());
    resp.setDivide(req.isDivide());
    resp.setPagador(new PessoaResponse(
        salvo.getPagador().getId(),
        salvo.getPagador().getNome()));

    List<LancamentoRateio> rateios = rateioRepository.findByLancamentoId(salvo.getId());

    if (req.isDivide()) {
      resp.setParticipantes(
          rateios.stream()
              .map(r -> new LancamentoPessoaValorResponse(
                  r.getPessoa().getId(),
                  r.getPessoa().getNome(),
                  r.getValorDevido()))
              .toList());
    } else {
      resp.setDevedores(
          rateios.stream()
              .map(r -> new LancamentoPessoaValorResponse(
                  r.getPessoa().getId(),
                  r.getPessoa().getNome(),
                  r.getValorDevido()))
              .toList());
    }

    return resp;
  }

  @GetMapping
  public List<LancamentoResponse> listar() {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    return lancamentoRepository.findAll()
        .stream()
        .map(l -> {
          List<LancamentoRateio> rateios = rateioRepository.findByLancamentoId(l.getId());
          boolean dividido = rateios.stream()
              .anyMatch(r -> r.getPessoa() != null
                  && r.getPessoa().getId() != null
                  && l.getPagador() != null
                  && l.getPagador().getId() != null
                  && r.getPessoa().getId().equals(l.getPagador().getId()));

          LancamentoResponse resp = new LancamentoResponse();
          resp.setId(l.getId());
          resp.setDescricao(l.getDescricao());
          resp.setData(l.getData().format(fmt));
          resp.setValor(l.getValor());
          resp.setDivide(dividido);
          resp.setPagador(new PessoaResponse(
              l.getPagador().getId(),
              l.getPagador().getNome()));

          if (dividido) {
            resp.setParticipantes(
                rateios.stream()
                    .map(r -> new LancamentoPessoaValorResponse(
                        r.getPessoa().getId(),
                        r.getPessoa().getNome(),
                        r.getValorDevido()))
                    .toList());
          } else {
            resp.setDevedores(
                rateios.stream()
                    .map(r -> new LancamentoPessoaValorResponse(
                        r.getPessoa().getId(),
                        r.getPessoa().getNome(),
                        r.getValorDevido()))
                    .toList());
          }

          return resp;
        })
        .toList();
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody LancamentoCreateRequest req) {
    Lancamento existente = lancamentoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Lançamento não encontrado."));

    fechamentoMesService.validarAberto(existente.getData());
    fechamentoMesService.validarAberto(req.getData());

    if (req.getDescricao() == null || req.getDescricao().trim().isEmpty()) {
      throw new IllegalArgumentException("descricao é obrigatória");
    }
    if (req.getData() == null) {
      throw new IllegalArgumentException("data é obrigatória");
    }
    if (req.getValor() == null || req.getValor().signum() <= 0) {
      throw new IllegalArgumentException("valor deve ser maior que zero.");
    }
    if (req.getPagadorId() == null) {
      throw new IllegalArgumentException("pagadorId é obrigatório");
    }

    Pessoa pagador = pessoaRepository.findById(req.getPagadorId())
        .orElseThrow(() -> new IllegalArgumentException("Pagador não encontrado"));

    boolean duplicado = lancamentoRepository.existsByDescricaoAndDataAndValorAndPagadorAndIdNot(
        req.getDescricao(), req.getData(), req.getValor(), pagador, id);
    if (duplicado) {
      throw new DuplicateException("Lançamento já existe.");
    }

    existente.setDescricao(req.getDescricao());
    existente.setData(req.getData());
    existente.setValor(req.getValor());
    existente.setPagador(pagador);

    Lancamento salvo = lancamentoRepository.save(existente);

    // regrava rateios conforme payload
    rateioRepository.deleteByLancamentoId(id);

    if (req.isDivide()) {
      List<Long> ids = req.getParticipantesIds();

      if (ids == null || ids.isEmpty()) {
        throw new IllegalArgumentException("Para divide=true, informe participantesIds.");
      }

      if (ids.stream().anyMatch(i -> i == null)) {
        throw new IllegalArgumentException("participantesIds não pode conter null.");
      }

      Set<Long> uniq = new HashSet<>(ids);
      ids = new ArrayList<>(uniq);

      if (!ids.contains(req.getPagadorId())) {
        ids.add(req.getPagadorId());
      }

      if (ids.size() < 2) {
        throw new IllegalArgumentException("Para dividir, é necessário pelo menos 2 pessoas (incluindo o pagador).");
      }

      List<Pessoa> participantes = pessoaRepository.findAllById(ids);
      if (participantes.size() != ids.size()) {
        throw new IllegalArgumentException("Um ou mais participantes não foram encontrados.");
      }

      participantes.sort((a, b) -> a.getId().compareTo(b.getId()));

      BigDecimal total = salvo.getValor().setScale(2, RoundingMode.HALF_UP);
      long totalCentavos = total.movePointRight(2).longValueExact();
      int n = participantes.size();
      long base = totalCentavos / n;
      long resto = totalCentavos % n;

      for (int i = 0; i < participantes.size(); i++) {
        Pessoa p = participantes.get(i);
        long centavos = base + (i < resto ? 1 : 0);
        BigDecimal valorPorPessoa = BigDecimal.valueOf(centavos).movePointLeft(2);
        LancamentoRateio r = new LancamentoRateio();
        r.setLancamento(salvo);
        r.setPessoa(p);
        r.setValorDevido(valorPorPessoa);
        rateioRepository.save(r);
      }

    } else {
      List<DevedorItemRequest> itens = req.getDevedores();

      if (itens == null || itens.isEmpty()) {
        throw new IllegalArgumentException("Para divide=false, informe a lista de devedores.");
      }

      BigDecimal soma = BigDecimal.ZERO;
      Set<Long> uniqDevedores = new HashSet<>();

      for (DevedorItemRequest item : itens) {
        if (item.getPessoaId() == null || item.getValor() == null) {
          throw new IllegalArgumentException("Cada devedor deve ter pessoaId e valor.");
        }
        if (item.getValor().compareTo(BigDecimal.ZERO) <= 0) {
          throw new IllegalArgumentException("Valor do devedor deve ser maior que zero.");
        }
        if (item.getPessoaId().equals(req.getPagadorId())) {
          throw new IllegalArgumentException("O pagador não pode ser devedor.");
        }
        if (!uniqDevedores.add(item.getPessoaId())) {
          throw new IllegalArgumentException("Devedor repetido: pessoaId=" + item.getPessoaId());
        }

        Pessoa devedor = pessoaRepository.findById(item.getPessoaId())
            .orElseThrow(() -> new IllegalArgumentException("Devedor não encontrado: pessoaId=" + item.getPessoaId()));

        LancamentoRateio r = new LancamentoRateio();
        r.setLancamento(salvo);
        r.setPessoa(devedor);
        r.setValorDevido(item.getValor());
        rateioRepository.save(r);

        soma = soma.add(item.getValor());
      }

      if (soma.compareTo(salvo.getValor()) != 0) {
        throw new IllegalArgumentException("A soma dos devedores deve ser igual ao valor do lançamento.");
      }
    }

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    LancamentoResponse resp = new LancamentoResponse();
    resp.setId(salvo.getId());
    resp.setDescricao(salvo.getDescricao());
    resp.setData(salvo.getData().format(fmt));
    resp.setValor(salvo.getValor());
    resp.setDivide(req.isDivide());
    resp.setPagador(new PessoaResponse(
        salvo.getPagador().getId(),
        salvo.getPagador().getNome()));

    List<LancamentoRateio> rateios = rateioRepository.findByLancamentoId(salvo.getId());

    if (req.isDivide()) {
      resp.setParticipantes(
          rateios.stream()
              .map(r -> new LancamentoPessoaValorResponse(
                  r.getPessoa().getId(),
                  r.getPessoa().getNome(),
                  r.getValorDevido()))
              .toList());
    } else {
      resp.setDevedores(
          rateios.stream()
              .map(r -> new LancamentoPessoaValorResponse(
                  r.getPessoa().getId(),
                  r.getPessoa().getNome(),
                  r.getValorDevido()))
              .toList());
    }

    return ResponseEntity.ok(resp);
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<Map<String, String>> deletar(@PathVariable Long id) {

    Lancamento l = lancamentoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Lançamento não encontrado."));

    fechamentoMesService.validarAberto(l.getData());

    rateioRepository.deleteByLancamentoId(id);
    lancamentoRepository.deleteById(id);

    return ResponseEntity.ok(Map.of("mensagem", "Lançamento excluído com sucesso!"));
  }

}
