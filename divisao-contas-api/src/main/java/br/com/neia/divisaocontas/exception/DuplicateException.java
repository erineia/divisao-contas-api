package br.com.neia.divisaocontas.exception;

public class DuplicateException extends RuntimeException {
  public DuplicateException(String message) {
    super(message);
  }
}
