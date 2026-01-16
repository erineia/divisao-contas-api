package br.com.neia.divisaocontas.dto;

import java.math.BigDecimal;

public class PagamentoResponse {
  private Long id;
  private String data; // dd/MM/yyyy
  private BigDecimal valor;
  private String pagador;
  private String recebedor;
  private String observacao;

  public PagamentoResponse(Long id, String data, BigDecimal valor, String pagador, String recebedor,
      String observacao) {
    this.id = id;
    this.data = data;
    this.valor = valor;
    this.pagador = pagador;
    this.recebedor = recebedor;
    this.observacao = observacao;
  }

  public Long getId() {
    return id;
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

  public String getRecebedor() {
    return recebedor;
  }

  public String getObservacao() {
    return observacao;
  }
}
