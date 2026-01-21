package br.com.neia.divisaocontas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

public class ErrorResponse {

  @JsonProperty("dataHora")
  private String dataHora;

  @JsonProperty("status")
  private int status;

  @JsonProperty("erro")
  private String erro;

  @JsonProperty("mensagem")
  private String mensagem;

  @JsonProperty("path")
  private String path;

  public String getDataHora() {
    return dataHora;
  }

  public void setDataHora(String dataHora) {
    this.dataHora = dataHora;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getErro() {
    return erro;
  }

  public void setErro(String erro) {
    this.erro = erro;
  }

  public String getMensagem() {
    return mensagem;
  }

  public void setMensagem(String mensagem) {
    this.mensagem = mensagem;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
