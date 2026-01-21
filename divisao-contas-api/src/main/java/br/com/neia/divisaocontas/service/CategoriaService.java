package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.dto.CategoriaCreateRequest;
import br.com.neia.divisaocontas.dto.CategoriaResponse;
import br.com.neia.divisaocontas.entity.Categoria;
import br.com.neia.divisaocontas.exception.DuplicateException;
import br.com.neia.divisaocontas.exception.NotFoundException;
import br.com.neia.divisaocontas.repository.CategoriaRepository;
import br.com.neia.divisaocontas.repository.FechamentoMesRepository;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CategoriaService {

  private final CategoriaRepository categoriaRepository;
  private final LancamentoRepository lancamentoRepository;
  private final PagamentoRepository pagamentoRepository;
  private final FechamentoMesRepository fechamentoMesRepository;

  public CategoriaService(
      CategoriaRepository categoriaRepository,
      LancamentoRepository lancamentoRepository,
      PagamentoRepository pagamentoRepository,
      FechamentoMesRepository fechamentoMesRepository) {
    this.categoriaRepository = categoriaRepository;
    this.lancamentoRepository = lancamentoRepository;
    this.pagamentoRepository = pagamentoRepository;
    this.fechamentoMesRepository = fechamentoMesRepository;
  }

  @Transactional
  public CategoriaResponse criar(CategoriaCreateRequest req) {
    if (req == null || req.getNome() == null || req.getNome().trim().isEmpty()) {
      throw new IllegalArgumentException("nome é obrigatório");
    }

    String nome = req.getNome().trim();
    Categoria categoria = getOrCreateByNome(nome);
    return toResponse(categoria);
  }

  @Transactional(readOnly = true)
  public List<CategoriaResponse> listar() {
    return categoriaRepository.findAll().stream()
        .map(this::toResponse)
        .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
        .toList();
  }

  @Transactional(readOnly = true)
  public Categoria buscarPorId(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("categoriaId é obrigatório");
    }

    return categoriaRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Categoria não encontrada."));
  }

  @Transactional
  public CategoriaResponse atualizar(Long id, CategoriaCreateRequest req) {
    if (id == null) {
      throw new IllegalArgumentException("id é obrigatório");
    }
    if (req == null || req.getNome() == null || req.getNome().trim().isEmpty()) {
      throw new IllegalArgumentException("nome é obrigatório");
    }

    Categoria categoria = buscarPorId(id);
    String novoNome = req.getNome().trim();

    if (categoria.getNome() != null && categoria.getNome().equalsIgnoreCase(novoNome)) {
      // nada a fazer
      return toResponse(categoria);
    }

    categoriaRepository.findByNome(novoNome)
        .filter(c -> !c.getId().equals(id))
        .ifPresent(c -> {
          throw new DuplicateException("Já existe uma categoria com esse nome.");
        });

    categoria.setNome(novoNome);
    Categoria salvo = categoriaRepository.save(categoria);
    return toResponse(salvo);
  }

  @Transactional
  public void deletar(Long id) {
    Categoria categoria = buscarPorId(id);

    boolean emUso = lancamentoRepository.existsByCategoriaId(categoria.getId())
        || pagamentoRepository.existsByCategoriaId(categoria.getId())
        || fechamentoMesRepository.existsByCategoriaId(categoria.getId());

    if (emUso) {
      throw new DuplicateException("Categoria está em uso e não pode ser removida.");
    }

    categoriaRepository.delete(categoria);
  }

  @Transactional
  public Categoria getOrCreateByNome(String nome) {
    if (nome == null || nome.trim().isEmpty()) {
      throw new IllegalArgumentException("nome é obrigatório");
    }

    String trimmed = nome.trim();

    return categoriaRepository.findByNome(trimmed)
        .orElseGet(() -> categoriaRepository.save(new Categoria(trimmed)));
  }

  @Transactional
  public Categoria getOrCreateMes(int mes) {
    if (mes < 1 || mes > 12) {
      throw new IllegalArgumentException("Mês deve ser entre 1 e 12.");
    }

    String nome = String.format("Mes/%02d", mes);
    return getOrCreateByNome(nome);
  }

  @Transactional
  public Categoria resolveCategoria(Long categoriaId, LocalDate data) {
    if (categoriaId != null) {
      return buscarPorId(categoriaId);
    }

    if (data == null) {
      throw new IllegalArgumentException("data é obrigatória para resolver categoria default");
    }

    return getOrCreateMes(data.getMonthValue());
  }

  private CategoriaResponse toResponse(Categoria c) {
    return new CategoriaResponse(c.getId(), c.getNome());
  }
}
