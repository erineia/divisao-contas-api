package br.com.neia.divisaocontas.security;

import io.swagger.v3.oas.annotations.media.Schema;

public class TokenResponse {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String token;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Bearer")
  private String type;

  public TokenResponse(String token) {
    this.token = token;
    this.type = "Bearer";
  }

  public String getToken() {
    return token;
  }

  public String getType() {
    return type;
  }
}
