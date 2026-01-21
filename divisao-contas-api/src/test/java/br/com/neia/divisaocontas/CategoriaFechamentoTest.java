package br.com.neia.divisaocontas;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CategoriaFechamentoTest {

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
  void deveIsolarFechamentoPorCategoriaEManterDefaultMesXX() {

    // pessoas
    int pagadorId = auth().contentType(ContentType.JSON)
        .body("{\"nome\":\"Natalia\"}")
        .when().post("/api/pessoas")
        .then().statusCode(201)
        .extract().path("id");

    int participanteId = auth().contentType(ContentType.JSON)
        .body("{\"nome\":\"Neia\"}")
        .when().post("/api/pessoas")
        .then().statusCode(201)
        .extract().path("id");

    // categorias
    int contasMesId = auth().contentType(ContentType.JSON)
        .body("{\"nome\":\"Contas Mês\"}")
        .when().post("/api/categorias")
        .then().statusCode(201)
        .extract().path("id");

    int viagemId = auth().contentType(ContentType.JSON)
        .body("{\"nome\":\"Viagem Salvador\"}")
        .when().post("/api/categorias")
        .then().statusCode(201)
        .extract().path("id");

    // 1) fecha 01/2099 somente para categoria "Contas Mês"
    auth()
        .when()
        .post("/api/fechamentos?ano=2100&mes=1&categoriaId=" + contasMesId)
        .then()
        .statusCode(201);

    // 2) lançamento na categoria fechada deve falhar
    String lancamentoFechado = """
        {
          \"descricao\": \"Compras Mercado\",
          \"data\": \"2100-01-15\",
          \"valor\": 400,
          \"pagadorId\": %d,
          \"categoriaId\": %d,
          \"divide\": true,
          \"participantesIds\": [%d]
        }
        """.formatted(pagadorId, contasMesId, participanteId);

    auth().contentType(ContentType.JSON)
        .body(lancamentoFechado)
        .when().post("/api/lancamentos")
        .then()
        .statusCode(400)
        .body("mensagem", containsString("mês está fechado"));

    // 3) mesmo mês, outra categoria deve passar
    String lancamentoOutraCategoria = """
        {
          \"descricao\": \"Almoço Viagem\",
          \"data\": \"2100-01-20\",
          \"valor\": 200,
          \"pagadorId\": %d,
          \"categoriaId\": %d,
          \"divide\": true,
          \"participantesIds\": [%d]
        }
        """.formatted(pagadorId, viagemId, participanteId);

    auth().contentType(ContentType.JSON)
        .body(lancamentoOutraCategoria)
        .when().post("/api/lancamentos")
        .then()
        .statusCode(201);

    // 4) default Mes/02: fecha sem categoriaId
    auth()
        .when()
        .post("/api/fechamentos?ano=2100&mes=2")
        .then()
        .statusCode(201);

    // 5) lançamento sem categoriaId (default Mes/02) deve falhar
    String lancamentoDefaultMes = """
        {
          \"descricao\": \"Conta Água\",
          \"data\": \"2100-02-10\",
          \"valor\": 100,
          \"pagadorId\": %d,
          \"divide\": true,
          \"participantesIds\": [%d]
        }
        """.formatted(pagadorId, participanteId);

    auth().contentType(ContentType.JSON)
        .body(lancamentoDefaultMes)
        .when().post("/api/lancamentos")
        .then()
        .statusCode(400)
        .body("mensagem", containsString("mês está fechado"));

    // 6) mesmo mês, mas categoria explicitamente diferente deve passar
    String lancamentoMes02OutraCategoria = """
        {
          \"descricao\": \"Passeio Viagem\",
          \"data\": \"2100-02-12\",
          \"valor\": 80,
          \"pagadorId\": %d,
          \"categoriaId\": %d,
          \"divide\": true,
          \"participantesIds\": [%d]
        }
        """.formatted(pagadorId, viagemId, participanteId);

    auth().contentType(ContentType.JSON)
        .body(lancamentoMes02OutraCategoria)
        .when().post("/api/lancamentos")
        .then()
        .statusCode(201);
  }
}
