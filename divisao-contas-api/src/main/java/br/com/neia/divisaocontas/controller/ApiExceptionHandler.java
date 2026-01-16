
package br.com.neia.divisaocontas.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import br.com.neia.divisaocontas.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Hidden // Oculta do Swagger UI, mas pode ser removido se quiser exibir
public class ApiExceptionHandler {

  private ResponseEntity<Map<String, Object>> build(HttpStatus status, String mensagem, String path) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("dataHora", OffsetDateTime.now().toString());
    body.put("status", status.value());
    body.put("erro", status.getReasonPhrase());
    body.put("mensagem", mensagem);
    body.put("path", path);
    return ResponseEntity.status(status).body(body);
  }

  // 400: regras de negócio / validações manuais
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex,
      jakarta.servlet.http.HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
  }

  // 400: parâmetro com tipo errado (ex: ano=abc)
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
      jakarta.servlet.http.HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + ex.getName(), req.getRequestURI());
  }

  // 400: JSON malformado
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex,
      jakarta.servlet.http.HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, "JSON inválido ou campos com formato incorreto.", req.getRequestURI());
  }

  // 500: erro inesperado (não expor detalhes)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex,
      jakarta.servlet.http.HttpServletRequest req) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado.", req.getRequestURI());
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex,
      jakarta.servlet.http.HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
  }
}
