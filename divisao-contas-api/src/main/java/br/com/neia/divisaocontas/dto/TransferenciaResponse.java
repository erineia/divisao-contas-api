package br.com.neia.divisaocontas.dto;

import java.math.BigDecimal;

public class TransferenciaResponse {
  private String devedor;
  private String credor;
  private BigDecimal valor;

  public TransferenciaResponse(String devedor, String credor, BigDecimal valor) {
    this.devedor = devedor;
    this.credor = credor;
    this.valor = valor;
  }

  public String getDevedor() {
    return devedor;
  }

  public String getCredor() {
    return credor;
  }

  public BigDecimal getValor() {
    return valor;
  }
}
