package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
    boolean existsByDescricaoAndDataAndValorAndPagadorAndGrupo(String descricao, java.time.LocalDate data,
            java.math.BigDecimal valor, br.com.neia.divisaocontas.entity.Pessoa pagador,
            br.com.neia.divisaocontas.entity.Grupo grupo);

    boolean existsByDescricaoAndDataAndValorAndPagadorAndGrupoAndIdNot(String descricao, java.time.LocalDate data,
            java.math.BigDecimal valor, br.com.neia.divisaocontas.entity.Pessoa pagador,
            br.com.neia.divisaocontas.entity.Grupo grupo, Long id);

    boolean existsByPagadorId(Long pagadorId);

    boolean existsByGrupoId(Long grupoId);

}
