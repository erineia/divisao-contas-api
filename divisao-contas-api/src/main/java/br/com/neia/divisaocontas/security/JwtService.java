package br.com.neia.divisaocontas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final Logger log = LoggerFactory.getLogger(JwtService.class);

  private final String secret;
  private final long expirationSeconds;

  public JwtService(
      @Value("${app.security.jwt.secret}") String secret,
      @Value("${app.security.jwt.expiration-seconds:3600}") long expirationSeconds) {
    this.secret = secret;
    this.expirationSeconds = expirationSeconds;
  }

  public String generateToken(Authentication authentication) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(expirationSeconds);

    List<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    return Jwts.builder()
        .setSubject(authentication.getName())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .claim("roles", roles)
        .signWith(signingKey(), Jwts.SIG.HS256)
        .compact();
  }

  public String extractUsername(String token) {
    return parseAllClaims(token).getSubject();
  }

  public boolean isTokenValid(String token) {
    try {
      parseAllClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private Claims parseAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(signingKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private SecretKey signingKey() {
    byte[] keyBytes = null;
    boolean usedBase64 = false;
    boolean usedBase64Url = false;

    // Se o secret for Base64 válido e gerar uma chave forte, usamos.
    // Caso contrário, tratamos como string "normal" (UTF-8) para evitar falhas por
    // chave fraca.
    try {
      byte[] decoded = Decoders.BASE64.decode(secret);
      if (decoded != null && decoded.length >= 32) {
        keyBytes = decoded;
        usedBase64 = true;
      }
    } catch (Exception ignored) {
      // não era base64 (ou tinha caracteres inválidos)
    }

    if (keyBytes == null) {
      try {
        byte[] decodedUrl = Decoders.BASE64URL.decode(secret);
        if (decodedUrl != null && decodedUrl.length >= 32) {
          keyBytes = decodedUrl;
          usedBase64Url = true;
        }
      } catch (Exception ignored) {
        // não era base64url (ou tinha caracteres inválidos)
      }
    }

    if (keyBytes == null) {
      keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    // HS256 exige pelo menos 256 bits (32 bytes)
    if (keyBytes.length < 32) {
      throw new IllegalStateException("Configuração inválida: app.security.jwt.secret deve ter pelo menos 32 bytes.");
    }

    log.debug("JWT signing key pronta (bytes={}, base64={}, base64Url={})", keyBytes.length, usedBase64, usedBase64Url);

    return Keys.hmacShaKeyFor(keyBytes);
  }
}
