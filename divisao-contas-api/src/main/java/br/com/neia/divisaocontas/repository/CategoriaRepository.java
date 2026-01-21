package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
  Optional<Categoria> findByNome(String nome);
}
