package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
  boolean existsByNomeIgnoreCase(String nome);
}
