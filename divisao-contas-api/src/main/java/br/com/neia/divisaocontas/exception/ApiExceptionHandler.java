package br.com.neia.divisaocontas.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import io.swagger.v3.oas.annotations.Hidden;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Hidden // Oculta do Swagger UI, mas pode ser removido se quiser exibir
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private static final DateTimeFormatter DATA_HORA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  private ResponseEntity<Map<String, Object>> build(HttpStatus status, String mensagem, String path) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("dataHora", OffsetDateTime.now().format(DATA_HORA_BR));
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

  // 400: validação de Bean Validation (@Valid)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleNotValid(MethodArgumentNotValidException ex,
      jakarta.servlet.http.HttpServletRequest req) {
    String mensagem = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : ("Campo inválido: " + err.getField()))
        .orElse("Dados inválidos.");

    return build(HttpStatus.BAD_REQUEST, mensagem, req.getRequestURI());
  }

  // 409: duplicidade / conflito
  @ExceptionHandler(DuplicateException.class)
  public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateException ex,
      jakarta.servlet.http.HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
  }

  // 409: violação de unicidade / integridade no banco
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex,
      jakarta.servlet.http.HttpServletRequest req) {
    String path = req.getRequestURI();
    String mensagem;

    if (path != null && path.startsWith("/api/pessoas")) {
      mensagem = "Já existe uma pessoa com esse nome.";
    } else if (path != null && path.startsWith("/api/fechamentos")) {
      mensagem = "Este mês já está fechado.";
    } else if (path != null && path.startsWith("/api/lancamentos")) {
      mensagem = "Lançamento já existe.";
    } else {
      mensagem = "Conflito de dados: registro já existe ou viola uma restrição.";
    }

    return build(HttpStatus.CONFLICT, mensagem, path);
  }

  // 401: credenciais inválidas / erro de autenticação
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex,
      jakarta.servlet.http.HttpServletRequest req) {
    log.warn("Falha de autenticação em {}: {}", req.getRequestURI(), ex.getClass().getSimpleName());
    return build(HttpStatus.UNAUTHORIZED, "Usuário e/ou senha inválidos.", req.getRequestURI());
  }

  // 403: autenticado porém sem permissão
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
      jakarta.servlet.http.HttpServletRequest req) {
    return build(HttpStatus.FORBIDDEN, "Acesso negado.", req.getRequestURI());
  }

  // 500: erro inesperado (não expor detalhes)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex,
      jakarta.servlet.http.HttpServletRequest req) {
    log.error("Erro inesperado em {}", req.getRequestURI(), ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado.", req.getRequestURI());
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex,
      jakarta.servlet.http.HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
  }
}
