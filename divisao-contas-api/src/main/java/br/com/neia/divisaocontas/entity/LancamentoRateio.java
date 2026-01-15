package br.com.neia.divisaocontas.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "lancamento_rateio")
public class LancamentoRateio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "lancamento_id")
  private Lancamento lancamento;

  @ManyToOne(optional = false)
  @JoinColumn(name = "pessoa_id")
  private Pessoa pessoa;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal valorDevido;

  public LancamentoRateio() {
  }

  public Long getId() {
    return id;
  }

  public Lancamento getLancamento() {
    return lancamento;
  }

  public Pessoa getPessoa() {
    return pessoa;
  }

  public BigDecimal getValorDevido() {
    return valorDevido;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setLancamento(Lancamento lancamento) {
    this.lancamento = lancamento;
  }

  public void setPessoa(Pessoa pessoa) {
    this.pessoa = pessoa;
  }

  public void setValorDevido(BigDecimal valorDevido) {
    this.valorDevido = valorDevido;
  }
}
