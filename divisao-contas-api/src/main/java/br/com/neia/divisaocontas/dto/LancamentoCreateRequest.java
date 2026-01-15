package br.com.neia.divisaocontas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LancamentoCreateRequest {

  private String descricao;
  private LocalDate data;
  private BigDecimal valor;
  private Long pagadorId;
  private List<Long> participantesIds;

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

  public List<Long> getParticipantesIds() {
    return participantesIds;
  }

  public void setParticipantesIds(List<Long> participantesIds) {
    this.participantesIds = participantesIds;
  }
}
