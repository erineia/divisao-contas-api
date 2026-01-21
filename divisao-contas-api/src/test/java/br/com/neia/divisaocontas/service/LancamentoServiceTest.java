package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.dto.LancamentoCreateRequest;
import br.com.neia.divisaocontas.entity.Categoria;
import br.com.neia.divisaocontas.repository.LancamentoRateioRepository;
import br.com.neia.divisaocontas.repository.LancamentoRepository;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

  @Mock
  LancamentoRepository lancamentoRepository;

  @Mock
  PessoaRepository pessoaRepository;

  @Mock
  LancamentoRateioRepository rateioRepository;

  @Mock
  FechamentoMesService fechamentoMesService;

  @Mock
  CategoriaService categoriaService;

  @Test
  void criar_quandoMesFechado_deveFalharAntesDeAcessarRepositorios() {
    LancamentoService service = new LancamentoService(
        lancamentoRepository,
        pessoaRepository,
        rateioRepository,
        fechamentoMesService,
        categoriaService);

    LancamentoCreateRequest req = new LancamentoCreateRequest();
    req.setDescricao("Teste");
    req.setData(LocalDate.of(2099, 1, 10));
    req.setValor(new BigDecimal("10.00"));
    req.setPagadorId(1L);

    when(categoriaService.resolveCategoria(any(), eq(req.getData())))
      .thenReturn(new Categoria("Mes/01"));

    doThrow(new IllegalArgumentException("Este mês está fechado. Reabra para alterar."))
        .when(fechamentoMesService)
      .validarAberto(any(), any());

    assertThrows(IllegalArgumentException.class, () -> service.criar(req));

    verifyNoInteractions(lancamentoRepository, pessoaRepository, rateioRepository);
  }

  @Test
  void criar_quandoDescricaoNula_deveFalharSemChamarOutrasDependencias() {
    LancamentoService service = new LancamentoService(
        lancamentoRepository,
        pessoaRepository,
        rateioRepository,
        fechamentoMesService,
        categoriaService);

    LancamentoCreateRequest req = new LancamentoCreateRequest();
    req.setDescricao(null);
    req.setData(LocalDate.of(2099, 1, 10));
    req.setValor(new BigDecimal("10.00"));
    req.setPagadorId(1L);

    assertThrows(IllegalArgumentException.class, () -> service.criar(req));

    verifyNoInteractions(fechamentoMesService, lancamentoRepository, pessoaRepository, rateioRepository);
  }
}
