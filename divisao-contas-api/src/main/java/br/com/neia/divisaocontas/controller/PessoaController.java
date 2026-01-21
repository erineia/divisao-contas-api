package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.ErrorResponse;
import br.com.neia.divisaocontas.dto.PessoaCreateRequest;
import br.com.neia.divisaocontas.dto.PessoaResponse;
import br.com.neia.divisaocontas.service.PessoaService;
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
@RequestMapping("/api/pessoas")
@Tag(name = "Pessoas")
public class PessoaController {

  private final PessoaService pessoaService;

  public PessoaController(PessoaService pessoaService) {
    this.pessoaService = pessoaService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar pessoa")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Pessoa criada"),
      @ApiResponse(responseCode = "409", description = "Nome duplicado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public PessoaResponse criar(@RequestBody PessoaCreateRequest req) {
    return pessoaService.criar(req);
  }

  @GetMapping
  @Operation(summary = "Listar pessoas")
  public List<PessoaResponse> listar() {
    return pessoaService.listar();
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar pessoa")
  public ResponseEntity<PessoaResponse> atualizar(@PathVariable Long id, @RequestBody PessoaCreateRequest req) {
    return ResponseEntity.ok(pessoaService.atualizar(id, req));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Excluir pessoa")
  public ResponseEntity<Map<String, String>> deletar(@PathVariable Long id) {
    pessoaService.deletar(id);
    return ResponseEntity.ok(Map.of("mensagem", "Pessoa excluída com sucesso!"));
  }

}
