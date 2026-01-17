package br.com.neia.divisaocontas.dto;

import java.math.BigDecimal;

public class DevedorItemRequest {
  private Long pessoaId;
  private BigDecimal valor;

  public Long getPessoaId() {
    return pessoaId;
  }

  public void setPessoaId(Long pessoaId) {
    this.pessoaId = pessoaId;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public void setValor(BigDecimal valor) {
    this.valor = valor;
  }
}
