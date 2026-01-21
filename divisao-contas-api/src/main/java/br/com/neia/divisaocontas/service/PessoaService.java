package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.dto.PessoaCreateRequest;
import br.com.neia.divisaocontas.dto.PessoaResponse;
import br.com.neia.divisaocontas.entity.Pessoa;
import br.com.neia.divisaocontas.exception.DuplicateException;
import br.com.neia.divisaocontas.exception.NotFoundException;
import br.com.neia.divisaocontas.repository.LancamentoRateioRepository;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PessoaService {

  private final PessoaRepository pessoaRepository;
  private final PagamentoRepository pagamentoRepository;
  private final LancamentoRepository lancamentoRepository;
  private final LancamentoRateioRepository rateioRepository;

  public PessoaService(PessoaRepository pessoaRepository,
      PagamentoRepository pagamentoRepository,
      LancamentoRepository lancamentoRepository,
      LancamentoRateioRepository rateioRepository) {
    this.pessoaRepository = pessoaRepository;
    this.pagamentoRepository = pagamentoRepository;
    this.lancamentoRepository = lancamentoRepository;
    this.rateioRepository = rateioRepository;
  }

  @Transactional
  public PessoaResponse criar(PessoaCreateRequest req) {
    String nome = normalizarNome(req);

    if (pessoaRepository.existsByNomeIgnoreCase(nome)) {
      throw new DuplicateException("Já existe uma pessoa com esse nome.");
    }

    Pessoa salva = pessoaRepository.save(new Pessoa(nome));
    return new PessoaResponse(salva.getId(), salva.getNome());
  }

  @Transactional(readOnly = true)
  public List<PessoaResponse> listar() {
    return pessoaRepository.findAll()
        .stream()
        .map(p -> new PessoaResponse(p.getId(), p.getNome()))
        .toList();
  }

  @Transactional
  public PessoaResponse atualizar(Long id, PessoaCreateRequest req) {
    Pessoa pessoa = pessoaRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Pessoa não encontrada."));

    String nome = normalizarNome(req);

    if (pessoaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
      throw new DuplicateException("Já existe uma pessoa com esse nome.");
    }

    pessoa.setNome(nome);
    Pessoa salva = pessoaRepository.save(pessoa);
    return new PessoaResponse(salva.getId(), salva.getNome());
  }

  @Transactional
  public void deletar(Long id) {
    pessoaRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Pessoa não encontrada."));

    boolean temPagamentos = pagamentoRepository.existsByPagadorId(id) || pagamentoRepository.existsByRecebedorId(id);
    boolean temLancamentos = lancamentoRepository.existsByPagadorId(id);
    boolean temRateios = rateioRepository.existsByPessoaId(id);

    if (temPagamentos || temLancamentos || temRateios) {
      throw new IllegalArgumentException(
          "Não é possível excluir a pessoa porque ela possui vínculos (pagamentos, lançamentos ou rateios).");
    }

    pessoaRepository.deleteById(id);
  }

  private String normalizarNome(PessoaCreateRequest req) {
    String nome = req.getNome() == null ? "" : req.getNome().trim();

    if (nome.isEmpty()) {
      throw new IllegalArgumentException("nome é obrigatório");
    }

    return nome;
  }
}
