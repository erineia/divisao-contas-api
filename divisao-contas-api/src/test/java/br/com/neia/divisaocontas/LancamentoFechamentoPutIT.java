package br.com.neia.divisaocontas;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.jayway.jsonpath.JsonPath;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LancamentoFechamentoPutIT {

  @LocalServerPort
  int port;

  @BeforeEach
  void setup() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  @Test
  void deveBloquearAlteracaoDeLancamentoQuandoMesFechado() {

    // 1) cria pessoas
    int nataliaId = given().contentType(ContentType.JSON)
        .body("{\"nome\":\"Natalia\"}")
        .when().post("/api/pessoas")
        .then().statusCode(201)
        .extract().path("id");

    int neiaId = given().contentType(ContentType.JSON)
        .body("{\"nome\":\"Neia\"}")
        .when().post("/api/pessoas")
        .then().statusCode(201)
        .extract().path("id");

    // 2) cria lançamento em 01/2026 (antes de fechar)
    String criarBody = """
        {
          "descricao": "Compras Mercado",
          "data": "2026-01-15",
          "valor": 400,
          "pagadorId": %d,
          "divide": true,
          "participantesIds": [%d]
        }
        """.formatted(nataliaId, neiaId);

    int lancamentoId = given().contentType(ContentType.JSON)
        .body(criarBody)
        .when().post("/api/lancamentos")
        .then().statusCode(201)
        .extract().path("id");

    // 3) fecha 01/2026
    given()
        .when()
        .post("/api/fechamentos?ano=2026&mes=1")
        .then()
        .statusCode(201);

    // 4) tenta alterar o lançamento (deve falhar 400)
    String putBody = """
        {
          "descricao": "Compras Mercado ALTERADO",
          "data": "2026-01-15",
          "valor": 500,
          "pagadorId": %d,
          "divide": true,
          "participantesIds": [%d]
        }
        """.formatted(nataliaId, neiaId);

    URI uri = URI.create("http://localhost:" + port + "/api/lancamentos/" + lancamentoId);

    HttpRequest request = HttpRequest.newBuilder(uri)
        .header("Content-Type", "application/json")
        .PUT(HttpRequest.BodyPublishers.ofString(putBody))
        .build();

    HttpResponse<String> response;
    try {
      response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    assertEquals(400, response.statusCode());
    String mensagem = JsonPath.read(response.body(), "$.mensagem");
    assertNotNull(mensagem);
    assertTrue(mensagem.contains("mês está fechado"));
  }
}
