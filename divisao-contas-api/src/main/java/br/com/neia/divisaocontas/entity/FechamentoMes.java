package br.com.neia.divisaocontas.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fechamento_mes_grupo", uniqueConstraints = @UniqueConstraint(columnNames = { "grupo_id", "ano",
    "mes" }))
public class FechamentoMes {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int ano;
  private int mes;

  @ManyToOne(optional = false)
  @JoinColumn(name = "grupo_id", nullable = false)
  private Grupo grupo;

  private LocalDateTime dataFechamento;

  private String observacao;

  public FechamentoMes() {
  }

  public FechamentoMes(int ano, int mes, Grupo grupo, String observacao) {
    this.ano = ano;
    this.mes = mes;
    this.grupo = grupo;
    this.observacao = observacao;
    this.dataFechamento = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public int getAno() {
    return ano;
  }

  public int getMes() {
    return mes;
  }

  public Grupo getGrupo() {
    return grupo;
  }

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
  public LocalDateTime getDataFechamento() {
    return dataFechamento;
  }

  public String getObservacao() {
    return observacao;
  }

  public void setGrupo(Grupo grupo) {
    this.grupo = grupo;
  }

  public void setObservacao(String observacao) {
    this.observacao = observacao;
  }
}
