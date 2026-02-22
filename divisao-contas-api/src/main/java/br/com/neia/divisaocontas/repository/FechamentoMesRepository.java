package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.FechamentoMes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FechamentoMesRepository extends JpaRepository<FechamentoMes, Long> {
  boolean existsByAnoAndMesAndGrupoId(int ano, int mes, Long grupoId);

  boolean existsByGrupoId(Long grupoId);

  Optional<FechamentoMes> findByAnoAndMesAndGrupoId(int ano, int mes, Long grupoId);

  void deleteByAnoAndMesAndGrupoId(int ano, int mes, Long grupoId);
}
