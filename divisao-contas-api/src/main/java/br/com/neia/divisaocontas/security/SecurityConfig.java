package br.com.neia.divisaocontas.security;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private static final DateTimeFormatter DATA_HORA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter)
      throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
            // Sem autenticação (sem token/ token inválido) -> 401
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setCharacterEncoding(StandardCharsets.UTF_8.name());
              response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

              String json = "{"
                  + "\"dataHora\":\"" + OffsetDateTime.now().format(DATA_HORA_BR) + "\","
                  + "\"status\":401,"
                  + "\"erro\":\"Unauthorized\","
                  + "\"mensagem\":\"Não autenticado.\","
                  + "\"path\":\"" + request.getRequestURI() + "\""
                  + "}";

              response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            })
            // Autenticado porém sem permissão -> 403
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              response.setStatus(HttpServletResponse.SC_FORBIDDEN);
              response.setCharacterEncoding(StandardCharsets.UTF_8.name());
              response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

              String json = "{"
                  + "\"dataHora\":\"" + OffsetDateTime.now().format(DATA_HORA_BR) + "\","
                  + "\"status\":403,"
                  + "\"erro\":\"Forbidden\","
                  + "\"mensagem\":\"Acesso negado.\","
                  + "\"path\":\"" + request.getRequestURI() + "\""
                  + "}";

              response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            }))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/auth/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html")
            .permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of(
        "http://localhost:*",
        "http://127.0.0.1:*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
    config.setExposedHeaders(List.of("Authorization"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public UserDetailsService userDetailsService(
      PasswordEncoder encoder,
      @Value("${app.security.user.name:admin}") String username,
      @Value("${app.security.user.password:admin}") String rawPassword) {

    return new InMemoryUserDetailsManager(
        User.withUsername(username)
            .password(encoder.encode(rawPassword))
            .roles("USER")
            .build());
  }

  @Bean
  public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
  }
}
