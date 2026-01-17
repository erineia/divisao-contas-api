package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.PessoaCreateRequest;
import br.com.neia.divisaocontas.dto.PessoaResponse;
import br.com.neia.divisaocontas.entity.Pessoa;
import br.com.neia.divisaocontas.repository.LancamentoRateioRepository;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pessoas")
public class PessoaController {

  private final PessoaRepository pessoaRepository;
  private final PagamentoRepository pagamentoRepository;
  private final LancamentoRepository lancamentoRepository;
  private final LancamentoRateioRepository rateioRepository;

  public PessoaController(PessoaRepository pessoaRepository,
      PagamentoRepository pagamentoRepository,
      LancamentoRepository lancamentoRepository,
      LancamentoRateioRepository rateioRepository) {
    this.pessoaRepository = pessoaRepository;
    this.pagamentoRepository = pagamentoRepository;
    this.lancamentoRepository = lancamentoRepository;
    this.rateioRepository = rateioRepository;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PessoaResponse criar(@RequestBody PessoaCreateRequest req) {
    String nome = req.getNome() == null ? "" : req.getNome().trim();

    if (nome.isEmpty()) {
      throw new IllegalArgumentException("nome é obrigatório");
    }

    if (pessoaRepository.existsByNomeIgnoreCase(nome)) {
      throw new IllegalArgumentException("Já existe uma pessoa com esse nome.");
    }

    Pessoa salva = pessoaRepository.save(new Pessoa(nome));
    return new PessoaResponse(salva.getId(), salva.getNome());
  }

  @GetMapping
  public List<PessoaResponse> listar() {
    return pessoaRepository.findAll()
        .stream()
        .map(p -> new PessoaResponse(p.getId(), p.getNome()))
        .toList();
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<Map<String, String>> deletar(@PathVariable Long id) {

    boolean existe = pessoaRepository.existsById(id);
    if (!existe) {
      return ResponseEntity.ok(Map.of("mensagem", "Pessoa informada não existe."));
    }

    boolean temPagamentos = pagamentoRepository.existsByPagadorId(id) || pagamentoRepository.existsByRecebedorId(id);
    boolean temLancamentos = lancamentoRepository.existsByPagadorId(id);
    boolean temRateios = rateioRepository.existsByPessoaId(id);

    if (temPagamentos || temLancamentos || temRateios) {
      throw new IllegalArgumentException(
          "Não é possível excluir a pessoa porque ela possui vínculos (pagamentos, lançamentos ou rateios).");
    }

    pessoaRepository.deleteById(id);
    return ResponseEntity.ok(Map.of("mensagem", "Pessoa excluída com sucesso!"));
  }

}
