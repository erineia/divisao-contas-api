package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.PagamentoCreateRequest;
import br.com.neia.divisaocontas.dto.PagamentoResponse;
import br.com.neia.divisaocontas.entity.Pagamento;
import br.com.neia.divisaocontas.entity.Pessoa;
import br.com.neia.divisaocontas.exception.NotFoundException;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import br.com.neia.divisaocontas.service.FechamentoMesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

  private final PagamentoRepository pagamentoRepository;
  private final PessoaRepository pessoaRepository;
  private final FechamentoMesService fechamentoMesService;

  public PagamentoController(PagamentoRepository pagamentoRepository,
      PessoaRepository pessoaRepository,
      FechamentoMesService fechamentoMesService) {
    this.pagamentoRepository = pagamentoRepository;
    this.pessoaRepository = pessoaRepository;
    this.fechamentoMesService = fechamentoMesService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PagamentoResponse criar(@RequestBody PagamentoCreateRequest req) {
    fechamentoMesService.validarAberto(req.getData());

    if (req.getValor() == null || req.getValor().signum() <= 0) {
      throw new IllegalArgumentException("valor deve ser maior que zero.");
    }
    if (req.getData() == null) {
      throw new IllegalArgumentException("data é obrigatória.");
    }
    if (req.getPagadorId() == null || req.getRecebedorId() == null) {
      throw new IllegalArgumentException("pagadorId e recebedorId são obrigatórios.");
    }
    if (req.getPagadorId().equals(req.getRecebedorId())) {
      throw new IllegalArgumentException("pagador e recebedor não podem ser a mesma pessoa.");
    }

    Pessoa pagador = pessoaRepository.findById(req.getPagadorId())
        .orElseThrow(() -> new NotFoundException("Pagador não encontrado."));
    Pessoa recebedor = pessoaRepository.findById(req.getRecebedorId())
        .orElseThrow(() -> new NotFoundException("Recebedor não encontrado."));

    Pagamento p = new Pagamento();
    p.setData(req.getData());
    p.setValor(req.getValor());
    p.setPagador(pagador);
    p.setRecebedor(recebedor);
    p.setObservacao(req.getObservacao());

    Pagamento salvo = pagamentoRepository.save(p);

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    return new PagamentoResponse(
        salvo.getId(),
        salvo.getData().format(fmt),
        salvo.getValor(),
        salvo.getPagador().getNome(),
        salvo.getRecebedor().getNome(),
        salvo.getObservacao());
  }

  @GetMapping("/periodo")
  public List<PagamentoResponse> listarPorPeriodo(@RequestParam String dataInicio,
      @RequestParam String dataFim) {

    LocalDate inicio = parseData(dataInicio);
    LocalDate fim = parseData(dataFim);

    if (fim.isBefore(inicio)) {
      throw new IllegalArgumentException("dataFim não pode ser menor que dataInicio.");
    }

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    return pagamentoRepository.findByDataBetween(inicio, fim)
        .stream()
        .map(p -> new PagamentoResponse(
            p.getId(),
            p.getData() != null ? p.getData().format(fmt) : null,
            p.getValor(),
            p.getPagador() != null ? p.getPagador().getNome() : null,
            p.getRecebedor() != null ? p.getRecebedor().getNome() : null,
            p.getObservacao()))
        .toList();
  }

  private LocalDate parseData(String s) {
    if (s == null || s.isBlank()) {
      throw new IllegalArgumentException("Data é obrigatória.");
    }

    // ISO: 2026-01-16
    try {
      return LocalDate.parse(s);
    } catch (DateTimeParseException ignored) {
    }

    // BR: 16/01/2026
    try {
      DateTimeFormatter br = DateTimeFormatter.ofPattern("dd/MM/yyyy");
      return LocalDate.parse(s, br);
    } catch (DateTimeParseException ignored) {
    }

    throw new IllegalArgumentException("Formato de data inválido. Use yyyy-MM-dd ou dd/MM/yyyy.");
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody PagamentoCreateRequest req) {
    Pagamento pagamento = pagamentoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Pagamento não encontrado."));

    if (req.getValor() == null || req.getValor().signum() <= 0) {
      throw new IllegalArgumentException("valor deve ser maior que zero.");
    }
    if (req.getData() == null) {
      throw new IllegalArgumentException("data é obrigatória.");
    }
    if (req.getPagadorId() == null || req.getRecebedorId() == null) {
      throw new IllegalArgumentException("pagadorId e recebedorId são obrigatórios.");
    }
    if (req.getPagadorId().equals(req.getRecebedorId())) {
      throw new IllegalArgumentException("pagador e recebedor não podem ser a mesma pessoa.");
    }

    // trava por mês fechado (data antiga e nova)
    fechamentoMesService.validarAberto(pagamento.getData());
    fechamentoMesService.validarAberto(req.getData());

    Pessoa pagador = pessoaRepository.findById(req.getPagadorId())
        .orElseThrow(() -> new NotFoundException("Pagador não encontrado."));
    Pessoa recebedor = pessoaRepository.findById(req.getRecebedorId())
        .orElseThrow(() -> new NotFoundException("Recebedor não encontrado."));

    pagamento.setData(req.getData());
    pagamento.setValor(req.getValor());
    pagamento.setPagador(pagador);
    pagamento.setRecebedor(recebedor);
    pagamento.setObservacao(req.getObservacao());

    Pagamento salvo = pagamentoRepository.save(pagamento);

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    return ResponseEntity.ok(new PagamentoResponse(
        salvo.getId(),
        salvo.getData() != null ? salvo.getData().format(fmt) : null,
        salvo.getValor(),
        salvo.getPagador() != null ? salvo.getPagador().getNome() : null,
        salvo.getRecebedor() != null ? salvo.getRecebedor().getNome() : null,
        salvo.getObservacao()));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<Map<String, String>> deletar(@PathVariable Long id) {

    Pagamento p = pagamentoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Pagamento não encontrado."));

    // trava por mês fechado (pela data do pagamento)
    fechamentoMesService.validarAberto(p.getData());

    pagamentoRepository.deleteById(id);

    return ResponseEntity.ok(Map.of("mensagem", "Pagamento excluído com sucesso!"));
  }

}
