package br.com.neia.divisaocontas.entity;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lancamento")
public class Lancamento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String descricao;

  @Column(nullable = false)
  private LocalDate data;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal valor;

  @ManyToOne
  @JoinColumn(name = "pagador_id", nullable = false)
  private Pessoa pagador;

  public Lancamento() {
  }

  public Long getId() {
    return id;
  }

  public String getDescricao() {
    return descricao;
  }

  public LocalDate getData() {
    return data;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public Pessoa getPagador() {
    return pagador;
  }

  public void setId(Long id) {
    this.id = id;
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

  public void setPagador(Pessoa pagador) {
    this.pagador = pagador;
  }
}
