package br.com.neia.divisaocontas;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class TestAuth {

  private static String cachedToken;
  private static int cachedPort;

  private TestAuth() {
  }

  static String token(int port) {
    if (cachedToken != null && cachedPort == port) {
      return cachedToken;
    }

    URI uri = URI.create("http://localhost:" + port + "/auth/login");
    String body = "{\"usuario\":\"test\",\"senha\":\"123456\"}";

    HttpRequest request = HttpRequest.newBuilder(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response;
    try {
      response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "Login de teste falhou: HTTP " + response.statusCode() + " body=" + response.body());
    }

    cachedToken = JsonPath.read(response.body(), "$.token");
    cachedPort = port;
    return cachedToken;
  }
}
