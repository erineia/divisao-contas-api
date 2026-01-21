package br.com.neia.divisaocontas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PagamentoCreateRequest {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private LocalDate data;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal valor;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Long pagadorId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Long recebedorId;

  private Long categoriaId;

  private String observacao;

  public LocalDate getData() {
    return data;
  }

  public void setData(LocalDate data) {
    this.data = data;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public void setValor(BigDecimal valor) {
    this.valor = valor;
  }

  public Long getPagadorId() {
    return pagadorId;
  }

  public void setPagadorId(Long pagadorId) {
    this.pagadorId = pagadorId;
  }

  public Long getRecebedorId() {
    return recebedorId;
  }

  public Long getCategoriaId() {
    return categoriaId;
  }

  public void setRecebedorId(Long recebedorId) {
    this.recebedorId = recebedorId;
  }

  public void setCategoriaId(Long categoriaId) {
    this.categoriaId = categoriaId;
  }

  public String getObservacao() {
    return observacao;
  }

  public void setObservacao(String observacao) {
    this.observacao = observacao;
  }
}
