package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.LancamentoRateio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LancamentoRateioRepository extends JpaRepository<LancamentoRateio, Long> {
  List<LancamentoRateio> findByLancamentoId(Long lancamentoId);

  @Transactional
  void deleteByLancamentoId(Long lancamentoId);

  boolean existsByPessoaId(Long pessoaId);

}
