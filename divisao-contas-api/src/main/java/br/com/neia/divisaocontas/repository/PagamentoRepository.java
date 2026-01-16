package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
  List<Pagamento> findByDataBetween(LocalDate inicio, LocalDate fim);
}
