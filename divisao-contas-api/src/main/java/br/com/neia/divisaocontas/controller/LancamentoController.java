package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.LancamentoCreateRequest;
import br.com.neia.divisaocontas.dto.LancamentoResponse;
import br.com.neia.divisaocontas.entity.Lancamento;
import br.com.neia.divisaocontas.entity.LancamentoRateio;
import br.com.neia.divisaocontas.entity.Pessoa;
import br.com.neia.divisaocontas.repository.LancamentoRateioRepository;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import br.com.neia.divisaocontas.exception.NotFoundException;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {

  private final LancamentoRepository lancamentoRepository;
  private final PessoaRepository pessoaRepository;
  private final LancamentoRateioRepository rateioRepository;

  public LancamentoController(LancamentoRepository lancamentoRepository,
      PessoaRepository pessoaRepository,
      LancamentoRateioRepository rateioRepository) {
    this.lancamentoRepository = lancamentoRepository;
    this.pessoaRepository = pessoaRepository;
    this.rateioRepository = rateioRepository;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public LancamentoResponse criar(@RequestBody LancamentoCreateRequest req) {
    System.out.println(">>> POST /api/lancamentos chamado em: " + java.time.OffsetDateTime.now());
    Pessoa pagador = pessoaRepository.findById(req.getPagadorId())
        .orElseThrow(() -> new IllegalArgumentException("Pagador não encontrado"));

    // Verificação de duplicidade
    boolean existe = lancamentoRepository.existsByDescricaoAndDataAndValorAndPagador(
        req.getDescricao(), req.getData(), req.getValor(), pagador);
    if (existe) {
      throw new IllegalArgumentException("Lançamento já existe.");
    }

    Lancamento l = new Lancamento();
    l.setDescricao(req.getDescricao());
    l.setData(req.getData());
    l.setValor(req.getValor());
    l.setPagador(pagador);

    // 1) Salva o lançamento
    Lancamento salvo = lancamentoRepository.save(l);

    List<Long> ids = req.getParticipantesIds();
    if (ids == null || ids.isEmpty()) {
      throw new IllegalArgumentException("Informe pelo menos 1 participante para dividir o lançamento.");
    }

    // Garante que o pagador está na lista de participantes
    if (!ids.contains(req.getPagadorId())) {
      ids.add(req.getPagadorId());
    }

    // Remove duplicatas caso o pagador já esteja na lista
    List<Long> idsUnicos = ids.stream().distinct().toList();

    List<Pessoa> participantes = pessoaRepository.findAllById(idsUnicos);
    if (participantes.size() != idsUnicos.size()) {
      throw new IllegalArgumentException("Um ou mais participantes não foram encontrados.");
    }

    BigDecimal valorPorPessoa = salvo.getValor()
        .divide(BigDecimal.valueOf(participantes.size()), 2, RoundingMode.HALF_UP);

    for (Pessoa p : participantes) {
      LancamentoRateio r = new LancamentoRateio();
      r.setLancamento(salvo);
      r.setPessoa(p);
      r.setValorDevido(valorPorPessoa);
      rateioRepository.save(r);
    }

    // 3) Retorna DTO “bonito”
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    return new LancamentoResponse(
        salvo.getId(),
        salvo.getDescricao(),
        salvo.getData().format(fmt),
        salvo.getValor(),
        salvo.getPagador().getNome());
  }

  @GetMapping
  public List<LancamentoResponse> listar() {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    return lancamentoRepository.findAll()
        .stream()
        .map(l -> new LancamentoResponse(
            l.getId(),
            l.getDescricao(),
            l.getData().format(fmt),
            l.getValor(),
            l.getPagador().getNome()))
        .toList();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletar(@PathVariable Long id) {
    if (!lancamentoRepository.existsById(id)) {
      throw new NotFoundException("Lançamento não encontrado.");
    }
    lancamentoRepository.deleteById(id);
  }

}
