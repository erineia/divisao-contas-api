package br.com.neia.divisaocontas.service;

import br.com.neia.divisaocontas.dto.PagamentoCreateRequest;
import br.com.neia.divisaocontas.entity.Categoria;
import br.com.neia.divisaocontas.entity.Pessoa;
import br.com.neia.divisaocontas.repository.PagamentoRepository;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

  @Mock
  PagamentoRepository pagamentoRepository;

  @Mock
  PessoaRepository pessoaRepository;

  @Mock
  FechamentoMesService fechamentoMesService;

  @Mock
  CategoriaService categoriaService;

  @Test
  void criar_quandoPagadorIgualRecebedor_deveFalharSemSalvar() {
    PagamentoService service = new PagamentoService(pagamentoRepository, pessoaRepository, fechamentoMesService,
        categoriaService);

    PagamentoCreateRequest req = new PagamentoCreateRequest();
    req.setData(LocalDate.of(2099, 1, 10));
    req.setValor(new BigDecimal("10.00"));
    req.setPagadorId(1L);
    req.setRecebedorId(1L);

    Pessoa p = new Pessoa();
    p.setId(1L);
    p.setNome("A");

    when(categoriaService.resolveCategoria(any(), eq(req.getData())))
      .thenReturn(new Categoria("Mes/01"));

    when(pessoaRepository.findById(1L)).thenReturn(Optional.of(p));

    assertThrows(IllegalArgumentException.class, () -> service.criar(req));

    verify(fechamentoMesService).validarAberto(eq(req.getData()), any());
    verify(pagamentoRepository, never()).save(any());
  }

  @Test
  void listarPorPeriodo_quandoFimAntesDoInicio_deveFalharSemConsultarRepositorio() {
    PagamentoService service = new PagamentoService(pagamentoRepository, pessoaRepository, fechamentoMesService,
        categoriaService);

    LocalDate inicio = LocalDate.of(2026, 1, 10);
    LocalDate fim = LocalDate.of(2026, 1, 1);

    assertThrows(IllegalArgumentException.class, () -> service.listarPorPeriodo(inicio, fim));

    verifyNoInteractions(pagamentoRepository);
  }
}
