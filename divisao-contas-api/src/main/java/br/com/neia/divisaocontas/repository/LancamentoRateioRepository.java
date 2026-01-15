package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.LancamentoRateio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LancamentoRateioRepository extends JpaRepository<LancamentoRateio, Long> {
  List<LancamentoRateio> findByLancamentoId(Long lancamentoId);

  void deleteByLancamentoId(Long lancamentoId);

}
