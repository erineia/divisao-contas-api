package br.com.neia.divisaocontas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LancamentoCreateRequest {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String descricao;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private LocalDate data;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal valor;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Long pagadorId;

  private Long categoriaId;

  private boolean divide;

  private List<Long> participantesIds;

  private List<DevedorItemRequest> devedores;

  public String getDescricao() {
    return descricao;
  }

  public LocalDate getData() {
    return data;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public Long getPagadorId() {
    return pagadorId;
  }

  public Long getCategoriaId() {
    return categoriaId;
  }

  public void setDescricao(String descricao) {
    this.descricao = descricao;
  }

  public void setData(LocalDate data) {
    this.data = data;
  }

  public void setValor(BigDecimal valor) {
    this.valor = valor;
  }

  public void setPagadorId(Long pagadorId) {
    this.pagadorId = pagadorId;
  }

  public void setCategoriaId(Long categoriaId) {
    this.categoriaId = categoriaId;
  }

  public boolean isDivide() {
    return divide;
  }

  public void setDivide(boolean divide) {
    this.divide = divide;
  }

  public List<Long> getParticipantesIds() {
    return participantesIds;
  }

  public void setParticipantesIds(List<Long> participantesIds) {
    this.participantesIds = participantesIds;
  }

  public List<DevedorItemRequest> getDevedores() {
    return devedores;
  }

  public void setDevedores(List<DevedorItemRequest> devedores) {
    this.devedores = devedores;
  }

}
