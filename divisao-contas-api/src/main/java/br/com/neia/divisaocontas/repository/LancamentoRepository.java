package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
}
