package br.com.neia.divisaocontas.dto;

import java.math.BigDecimal;

public class LancamentoResponse {
  private Long id;
  private String descricao;
  private String data; // dd/MM/yyyy
  private BigDecimal valor;
  private String pagador;

  public LancamentoResponse(Long id, String descricao, String data,
      BigDecimal valor, String pagador) {
    this.id = id;
    this.descricao = descricao;
    this.data = data;
    this.valor = valor;
    this.pagador = pagador;
  }

  public Long getId() {
    return id;
  }

  public String getDescricao() {
    return descricao;
  }

  public String getData() {
    return data;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public String getPagador() {
    return pagador;
  }
}
