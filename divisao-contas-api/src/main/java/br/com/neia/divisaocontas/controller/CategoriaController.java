package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.CategoriaCreateRequest;
import br.com.neia.divisaocontas.dto.CategoriaResponse;
import br.com.neia.divisaocontas.dto.ErrorResponse;
import br.com.neia.divisaocontas.service.CategoriaService;
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
@RequestMapping("/api/categorias")
@Tag(name = "Categorias")
public class CategoriaController {

  private final CategoriaService categoriaService;

  public CategoriaController(CategoriaService categoriaService) {
    this.categoriaService = categoriaService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar categoria", description = "Cria uma categoria (ex: Contas Mês, Viagem Salvador, Mes/01). Se já existir, retorna a existente.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Criada", content = @Content(
        schema = @Schema(implementation = CategoriaResponse.class),
        examples = @ExampleObject(name = "Exemplo", value = "{\"id\": 1, \"nome\": \"Viagem Salvador\"}"))),
      @ApiResponse(responseCode = "400", description = "Validação", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
      schema = @Schema(implementation = CategoriaCreateRequest.class),
      examples = @ExampleObject(name = "Criar", value = "{\"nome\": \"Viagem Salvador\"}")))
    public CategoriaResponse criar(@RequestBody CategoriaCreateRequest req) {
    return categoriaService.criar(req);
  }

  @GetMapping
  @Operation(summary = "Listar categorias")
  public List<CategoriaResponse> listar() {
    return categoriaService.listar();
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar categoria", description = "Atualiza o nome de uma categoria.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Atualizada", content = @Content(
        schema = @Schema(implementation = CategoriaResponse.class),
        examples = @ExampleObject(name = "Exemplo", value = "{\"id\": 1, \"nome\": \"Contas Mês\"}"))),
      @ApiResponse(responseCode = "400", description = "Validação", content = @Content(
        schema = @Schema(implementation = ErrorResponse.class),
        examples = @ExampleObject(name = "Erro", value = "{\"dataHora\":\"21/01/2026 11:20:00\",\"status\":400,\"erro\":\"Bad Request\",\"mensagem\":\"nome é obrigatório\",\"path\":\"/api/categorias/1\"}"))),
      @ApiResponse(responseCode = "404", description = "Não encontrada", content = @Content(
        schema = @Schema(implementation = ErrorResponse.class),
        examples = @ExampleObject(name = "Erro", value = "{\"dataHora\":\"21/01/2026 11:20:00\",\"status\":404,\"erro\":\"Not Found\",\"mensagem\":\"Categoria não encontrada.\",\"path\":\"/api/categorias/999\"}"))),
      @ApiResponse(responseCode = "409", description = "Nome duplicado", content = @Content(
        schema = @Schema(implementation = ErrorResponse.class),
        examples = @ExampleObject(name = "Erro", value = "{\"dataHora\":\"21/01/2026 11:20:00\",\"status\":409,\"erro\":\"Conflict\",\"mensagem\":\"Já existe uma categoria com esse nome.\",\"path\":\"/api/categorias/1\"}")))
  })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
      schema = @Schema(implementation = CategoriaCreateRequest.class),
      examples = @ExampleObject(name = "Atualizar", value = "{\"nome\": \"Contas Mês\"}")))
  public CategoriaResponse atualizar(@PathVariable Long id, @RequestBody CategoriaCreateRequest req) {
    return categoriaService.atualizar(id, req);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Deletar categoria", description = "Remove uma categoria. Se estiver em uso por lançamentos/pagamentos/fechamentos, retorna 409.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Removida"),
      @ApiResponse(responseCode = "404", description = "Não encontrada", content = @Content(
          schema = @Schema(implementation = ErrorResponse.class),
          examples = @ExampleObject(name = "Erro", value = "{\"dataHora\":\"21/01/2026 11:20:00\",\"status\":404,\"erro\":\"Not Found\",\"mensagem\":\"Categoria não encontrada.\",\"path\":\"/api/categorias/999\"}"))),
      @ApiResponse(responseCode = "409", description = "Categoria em uso", content = @Content(
          schema = @Schema(implementation = ErrorResponse.class),
          examples = @ExampleObject(name = "Erro", value = "{\"dataHora\":\"21/01/2026 11:20:00\",\"status\":409,\"erro\":\"Conflict\",\"mensagem\":\"Categoria está em uso e não pode ser removida.\",\"path\":\"/api/categorias/1\"}")))
  })
  public void deletar(@PathVariable Long id) {
    categoriaService.deletar(id);
  }
}
