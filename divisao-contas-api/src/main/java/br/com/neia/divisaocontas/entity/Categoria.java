package br.com.neia.divisaocontas.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categoria", uniqueConstraints = @UniqueConstraint(columnNames = { "nome" }))
public class Categoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String nome;

  public Categoria() {
  }

  public Categoria(String nome) {
    this.nome = nome;
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }
}
