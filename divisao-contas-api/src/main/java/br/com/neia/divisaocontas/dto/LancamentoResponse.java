package br.com.neia.divisaocontas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "id", "descricao", "data", "valor", "categoriaId", "categoriaNome", "divide", "pagador",
    "participantes", "devedores" })
public class LancamentoResponse {
  private Long id;

  private String descricao;

  private String data; // dd/MM/yyyy

  private BigDecimal valor;

  private Long categoriaId;

  private String categoriaNome;

  private boolean divide;

  @Schema(implementation = PessoaResponse.class)
  private PessoaResponse pagador;

  private List<LancamentoPessoaValorResponse> participantes;
  private List<LancamentoPessoaValorResponse> devedores;

  public LancamentoResponse() {
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

  public Long getCategoriaId() {
    return categoriaId;
  }

  public String getCategoriaNome() {
    return categoriaNome;
  }

  public boolean isDivide() {
    return divide;
  }

  public PessoaResponse getPagador() {
    return pagador;
  }

  public List<LancamentoPessoaValorResponse> getParticipantes() {
    return participantes;
  }

  public List<LancamentoPessoaValorResponse> getDevedores() {
    return devedores;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setDescricao(String descricao) {
    this.descricao = descricao;
  }

  public void setData(String data) {
    this.data = data;
  }

  public void setValor(BigDecimal valor) {
    this.valor = valor;
  }

  public void setCategoriaId(Long categoriaId) {
    this.categoriaId = categoriaId;
  }

  public void setCategoriaNome(String categoriaNome) {
    this.categoriaNome = categoriaNome;
  }

  public void setDivide(boolean divide) {
    this.divide = divide;
  }

  public void setPagador(PessoaResponse pagador) {
    this.pagador = pagador;
  }

  public void setParticipantes(List<LancamentoPessoaValorResponse> participantes) {
    this.participantes = participantes;
  }

  public void setDevedores(List<LancamentoPessoaValorResponse> devedores) {
    this.devedores = devedores;
  }
}
