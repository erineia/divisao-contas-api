package br.com.neia.divisaocontas.dto;

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
