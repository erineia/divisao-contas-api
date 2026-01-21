package br.com.neia.divisaocontas.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Schema(hidden = true)
public class Pagamento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDate data;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal valor;

  @ManyToOne(optional = false)
  private Pessoa pagador;

  @ManyToOne(optional = false)
  private Pessoa recebedor;

  @ManyToOne
  private Categoria categoria;

  private String observacao;

  public Long getId() {
    return id;
  }

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

  public Pessoa getPagador() {
    return pagador;
  }

  public void setPagador(Pessoa pagador) {
    this.pagador = pagador;
  }

  public Pessoa getRecebedor() {
    return recebedor;
  }

  public void setRecebedor(Pessoa recebedor) {
    this.recebedor = recebedor;
  }

  public Categoria getCategoria() {
    return categoria;
  }

  public void setCategoria(Categoria categoria) {
    this.categoria = categoria;
  }

  public String getObservacao() {
    return observacao;
  }

  public void setObservacao(String observacao) {
    this.observacao = observacao;
  }
}
