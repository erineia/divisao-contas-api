package br.com.neia.divisaocontas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class CategoriaCreateRequest {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Viagem Salvador")
  private String nome;

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }
}
