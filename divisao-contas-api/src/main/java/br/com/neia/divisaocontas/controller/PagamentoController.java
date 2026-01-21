package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.ErrorResponse;
import br.com.neia.divisaocontas.dto.PagamentoCreateRequest;
import br.com.neia.divisaocontas.dto.PagamentoResponse;
import br.com.neia.divisaocontas.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pagamentos")
@Tag(name = "Pagamentos")
public class PagamentoController {

  private final PagamentoService pagamentoService;

  public PagamentoController(PagamentoService pagamentoService) {
    this.pagamentoService = pagamentoService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar pagamento", description = "Registra um pagamento (acerto) do pagador para o recebedor.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Pagamento criado"),
      @ApiResponse(responseCode = "400", description = "Validação/regra de negócio", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Pessoa não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public PagamentoResponse criar(@RequestBody PagamentoCreateRequest req) {
    return pagamentoService.criar(req);
  }

  @GetMapping
  @Operation(summary = "Listar pagamentos")
  public List<PagamentoResponse> listar() {
    return pagamentoService.listar();
  }

  @GetMapping("/periodo")
  @Operation(summary = "Listar pagamentos por período", description = "Filtra pagamentos entre dataInicio e dataFim (yyyy-MM-dd).")
  public List<PagamentoResponse> listarPorPeriodo(@RequestParam String dataInicio,
      @RequestParam String dataFim) {
    // reaproveita seu parseData (você já tem em algum lugar)
    LocalDate ini = parseData(dataInicio);
    LocalDate fim = parseData(dataFim);
    return pagamentoService.listarPorPeriodo(ini, fim);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar pagamento")
  public ResponseEntity<PagamentoResponse> atualizar(@PathVariable Long id,
      @RequestBody PagamentoCreateRequest req) {
    return ResponseEntity.ok(pagamentoService.atualizar(id, req));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Excluir pagamento")
  public ResponseEntity<Void> deletar(@PathVariable Long id) {
    pagamentoService.deletar(id);
    return ResponseEntity.noContent().build();
  }

  private LocalDate parseData(String raw) {
    try {
      return LocalDate.parse(raw);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Data inválida (use yyyy-MM-dd): " + raw);
    }
  }
}
