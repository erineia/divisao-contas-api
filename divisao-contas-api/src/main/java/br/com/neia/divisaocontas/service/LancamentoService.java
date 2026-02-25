package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.dto.*;
import br.com.neia.divisaocontas.entity.*;
import br.com.neia.divisaocontas.exception.DuplicateException;
import br.com.neia.divisaocontas.exception.NotFoundException;
import br.com.neia.divisaocontas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LancamentoService {

  private static final Logger log = LoggerFactory.getLogger(LancamentoService.class);

  private final LancamentoRepository lancamentoRepository;
  private final PessoaRepository pessoaRepository;
  private final LancamentoRateioRepository rateioRepository;
  private final FechamentoMesService fechamentoMesService;
  private final GrupoService grupoService;

  public LancamentoService(LancamentoRepository lancamentoRepository,
      PessoaRepository pessoaRepository,
      LancamentoRateioRepository rateioRepository,
      FechamentoMesService fechamentoMesService,
      GrupoService grupoService) {
    this.lancamentoRepository = lancamentoRepository;
    this.pessoaRepository = pessoaRepository;
    this.rateioRepository = rateioRepository;
    this.fechamentoMesService = fechamentoMesService;
    this.grupoService = grupoService;
  }

  @Transactional
  public LancamentoResponse criar(LancamentoCreateRequest req) {
    validarCamposBasicos(req);

    Grupo grupo = grupoService.resolveGrupo(req.getGrupoId(), req.getData());
    fechamentoMesService.validarAberto(req.getData(), grupo.getId());

    Pessoa pagador = pessoaRepository.findById(req.getPagadorId())
        .orElseThrow(() -> new IllegalArgumentException("Pagador não encontrado"));

    boolean existe = lancamentoRepository.existsByDescricaoAndDataAndValorAndPagadorAndGrupo(
        req.getDescricao(), req.getData(), req.getValor(), pagador, grupo);
    if (existe)
      throw new DuplicateException("Lançamento já existe.");

    Lancamento l = new Lancamento();
    l.setDescricao(req.getDescricao());
    l.setData(req.getData());
    l.setValor(req.getValor());
    l.setPagador(pagador);
    l.setGrupo(grupo);
    // se você tiver coluna divide no entity, aqui é o lugar:
    // l.setDivide(req.isDivide());

    Lancamento salvo = lancamentoRepository.save(l);

    gravarRateiosConformeRegra(salvo, req);

    return toResponse(salvo, req.isDivide());
  }

  @Transactional
  public LancamentoResponse atualizar(Long id, LancamentoCreateRequest req) {
    log.info("Atualizar lancamento id={} pagadorId={} participantesIds={}", id, req.getPagadorId(),
        req.getParticipantesIds());
    Lancamento existente = lancamentoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Lançamento não encontrado."));

    Grupo grupoAntigo = existente.getGrupo();
    if (grupoAntigo == null) {
      grupoAntigo = grupoService.resolveGrupo(null, existente.getData());
      existente.setGrupo(grupoAntigo);
    }

    Grupo grupoNovo = grupoService.resolveGrupo(req.getGrupoId(), req.getData());

    // bloqueia tanto mês antigo quanto novo mês
    fechamentoMesService.validarAberto(existente.getData(), grupoAntigo.getId());
    validarCamposBasicos(req);
    fechamentoMesService.validarAberto(req.getData(), grupoNovo.getId());

    Pessoa pagador = pessoaRepository.findById(req.getPagadorId())
        .orElseThrow(() -> new IllegalArgumentException("Pagador não encontrado"));

    boolean duplicado = lancamentoRepository.existsByDescricaoAndDataAndValorAndPagadorAndGrupoAndIdNot(
        req.getDescricao(), req.getData(), req.getValor(), pagador, grupoNovo, id);
    if (duplicado)
      throw new DuplicateException("Lançamento já existe.");

    existente.setDescricao(req.getDescricao());
    existente.setData(req.getData());
    existente.setValor(req.getValor());
    existente.setPagador(pagador);
    existente.setGrupo(grupoNovo);
    // se tiver coluna divide:
    // existente.setDivide(req.isDivide());

    Lancamento salvo = lancamentoRepository.save(existente);

    rateioRepository.deleteByLancamentoId(id);
    gravarRateiosConformeRegra(salvo, req);

    // Garantia adicional: se o cliente solicitou divisão e NÃO incluiu o pagador
    // entre os participantes, removemos qualquer rateio residual do pagador.
    if (req.isDivide()) {
      List<Long> ids = req.getParticipantesIds();
      if (ids != null && !ids.contains(salvo.getPagador().getId())) {
        rateioRepository.deleteByLancamentoIdAndPessoaId(salvo.getId(), salvo.getPagador().getId());
      }
    }

    return toResponse(salvo, req.isDivide());
  }

  @Transactional
  public void deletar(Long id) {
    Lancamento l = lancamentoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Lançamento não encontrado."));

    Grupo grupoAtual = l.getGrupo();
    if (grupoAtual == null) {
      grupoAtual = grupoService.resolveGrupo(null, l.getData());
      l.setGrupo(grupoAtual);
    }

    fechamentoMesService.validarAberto(l.getData(), grupoAtual.getId());

    rateioRepository.deleteByLancamentoId(id);
    lancamentoRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<LancamentoResponse> listar() {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    return lancamentoRepository.findAll().stream()
        .map(l -> {
          List<LancamentoRateio> rateios = rateioRepository.findByLancamentoId(l.getId());

          LancamentoResponse resp = new LancamentoResponse();
          resp.setId(l.getId());
          resp.setDescricao(l.getDescricao());
          resp.setData(l.getData().format(fmt));
          resp.setValor(l.getValor());
          if (l.getGrupo() != null) {
            resp.setGrupoId(l.getGrupo().getId());
            resp.setGrupoNome(l.getGrupo().getNome());
          }
          // Se houver rateios, preenche ambos participantes/devedores para o cliente.
          // Nota: a regra de divisão agora respeita `participantesIds` enviados pelo
          // cliente;
          // não adicionamos mais o pagador automaticamente.
          resp.setDivide(!rateios.isEmpty());
          resp.setPagador(new PessoaResponse(l.getPagador().getId(), l.getPagador().getNome()));
          resp.setParticipantes(rateios.stream()
              .map(r -> new LancamentoPessoaValorResponse(r.getPessoa().getId(), r.getPessoa().getNome(),
                  r.getValorDevido()))
              .toList());
          resp.setDevedores(rateios.stream()
              .map(r -> new LancamentoPessoaValorResponse(r.getPessoa().getId(), r.getPessoa().getNome(),
                  r.getValorDevido()))
              .toList());

          return resp;
        })
        .toList();
  }

  // ===== helpers =====

  private void validarCamposBasicos(LancamentoCreateRequest req) {
    if (req.getDescricao() == null || req.getDescricao().trim().isEmpty())
      throw new IllegalArgumentException("descricao é obrigatória");
    if (req.getData() == null)
      throw new IllegalArgumentException("data é obrigatória");
    if (req.getValor() == null || req.getValor().signum() <= 0)
      throw new IllegalArgumentException("valor deve ser maior que zero.");
    if (req.getPagadorId() == null)
      throw new IllegalArgumentException("pagadorId é obrigatório");
  }

  private void gravarRateiosConformeRegra(Lancamento salvo, LancamentoCreateRequest req) {
    if (req.isDivide()) {
      List<Long> ids = req.getParticipantesIds();

      if (ids == null || ids.isEmpty())
        throw new IllegalArgumentException("Para divide=true, informe participantesIds.");
      if (ids.stream().anyMatch(Objects::isNull))
        throw new IllegalArgumentException("participantesIds não pode conter null.");

      Set<Long> uniq = new HashSet<>(ids);
      ids = new ArrayList<>(uniq);

      // Não adicionar automaticamente o pagador aos participantes.
      // A divisão será feita apenas entre os `participantesIds` informados pelo
      // usuário.

      List<Pessoa> participantes = pessoaRepository.findAllById(ids);
      if (participantes.size() != ids.size())
        throw new IllegalArgumentException("Um ou mais participantes não foram encontrados.");

      participantes.sort(Comparator.comparing(Pessoa::getId));

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

      if (itens == null || itens.isEmpty())
        throw new IllegalArgumentException("Para divide=false, informe a lista de devedores.");

      BigDecimal soma = BigDecimal.ZERO;
      Set<Long> uniqDevedores = new HashSet<>();

      for (DevedorItemRequest item : itens) {
        if (item.getPessoaId() == null || item.getValor() == null)
          throw new IllegalArgumentException("Cada devedor deve ter pessoaId e valor.");
        if (item.getValor().compareTo(BigDecimal.ZERO) <= 0)
          throw new IllegalArgumentException("Valor do devedor deve ser maior que zero.");
        if (item.getPessoaId().equals(req.getPagadorId()))
          throw new IllegalArgumentException("O pagador não pode ser devedor.");
        if (!uniqDevedores.add(item.getPessoaId()))
          throw new IllegalArgumentException("Devedor repetido: pessoaId=" + item.getPessoaId());

        Pessoa devedor = pessoaRepository.findById(item.getPessoaId())
            .orElseThrow(() -> new IllegalArgumentException("Devedor não encontrado: pessoaId=" + item.getPessoaId()));

        LancamentoRateio r = new LancamentoRateio();
        r.setLancamento(salvo);
        r.setPessoa(devedor);
        r.setValorDevido(item.getValor());
        rateioRepository.save(r);

        soma = soma.add(item.getValor());
      }

      if (soma.compareTo(salvo.getValor()) != 0)
        throw new IllegalArgumentException("A soma dos devedores deve ser igual ao valor do lançamento.");
    }
  }

  private LancamentoResponse toResponse(Lancamento salvo, boolean divide) {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    LancamentoResponse resp = new LancamentoResponse();
    resp.setId(salvo.getId());
    resp.setDescricao(salvo.getDescricao());
    resp.setData(salvo.getData().format(fmt));
    resp.setValor(salvo.getValor());
    if (salvo.getGrupo() != null) {
      resp.setGrupoId(salvo.getGrupo().getId());
      resp.setGrupoNome(salvo.getGrupo().getNome());
    }
    resp.setDivide(divide);
    resp.setPagador(new PessoaResponse(salvo.getPagador().getId(), salvo.getPagador().getNome()));

    List<LancamentoRateio> rateios = rateioRepository.findByLancamentoId(salvo.getId());

    if (divide) {
      resp.setParticipantes(rateios.stream()
          .map(r -> new LancamentoPessoaValorResponse(r.getPessoa().getId(), r.getPessoa().getNome(),
              r.getValorDevido()))
          .toList());
    } else {
      resp.setDevedores(rateios.stream()
          .map(r -> new LancamentoPessoaValorResponse(r.getPessoa().getId(), r.getPessoa().getNome(),
              r.getValorDevido()))
          .toList());
    }
    return resp;
  }
}
