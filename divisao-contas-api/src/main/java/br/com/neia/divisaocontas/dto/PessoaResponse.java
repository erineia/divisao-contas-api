package br.com.neia.divisaocontas.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({ "id", "nome" })
public class PessoaResponse {
  private Long id;

  private String nome;

  public PessoaResponse(Long id, String nome) {
    this.id = id;
    this.nome = nome;
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }
}
