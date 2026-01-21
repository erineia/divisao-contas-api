package br.com.neia.divisaocontas.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  @Operation(summary = "Gera um token JWT")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    String usuario = request.getUsuario();
    log.info("/auth/login: iniciando autenticação para usuario='{}'", usuario);

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(usuario, request.getSenha()));

    log.info("/auth/login: autenticação OK para usuario='{}' (authorities={})", usuario,
        authentication.getAuthorities() == null ? 0 : authentication.getAuthorities().size());

    String token = jwtService.generateToken(authentication);
    log.info("/auth/login: token gerado para usuario='{}' (len={})", usuario, token == null ? 0 : token.length());
    return ResponseEntity.ok(new TokenResponse(token));
  }
}
