package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.dto.PagamentoCreateRequest;
import br.com.neia.divisaocontas.dto.PagamentoResponse;
import br.com.neia.divisaocontas.entity.Pagamento;
import br.com.neia.divisaocontas.entity.Pessoa;
import br.com.neia.divisaocontas.exception.NotFoundException;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PagamentoService {

  private final PagamentoRepository pagamentoRepository;
  private final PessoaRepository pessoaRepository;
  private final FechamentoMesService fechamentoMesService;
  private final CategoriaService categoriaService;

  public PagamentoService(PagamentoRepository pagamentoRepository,
      PessoaRepository pessoaRepository,
      FechamentoMesService fechamentoMesService,
      CategoriaService categoriaService) {
    this.pagamentoRepository = pagamentoRepository;
    this.pessoaRepository = pessoaRepository;
    this.fechamentoMesService = fechamentoMesService;
    this.categoriaService = categoriaService;
  }

  @Transactional
  public PagamentoResponse criar(PagamentoCreateRequest req) {
    validar(req);

    var categoria = categoriaService.resolveCategoria(req.getCategoriaId(), req.getData());
    fechamentoMesService.validarAberto(req.getData(), categoria.getId());

    Pessoa pagador = pessoaRepository.findById(req.getPagadorId())
        .orElseThrow(() -> new IllegalArgumentException("Pagador não encontrado"));

    Pessoa recebedor = pessoaRepository.findById(req.getRecebedorId())
        .orElseThrow(() -> new IllegalArgumentException("Recebedor não encontrado"));

    if (pagador.getId().equals(recebedor.getId())) {
      throw new IllegalArgumentException("Pagador e recebedor não podem ser a mesma pessoa.");
    }

    Pagamento p = new Pagamento();
    p.setData(req.getData());
    p.setValor(req.getValor());
    p.setPagador(pagador);
    p.setRecebedor(recebedor);
    p.setObservacao(req.getObservacao());
    p.setCategoria(categoria);

    Pagamento salvo = pagamentoRepository.save(p);

    return toResponse(salvo);
  }

  @Transactional(readOnly = true)
  public List<PagamentoResponse> listar() {
    return pagamentoRepository.findAll().stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PagamentoResponse> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
    if (fim.isBefore(inicio)) {
      throw new IllegalArgumentException("dataFim não pode ser menor que dataInicio.");
    }

    return pagamentoRepository.findByDataBetween(inicio, fim).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public PagamentoResponse atualizar(Long id, PagamentoCreateRequest req) {
    Pagamento existente = pagamentoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Pagamento não encontrado."));

    var categoriaAntiga = existente.getCategoria();
    if (categoriaAntiga == null) {
      categoriaAntiga = categoriaService.resolveCategoria(null, existente.getData());
      existente.setCategoria(categoriaAntiga);
    }

    var categoriaNova = categoriaService.resolveCategoria(req.getCategoriaId(), req.getData());

    // trava tanto mês antigo quanto novo
    fechamentoMesService.validarAberto(existente.getData(), categoriaAntiga.getId());

    validar(req);
    fechamentoMesService.validarAberto(req.getData(), categoriaNova.getId());

    Pessoa pagador = pessoaRepository.findById(req.getPagadorId())
        .orElseThrow(() -> new IllegalArgumentException("Pagador não encontrado"));

    Pessoa recebedor = pessoaRepository.findById(req.getRecebedorId())
        .orElseThrow(() -> new IllegalArgumentException("Recebedor não encontrado"));

    if (pagador.getId().equals(recebedor.getId())) {
      throw new IllegalArgumentException("Pagador e recebedor não podem ser a mesma pessoa.");
    }

    existente.setData(req.getData());
    existente.setValor(req.getValor());
    existente.setPagador(pagador);
    existente.setRecebedor(recebedor);
    existente.setObservacao(req.getObservacao());
    existente.setCategoria(categoriaNova);

    Pagamento salvo = pagamentoRepository.save(existente);
    return toResponse(salvo);
  }

  @Transactional
  public void deletar(Long id) {
    Pagamento p = pagamentoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Pagamento não encontrado."));

    var categoria = p.getCategoria();
    if (categoria == null) {
      categoria = categoriaService.resolveCategoria(null, p.getData());
      p.setCategoria(categoria);
    }

    fechamentoMesService.validarAberto(p.getData(), categoria.getId());

    pagamentoRepository.deleteById(id);
  }

  private void validar(PagamentoCreateRequest req) {
    if (req.getData() == null)
      throw new IllegalArgumentException("data é obrigatória");
    if (req.getValor() == null || req.getValor().signum() <= 0)
      throw new IllegalArgumentException("valor deve ser maior que zero.");
    if (req.getPagadorId() == null)
      throw new IllegalArgumentException("pagadorId é obrigatório");
    if (req.getRecebedorId() == null)
      throw new IllegalArgumentException("recebedorId é obrigatório");
  }

  private PagamentoResponse toResponse(Pagamento p) {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    Long categoriaId = p.getCategoria() == null ? null : p.getCategoria().getId();
    String categoriaNome = p.getCategoria() == null ? null : p.getCategoria().getNome();

    return new PagamentoResponse(
        p.getId(),
        p.getData().format(fmt),
        p.getValor(),
        p.getPagador().getNome(),
        p.getRecebedor().getNome(),
        p.getObservacao(),
        categoriaId,
        categoriaNome);
  }
}
