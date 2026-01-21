package br.com.neia.divisaocontas.dto;

public class FechamentoMesResponse {

  private Long id;

  private int ano;

  private int mes;

  private Long categoriaId;

  private String categoriaNome;

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

  public FechamentoMesResponse(Long id, int ano, int mes, Long categoriaId, String categoriaNome, String dataFechamento,
      String observacao) {
    this.id = id;
    this.ano = ano;
    this.mes = mes;
    this.categoriaId = categoriaId;
    this.categoriaNome = categoriaNome;
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

  public Long getCategoriaId() {
    return categoriaId;
  }

  public String getCategoriaNome() {
    return categoriaNome;
  }

  public void setMes(int mes) {
    this.mes = mes;
  }

  public void setCategoriaId(Long categoriaId) {
    this.categoriaId = categoriaId;
  }

  public void setCategoriaNome(String categoriaNome) {
    this.categoriaNome = categoriaNome;
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
