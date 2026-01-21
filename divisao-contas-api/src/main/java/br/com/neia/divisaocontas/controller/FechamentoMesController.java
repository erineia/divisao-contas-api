package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.ErrorResponse;
import br.com.neia.divisaocontas.dto.FechamentoMesResponse;
import br.com.neia.divisaocontas.service.FechamentoMesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fechamentos")
@Tag(name = "Fechamentos")
public class FechamentoMesController {

  private final FechamentoMesService fechamentoMesService;

  public FechamentoMesController(FechamentoMesService fechamentoMesService) {
    this.fechamentoMesService = fechamentoMesService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Fechar mês", description = "Fecha um mês (ano/mes). Enquanto fechado, não é permitido criar/alterar/excluir lançamentos com data nesse mês.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Mês fechado"),
      @ApiResponse(responseCode = "400", description = "Ano/mês inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "Mês já fechado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public FechamentoMesResponse fechar(@RequestParam int ano,
      @RequestParam int mes,
      @RequestParam(required = false) Long categoriaId,
      @RequestParam(required = false) String observacao) {
    return fechamentoMesService.fechar(ano, mes, categoriaId, observacao);
  }

  @GetMapping
  @Operation(summary = "Listar meses fechados")
  public List<FechamentoMesResponse> listar(@RequestParam(required = false) Long categoriaId) {
    return fechamentoMesService.listar(categoriaId);
  }

  // Opcional (bom para DEV): reabrir mês
  @DeleteMapping
  @Operation(summary = "Reabrir mês", description = "Reabre um mês fechado (bom para DEV).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Reaberto"),
      @ApiResponse(responseCode = "400", description = "Ano/mês inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Fechamento não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<Map<String, String>> reabrir(@RequestParam int ano, @RequestParam int mes,
      @RequestParam(required = false) Long categoriaId) {
    fechamentoMesService.reabrir(ano, mes, categoriaId);

    return ResponseEntity.ok(Map.of("mensagem", "Mês reaberto com sucesso!"));
  }
}
