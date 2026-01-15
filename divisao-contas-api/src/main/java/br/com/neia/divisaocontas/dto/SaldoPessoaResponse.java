package br.com.neia.divisaocontas.dto;

import java.math.BigDecimal;

public class SaldoPessoaResponse {

  private Long pessoaId;
  private String nome;
  private BigDecimal totalPago;
  private BigDecimal valorDevido;
  private BigDecimal totalAReceber;

  public SaldoPessoaResponse(Long pessoaId, String nome,
      BigDecimal totalPago, BigDecimal valorDevido, BigDecimal totalAReceber) {
    this.pessoaId = pessoaId;
    this.nome = nome;
    this.totalPago = totalPago;
    this.valorDevido = valorDevido;
    this.totalAReceber = totalAReceber;
  }

  public Long getPessoaId() {
    return pessoaId;
  }

  public String getNome() {
    return nome;
  }

  public BigDecimal getTotalPago() {
    return totalPago;
  }

  public BigDecimal getValorDevido() {
    return valorDevido;
  }

  public BigDecimal getTotalAReceber() {
    return totalAReceber;
  }
}
