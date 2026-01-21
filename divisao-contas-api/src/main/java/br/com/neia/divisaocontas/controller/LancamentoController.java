package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.ErrorResponse;
import br.com.neia.divisaocontas.dto.LancamentoCreateRequest;
import br.com.neia.divisaocontas.dto.LancamentoResponse;
import br.com.neia.divisaocontas.service.LancamentoService;
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

@RestController
@RequestMapping("/api/lancamentos")
@Tag(name = "Lançamentos")
public class LancamentoController {

  private final LancamentoService lancamentoService;

  public LancamentoController(LancamentoService lancamentoService) {
    this.lancamentoService = lancamentoService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar lançamento", description = "Cria um lançamento (despesa) para um pagador.\n\nRegras:\n- Se divide=true: informe participantesIds; o valor será dividido igualmente entre participantes (pagador incluído).\n- Se divide=false: informe devedores com valores; a soma deve ser igual ao valor e o pagador não pode ser devedor.\n- Se o mês da data estiver fechado, a API retorna 400.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Lançamento criado"),
      @ApiResponse(responseCode = "400", description = "Validação/regra de negócio", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "Lançamento duplicado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = LancamentoCreateRequest.class)))
  public LancamentoResponse criar(@RequestBody LancamentoCreateRequest req) {
    return lancamentoService.criar(req);
  }

  @GetMapping
  @Operation(summary = "Listar lançamentos")
  public List<LancamentoResponse> listar() {
    return lancamentoService.listar();
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar lançamento", description = "Atualiza o lançamento e recalcula seus rateios. Bloqueia tanto o mês original quanto o mês da nova data caso estejam fechados.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lançamento atualizado"),
      @ApiResponse(responseCode = "400", description = "Validação/regra de negócio", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "Duplicado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<LancamentoResponse> atualizar(@PathVariable Long id,
      @RequestBody LancamentoCreateRequest req) {
    return ResponseEntity.ok(lancamentoService.atualizar(id, req));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Excluir lançamento", description = "Exclui o lançamento e seus rateios. Se o mês estiver fechado, retorna 400.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Excluído"),
      @ApiResponse(responseCode = "400", description = "Mês fechado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<Void> deletar(@PathVariable Long id) {
    lancamentoService.deletar(id);
    return ResponseEntity.noContent().build();
  }
}
