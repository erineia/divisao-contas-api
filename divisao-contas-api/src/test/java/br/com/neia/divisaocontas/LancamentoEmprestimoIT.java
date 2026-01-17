package br.com.neia.divisaocontas;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LancamentoEmprestimoIT {

  @LocalServerPort
  int port;

  @BeforeEach
  void setup() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  private int criaPessoa(String nome) {
    String nomeUnico = nome + "-" + UUID.randomUUID();
    return given().contentType(ContentType.JSON)
        .body("{\"nome\":\"" + nomeUnico + "\"}")
        .when().post("/api/pessoas")
        .then().statusCode(201)
        .extract().path("id");
  }

  @Test
  void deveCriarLancamentoEmprestimoQuandoSomaDevedoresIgualTotal() {

    int nataliaId = criaPessoa("Natalia");
    int neiaId = criaPessoa("Neia");
    int joaoId = criaPessoa("Joao");

    String body = """
        {
          "descricao": "Supermercado",
          "data": "2026-01-17",
          "valor": 400,
          "pagadorId": %d,
          "divide": false,
          "devedores": [
            { "pessoaId": %d, "valor": 250 },
            { "pessoaId": %d, "valor": 150 }
          ]
        }
        """.formatted(nataliaId, neiaId, joaoId);

    given().contentType(ContentType.JSON)
        .body(body)
        .when().post("/api/lancamentos")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("divide", equalTo(false))
        .body("pagador.id", equalTo(nataliaId))
        .body("devedores", hasSize(2))
        .body("participantes", nullValue()); // não deve existir quando divide=false

    // Confere que a listagem responde (RestAssured GET está estourando NPE neste
    // setup)
    URI uri = URI.create("http://localhost:" + port + "/api/lancamentos");
    HttpRequest request = HttpRequest.newBuilder(uri)
        .header("Accept", "application/json")
        .GET()
        .build();

    HttpResponse<String> response;
    try {
      response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode());
  }

  @Test
  void deveRetornar400QuandoSomaDevedoresDiferenteDoTotal() {

    int nataliaId = criaPessoa("Natalia");
    int neiaId = criaPessoa("Neia");

    // total 400, mas devedores somam 300 -> deve dar 400
    String body = """
        {
          "descricao": "Supermercado",
          "data": "2026-01-17",
          "valor": 400,
          "pagadorId": %d,
          "divide": false,
          "devedores": [
            { "pessoaId": %d, "valor": 300 }
          ]
        }
        """.formatted(nataliaId, neiaId);

    given().contentType(ContentType.JSON)
        .body(body)
        .when().post("/api/lancamentos")
        .then()
        .statusCode(400)
        // se seu handler usa outro campo, troque aqui
        .body("mensagem", containsString("soma"));
  }
}
