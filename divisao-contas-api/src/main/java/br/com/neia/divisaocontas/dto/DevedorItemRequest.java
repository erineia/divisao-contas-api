package br.com.neia.divisaocontas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public class DevedorItemRequest {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Long pessoaId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
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
