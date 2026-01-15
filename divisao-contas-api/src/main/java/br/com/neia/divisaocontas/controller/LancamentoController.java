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
    Pessoa pagador = pessoaRepository.findById(req.getPagadorId())
        .orElseThrow(() -> new IllegalArgumentException("Pagador não encontrado"));

    Lancamento l = new Lancamento();
    l.setDescricao(req.getDescricao());
    l.setData(req.getData());
    l.setValor(req.getValor());
    l.setPagador(pagador);

    // 1) Salva o lançamento
    Lancamento salvo = lancamentoRepository.save(l);

    List<Long> ids = req.getParticipantesIds();
    if (ids == null || ids.size() < 2) {
      throw new IllegalArgumentException("Informe pelo menos 2 participantes para dividir o lançamento.");
    }

    if (!ids.contains(req.getPagadorId())) {
      throw new IllegalArgumentException("O pagador deve estar na lista de participantes.");
    }

    List<Pessoa> participantes = pessoaRepository.findAllById(ids);
    if (participantes.size() != ids.size()) {
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
    rateioRepository.deleteByLancamentoId(id);
    lancamentoRepository.deleteById(id);
  }

}
