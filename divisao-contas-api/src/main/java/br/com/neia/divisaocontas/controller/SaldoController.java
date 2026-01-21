package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.SaldoPessoaResponse;
import org.springframework.web.bind.annotation.*;
import br.com.neia.divisaocontas.dto.TransferenciaResponse;
import java.util.List;
import br.com.neia.divisaocontas.service.SaldoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/saldos")
@Tag(name = "Saldos")
public class SaldoController {

  private final SaldoService saldoService;

  public SaldoController(SaldoService saldoService) {
    this.saldoService = saldoService;
  }

  @GetMapping
  @Operation(summary = "Saldo do mês", description = "Retorna, para cada pessoa, total pago, quanto deve e quanto tem a receber no mês.")
  public List<SaldoPessoaResponse> saldoDoMes(
      @RequestParam int ano,
      @RequestParam int mes,
      @RequestParam(required = false) Long categoriaId) {
    return saldoService.saldoDoMes(ano, mes, categoriaId);
  }

  @GetMapping("/quem-deve")
  @Operation(summary = "Quem deve (mês)", description = "Gera uma lista de transferências sugeridas (devedor -> credor) para quitar o mês.")
  public List<TransferenciaResponse> quemDeve(
      @RequestParam int ano,
      @RequestParam int mes,
      @RequestParam(required = false) Long categoriaId) {
    return saldoService.quemDeve(ano, mes, categoriaId);
  }

  @GetMapping("/periodo")
  @Operation(summary = "Saldo por período", description = "Aceita datas em yyyy-MM-dd ou dd/MM/yyyy.")
  public List<SaldoPessoaResponse> saldoPorPeriodo(@RequestParam String dataInicio,
      @RequestParam String dataFim,
      @RequestParam(required = false) Long categoriaId) {
    return saldoService.saldoPorPeriodo(dataInicio, dataFim, categoriaId);
  }

  @GetMapping("/quem-deve/periodo")
  @Operation(summary = "Quem deve (período)", description = "Gera transferências sugeridas para quitar o saldo no período.")
  public List<TransferenciaResponse> quemDevePeriodo(@RequestParam String dataInicio,
      @RequestParam String dataFim,
      @RequestParam(required = false) Long categoriaId) {
    return saldoService.quemDevePeriodo(dataInicio, dataFim, categoriaId);
  }

  @GetMapping("/acumulado")
  @Operation(summary = "Acumulado até mês", description = "Gera transferências sugeridas considerando lançamentos e pagamentos até (inclusive) o mês informado.")
  public List<TransferenciaResponse> acumuladoAteMes(
      @RequestParam int ateAno,
      @RequestParam int ateMes,
      @RequestParam(required = false) Long categoriaId) {
    return saldoService.acumuladoAteMes(ateAno, ateMes, categoriaId);
  }
}
