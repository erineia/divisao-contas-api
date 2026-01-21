package br.com.neia.divisaocontas.security;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoginRequest {

  @NotBlank
  @JsonAlias({ "username" })
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
  private String usuario;

  @NotBlank
  @Pattern(regexp = "\\d+", message = "Senha deve conter apenas números.")
  @JsonAlias({ "password" })
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
  private String senha;

  public String getUsuario() {
    return usuario;
  }

  public void setUsuario(String usuario) {
    this.usuario = usuario;
  }

  public String getSenha() {
    return senha;
  }

  public void setSenha(String senha) {
    this.senha = senha;
  }
}
