package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.GrupoCreateRequest;
import br.com.neia.divisaocontas.dto.GrupoResponse;
import br.com.neia.divisaocontas.dto.ErrorResponse;
import br.com.neia.divisaocontas.service.GrupoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos")
@Tag(name = "Grupos")
public class GrupoController {

  private final GrupoService grupoService;

  public GrupoController(GrupoService grupoService) {
    this.grupoService = grupoService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar grupo", description = "Cria um grupo (ex: Contas Mês, Viagem Salvador, Mes/01). Se já existir, retorna o existente.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Criado", content = @Content(schema = @Schema(implementation = GrupoResponse.class), examples = @ExampleObject(name = "Exemplo", value = "{\"id\": 1, \"nome\": \"Viagem Salvador\"}"))),
      @ApiResponse(responseCode = "400", description = "Validação", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = GrupoCreateRequest.class), examples = @ExampleObject(name = "Criar", value = "{\"nome\": \"Viagem Salvador\"}")))
  public GrupoResponse criar(@RequestBody GrupoCreateRequest req) {
    return grupoService.criar(req);
  }

  @GetMapping
  @Operation(summary = "Listar grupos")
  public List<GrupoResponse> listar() {
    return grupoService.listar();
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar grupo", description = "Atualiza o nome de um grupo.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Atualizado", content = @Content(schema = @Schema(implementation = GrupoResponse.class), examples = @ExampleObject(name = "Exemplo", value = "{\"id\": 1, \"nome\": \"Contas Mês\"}"))),
      @ApiResponse(responseCode = "400", description = "Validação", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Erro", value = "{\"dataHora\":\"21/01/2026 11:20:00\",\"status\":400,\"erro\":\"Bad Request\",\"mensagem\":\"nome é obrigatório\",\"path\":\"/api/grupos/1\"}"))),
      @ApiResponse(responseCode = "404", description = "Não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Erro", value = "{\"dataHora\":\"21/01/2026 11:20:00\",\"status\":404,\"erro\":\"Not Found\",\"mensagem\":\"Grupo não encontrado.\",\"path\":\"/api/grupos/999\"}"))),
      @ApiResponse(responseCode = "409", description = "Nome duplicado", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Erro", value = "{\"dataHora\":\"21/01/2026 11:20:00\",\"status\":409,\"erro\":\"Conflict\",\"mensagem\":\"Já existe um grupo com esse nome.\",\"path\":\"/api/grupos/1\"}")))
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = GrupoCreateRequest.class), examples = @ExampleObject(name = "Atualizar", value = "{\"nome\": \"Contas Mês\"}")))
  public GrupoResponse atualizar(@PathVariable Long id, @RequestBody GrupoCreateRequest req) {
    return grupoService.atualizar(id, req);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Deletar grupo", description = "Remove um grupo. Se estiver em uso por lançamentos/pagamentos/fechamentos, retorna 409.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Removida"),
      @ApiResponse(responseCode = "404", description = "Não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Erro", value = "{\"dataHora\":\"21/01/2026 11:20:00\",\"status\":404,\"erro\":\"Not Found\",\"mensagem\":\"Grupo não encontrado.\",\"path\":\"/api/grupos/999\"}"))),
      @ApiResponse(responseCode = "409", description = "Grupo em uso", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Erro", value = "{\"dataHora\":\"21/01/2026 11:20:00\",\"status\":409,\"erro\":\"Conflict\",\"mensagem\":\"Grupo está em uso e não pode ser removido.\",\"path\":\"/api/grupos/1\"}")))
  })
  public void deletar(@PathVariable Long id) {
    grupoService.deletar(id);
  }
}
