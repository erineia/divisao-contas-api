package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.dto.GrupoCreateRequest;
import br.com.neia.divisaocontas.dto.GrupoResponse;
import br.com.neia.divisaocontas.entity.Grupo;
import br.com.neia.divisaocontas.exception.DuplicateException;
import br.com.neia.divisaocontas.exception.NotFoundException;
import br.com.neia.divisaocontas.repository.GrupoRepository;
import br.com.neia.divisaocontas.repository.FechamentoMesRepository;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class GrupoService {

  private final GrupoRepository grupoRepository;
  private final LancamentoRepository lancamentoRepository;
  private final PagamentoRepository pagamentoRepository;
  private final FechamentoMesRepository fechamentoMesRepository;

  public GrupoService(
      GrupoRepository grupoRepository,
      LancamentoRepository lancamentoRepository,
      PagamentoRepository pagamentoRepository,
      FechamentoMesRepository fechamentoMesRepository) {
    this.grupoRepository = grupoRepository;
    this.lancamentoRepository = lancamentoRepository;
    this.pagamentoRepository = pagamentoRepository;
    this.fechamentoMesRepository = fechamentoMesRepository;
  }

  @Transactional
  public GrupoResponse criar(GrupoCreateRequest req) {
    if (req == null || req.getNome() == null || req.getNome().trim().isEmpty()) {
      throw new IllegalArgumentException("nome é obrigatório");
    }

    String nome = req.getNome().trim();
    Grupo grupo = getOrCreateByNome(nome);
    return toResponse(grupo);
  }

  @Transactional(readOnly = true)
  public List<GrupoResponse> listar() {
    return grupoRepository.findAll().stream()
        .map(this::toResponse)
        .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
        .toList();
  }

  @Transactional(readOnly = true)
  public Grupo buscarPorId(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("grupoId é obrigatório");
    }

    return grupoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Grupo não encontrado."));
  }

  @Transactional
  public GrupoResponse atualizar(Long id, GrupoCreateRequest req) {
    if (id == null) {
      throw new IllegalArgumentException("id é obrigatório");
    }
    if (req == null || req.getNome() == null || req.getNome().trim().isEmpty()) {
      throw new IllegalArgumentException("nome é obrigatório");
    }

    Grupo grupo = buscarPorId(id);
    String novoNome = req.getNome().trim();

    if (grupo.getNome() != null && grupo.getNome().equalsIgnoreCase(novoNome)) {
      // nada a fazer
      return toResponse(grupo);
    }

    grupoRepository.findByNome(novoNome)
        .filter(c -> !c.getId().equals(id))
        .ifPresent(c -> {
          throw new DuplicateException("Já existe um grupo com esse nome.");
        });

    grupo.setNome(novoNome);
    Grupo salvo = grupoRepository.save(grupo);
    return toResponse(salvo);
  }

  @Transactional
  public void deletar(Long id) {
    Grupo grupo = buscarPorId(id);

    boolean emUso = lancamentoRepository.existsByGrupoId(grupo.getId())
        || pagamentoRepository.existsByGrupoId(grupo.getId())
        || fechamentoMesRepository.existsByGrupoId(grupo.getId());

    if (emUso) {
      throw new DuplicateException("Grupo está em uso e não pode ser removido.");
    }

    grupoRepository.delete(grupo);
  }

  @Transactional
  public Grupo getOrCreateByNome(String nome) {
    if (nome == null || nome.trim().isEmpty()) {
      throw new IllegalArgumentException("nome é obrigatório");
    }

    String trimmed = nome.trim();

    return grupoRepository.findByNome(trimmed)
        .orElseGet(() -> grupoRepository.save(new Grupo(trimmed)));
  }

  @Transactional
  public Grupo getOrCreateMes(int mes) {
    if (mes < 1 || mes > 12) {
      throw new IllegalArgumentException("Mês deve ser entre 1 e 12.");
    }

    String nome = String.format("Mes/%02d", mes);
    return getOrCreateByNome(nome);
  }

  @Transactional
  public Grupo resolveGrupo(Long grupoId, LocalDate data) {
    if (grupoId != null) {
      return buscarPorId(grupoId);
    }

    if (data == null) {
      throw new IllegalArgumentException("data é obrigatória para resolver grupo default");
    }

    return getOrCreateMes(data.getMonthValue());
  }

  private GrupoResponse toResponse(Grupo g) {
    return new GrupoResponse(g.getId(), g.getNome());
  }
}
