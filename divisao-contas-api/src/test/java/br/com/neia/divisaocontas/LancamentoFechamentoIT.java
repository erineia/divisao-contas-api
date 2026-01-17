package br.com.neia.divisaocontas;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LancamentoFechamentoIT {

  @LocalServerPort
  int port;

  @BeforeEach
  void setup() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  @Test
  void deveBloquearCriacaoDeLancamentoQuandoMesFechado() {

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

    // 2) fecha 01/2026
    given()
        .when()
        .post("/api/fechamentos?ano=2026&mes=1")
        .then()
        .statusCode(201);

    // 3) tenta criar lançamento em janeiro/2026 (deve falhar 400)
    String body = """
        {
          "descricao": "Compras Mercado",
          "data": "2026-01-15",
          "valor": 400,
          "pagadorId": %d,
          "divide": true,
          "participantesIds": [%d]
        }
        """.formatted(nataliaId, neiaId);

    given().contentType(ContentType.JSON)
        .body(body)
        .when().post("/api/lancamentos")
        .then()
        .statusCode(400)
        .body("mensagem", containsString("soma"))
        .log().all()
        .statusCode(400);

  }
}
