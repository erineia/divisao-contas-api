package br.com.neia.divisaocontas.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

@JsonPropertyOrder({ "pessoaId", "nome", "valor" })
public class LancamentoPessoaValorResponse {
  private Long pessoaId;
  private String nome;
  private BigDecimal valor;

  public LancamentoPessoaValorResponse() {
  }

  public LancamentoPessoaValorResponse(Long pessoaId, String nome, BigDecimal valor) {
    this.pessoaId = pessoaId;
    this.nome = nome;
    this.valor = valor;
  }

  public Long getPessoaId() {
    return pessoaId;
  }

  public void setPessoaId(Long pessoaId) {
    this.pessoaId = pessoaId;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public void setValor(BigDecimal valor) {
    this.valor = valor;
  }
}
