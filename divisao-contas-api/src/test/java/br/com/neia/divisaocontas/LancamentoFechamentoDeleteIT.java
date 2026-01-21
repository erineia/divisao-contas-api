package br.com.neia.divisaocontas;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import com.jayway.jsonpath.JsonPath;
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
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LancamentoFechamentoDeleteIT {

  @LocalServerPort
  int port;

  private String token;

  @BeforeEach
  void setup() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    token = TestAuth.token(port);
  }

  private io.restassured.specification.RequestSpecification auth() {
    return given().auth().oauth2(token);
  }

  @Test
  void deveBloquearExclusaoDeLancamentoQuandoMesFechado() {

    String nataliaNome = "Natalia-" + UUID.randomUUID();
    int nataliaId = auth().contentType(ContentType.JSON)
        .body("{\"nome\":\"" + nataliaNome + "\"}")
        .when().post("/api/pessoas")
        .then().statusCode(201)
        .extract().path("id");

    String neiaNome = "Neia-" + UUID.randomUUID();
    int neiaId = auth().contentType(ContentType.JSON)
        .body("{\"nome\":\"" + neiaNome + "\"}")
        .when().post("/api/pessoas")
        .then().statusCode(201)
        .extract().path("id");

    // cria lançamento em 01/2099
    String criarBody = """
        {
          "descricao": "Compras Mercado",
        "data": "2099-01-15",
          "valor": 400,
          "pagadorId": %d,
          "divide": true,
          "participantesIds": [%d]
        }
        """.formatted(nataliaId, neiaId);

    int lancamentoId = auth().contentType(ContentType.JSON)
        .body(criarBody)
        .when().post("/api/lancamentos")
        .then().statusCode(201)
        .extract().path("id");

    // fecha 01/2099
    auth()
        .when()
        .post("/api/fechamentos?ano=2099&mes=1")
        .then()
        .statusCode(201);

    // tenta excluir (deve falhar 400)
    URI uri = URI.create("http://localhost:" + port + "/api/lancamentos/" + lancamentoId);

    HttpRequest request = HttpRequest.newBuilder(uri)
        .header("Authorization", "Bearer " + token)
        .header("Accept", "application/json")
        .DELETE()
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
