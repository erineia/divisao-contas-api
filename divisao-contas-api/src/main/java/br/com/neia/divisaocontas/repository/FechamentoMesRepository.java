package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.FechamentoMes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FechamentoMesRepository extends JpaRepository<FechamentoMes, Long> {
  boolean existsByAnoAndMesAndCategoriaId(int ano, int mes, Long categoriaId);

  boolean existsByCategoriaId(Long categoriaId);

  Optional<FechamentoMes> findByAnoAndMesAndCategoriaId(int ano, int mes, Long categoriaId);

  void deleteByAnoAndMesAndCategoriaId(int ano, int mes, Long categoriaId);
}
