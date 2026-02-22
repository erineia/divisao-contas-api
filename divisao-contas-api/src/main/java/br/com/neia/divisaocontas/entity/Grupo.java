package br.com.neia.divisaocontas.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "grupo", uniqueConstraints = @UniqueConstraint(columnNames = { "nome" }))
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    public Grupo() {
    }

    public Grupo(String nome) {
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
