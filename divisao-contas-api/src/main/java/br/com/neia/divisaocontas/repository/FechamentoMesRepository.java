package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.FechamentoMes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FechamentoMesRepository extends JpaRepository<FechamentoMes, Long> {
  boolean existsByAnoAndMes(int ano, int mes);

  Optional<FechamentoMes> findByAnoAndMes(int ano, int mes);

  void deleteByAnoAndMes(int ano, int mes);
}
