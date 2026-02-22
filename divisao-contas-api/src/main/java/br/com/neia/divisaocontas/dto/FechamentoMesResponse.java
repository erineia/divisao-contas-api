package br.com.neia.divisaocontas.dto;

public class FechamentoMesResponse {

  private Long id;

  private int ano;

  private int mes;

  private Long grupoId;

  private String grupoNome;

  private String dataFechamento;

  private String observacao;

  public FechamentoMesResponse() {
  }

  public FechamentoMesResponse(Long id, int ano, int mes, String dataFechamento, String observacao) {
    this.id = id;
    this.ano = ano;
    this.mes = mes;
    this.dataFechamento = dataFechamento;
    this.observacao = observacao;
  }

  public FechamentoMesResponse(Long id, int ano, int mes, Long grupoId, String grupoNome, String dataFechamento,
      String observacao) {
    this.id = id;
    this.ano = ano;
    this.mes = mes;
    this.grupoId = grupoId;
    this.grupoNome = grupoNome;
    this.dataFechamento = dataFechamento;
    this.observacao = observacao;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public int getAno() {
    return ano;
  }

  public void setAno(int ano) {
    this.ano = ano;
  }

  public int getMes() {
    return mes;
  }

  public Long getGrupoId() {
    return grupoId;
  }

  public String getGrupoNome() {
    return grupoNome;
  }

  public void setMes(int mes) {
    this.mes = mes;
  }

  public void setGrupoId(Long grupoId) {
    this.grupoId = grupoId;
  }

  public void setGrupoNome(String grupoNome) {
    this.grupoNome = grupoNome;
  }

  public String getDataFechamento() {
    return dataFechamento;
  }

  public void setDataFechamento(String dataFechamento) {
    this.dataFechamento = dataFechamento;
  }

  public String getObservacao() {
    return observacao;
  }

  public void setObservacao(String observacao) {
    this.observacao = observacao;
  }
}
