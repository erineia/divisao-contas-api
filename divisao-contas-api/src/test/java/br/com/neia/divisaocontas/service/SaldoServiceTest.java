package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.repository.LancamentoRateioRepository;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SaldoServiceTest {

  @Mock
  PessoaRepository pessoaRepository;

  @Mock
  LancamentoRepository lancamentoRepository;

  @Mock
  LancamentoRateioRepository rateioRepository;

  @Mock
  PagamentoRepository pagamentoRepository;

  @Mock
  CategoriaService categoriaService;

  @Test
  void quemDeve_quandoMesInvalido_deveFalharSemConsultarRepositorios() {
    SaldoService service = new SaldoService(pessoaRepository, lancamentoRepository, rateioRepository,
        pagamentoRepository, categoriaService);

    assertThrows(IllegalArgumentException.class, () -> service.quemDeve(2026, 0, null));

    verifyNoInteractions(pessoaRepository, lancamentoRepository, rateioRepository, pagamentoRepository, categoriaService);
  }

  @Test
  void saldoPorPeriodo_quandoDataFimAntesDaInicio_deveFalharSemConsultarRepositorios() {
    SaldoService service = new SaldoService(pessoaRepository, lancamentoRepository, rateioRepository,
        pagamentoRepository, categoriaService);

    assertThrows(IllegalArgumentException.class,
        () -> service.saldoPorPeriodo("2026-01-10", "2026-01-01", null));

    verifyNoInteractions(pessoaRepository, lancamentoRepository, rateioRepository, pagamentoRepository, categoriaService);
  }
}
