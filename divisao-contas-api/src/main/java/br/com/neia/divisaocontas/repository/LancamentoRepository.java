package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
  boolean existsByDescricaoAndDataAndValorAndPagador(String descricao, java.time.LocalDate data,
      java.math.BigDecimal valor, br.com.neia.divisaocontas.entity.Pessoa pagador);

  boolean existsByDescricaoAndDataAndValorAndPagadorAndIdNot(String descricao, java.time.LocalDate data,
      java.math.BigDecimal valor, br.com.neia.divisaocontas.entity.Pessoa pagador, Long id);

  boolean existsByPagadorId(Long pagadorId);

}
