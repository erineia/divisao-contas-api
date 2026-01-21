package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
  boolean existsByDescricaoAndDataAndValorAndPagadorAndCategoria(String descricao, java.time.LocalDate data,
      java.math.BigDecimal valor, br.com.neia.divisaocontas.entity.Pessoa pagador,
      br.com.neia.divisaocontas.entity.Categoria categoria);

  boolean existsByDescricaoAndDataAndValorAndPagadorAndCategoriaAndIdNot(String descricao, java.time.LocalDate data,
      java.math.BigDecimal valor, br.com.neia.divisaocontas.entity.Pessoa pagador,
      br.com.neia.divisaocontas.entity.Categoria categoria, Long id);

  boolean existsByPagadorId(Long pagadorId);

  boolean existsByCategoriaId(Long categoriaId);

}
