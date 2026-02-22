package br.com.neia.divisaocontas;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GrupoFechamentoTest {

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
    void deveIsolarFechamentoPorGrupoEManterDefaultMesXX() {

        // pessoas
        String nomePagador = "Natalia-" + UUID.randomUUID();
        int pagadorId = auth().contentType(ContentType.JSON)
                .body("{\"nome\":\"" + nomePagador + "\"}")
                .when().post("/api/pessoas")
                .then().statusCode(201)
                .extract().path("id");

        String nomeParticipante = "Neia-" + UUID.randomUUID();
        int participanteId = auth().contentType(ContentType.JSON)
                .body("{\"nome\":\"" + nomeParticipante + "\"}")
                .when().post("/api/pessoas")
                .then().statusCode(201)
                .extract().path("id");

        // grupos
        int contasMesId = auth().contentType(ContentType.JSON)
                .body("{\"nome\":\"Contas Mês\"}")
                .when().post("/api/grupos")
                .then().statusCode(201)
                .extract().path("id");

        int viagemId = auth().contentType(ContentType.JSON)
                .body("{\"nome\":\"Viagem Salvador\"}")
                .when().post("/api/grupos")
                .then().statusCode(201)
                .extract().path("id");

        // 1) fecha 01/2090 somente para grupo "Contas Mês"
        auth()
                .when()
                .post("/api/fechamentos?ano=2090&mes=1&grupoId=" + contasMesId)
                .then()
                .statusCode(anyOf(is(201), is(409)));

        // 2) lançamento no grupo fechado deve falhar
        String lancamentoFechado = """
                {
                  \"descricao\": \"Compras Mercado\",
                  \"data\": \"2090-01-15\",
                  \"valor\": 400,
                  \"pagadorId\": %d,
                  \"grupoId\": %d,
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

        // 3) mesmo mês, outro grupo deve passar
        String lancamentoOutroGrupo = """
                {
                  \"descricao\": \"Almoço Viagem\",
                  \"data\": \"2090-01-20\",
                  \"valor\": 200,
                  \"pagadorId\": %d,
                  \"grupoId\": %d,
                  \"divide\": true,
                  \"participantesIds\": [%d]
                }
                """.formatted(pagadorId, viagemId, participanteId);

        auth().contentType(ContentType.JSON)
                .body(lancamentoOutroGrupo)
                .when().post("/api/lancamentos")
                .then()
                .statusCode(201);

        // 4) default Mes/02: fecha sem grupoId (grupo default Mes/02)
        auth()
                .when()
                .post("/api/fechamentos?ano=2090&mes=2")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        // 5) lançamento sem grupoId (default Mes/02) deve falhar
        String lancamentoDefaultMes = """
                {
                  \"descricao\": \"Conta Água\",
                  \"data\": \"2090-02-10\",
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

        // 6) mesmo mês, mas grupo explicitamente diferente deve passar
        String lancamentoMes02OutroGrupo = """
                {
                  \"descricao\": \"Passeio Viagem\",
                  \"data\": \"2090-02-12\",
                  \"valor\": 80,
                  \"pagadorId\": %d,
                  \"grupoId\": %d,
                  \"divide\": true,
                  \"participantesIds\": [%d]
                }
                """.formatted(pagadorId, viagemId, participanteId);

        auth().contentType(ContentType.JSON)
                .body(lancamentoMes02OutroGrupo)
                .when().post("/api/lancamentos")
                .then()
                .statusCode(201);
    }
}
