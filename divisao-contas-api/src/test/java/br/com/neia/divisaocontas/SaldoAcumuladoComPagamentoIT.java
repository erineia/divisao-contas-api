package br.com.neia.divisaocontas;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SaldoAcumuladoComPagamentoIT {

  @LocalServerPort
  int port;

  @BeforeEach
  void setup() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  private static record PessoaCriada(int id, String nome) {
  }

  private PessoaCriada criaPessoa(String nomeBase) {
    String nomeUnico = nomeBase + "-" + UUID.randomUUID();
    int id = given().contentType(ContentType.JSON)
        .body("{\"nome\":\"" + nomeUnico + "\"}")
        .when().post("/api/pessoas")
        .then().statusCode(201)
        .extract().path("id");

    return new PessoaCriada(id, nomeUnico);
  }

  @Test
  void deveAcumularSaldoEntreMesesEAbaterComPagamento() {

    PessoaCriada natalia = criaPessoa("Natalia");
    PessoaCriada neia = criaPessoa("Neia");

    // 1) Janeiro: Natalia paga 500 e Neia fica devendo 500 (emprestimo)
    String lancJan = """
        {
          "descricao": "Mercado Jan",
          "data": "2026-01-10",
          "valor": 500,
          "pagadorId": %d,
          "divide": false,
          "devedores": [
            { "pessoaId": %d, "valor": 500 }
          ]
        }
        """.formatted(natalia.id(), neia.id());

    given().contentType(ContentType.JSON)
        .body(lancJan)
        .when().post("/api/lancamentos")
        .then().statusCode(201);

    // 2) Consulta acumulado em Janeiro (Neia deve 500 para Natalia)
    // endpoint retorna TransferenciaResponse: [{devedor, credor, valor}]
    String bodyJan = getJson("/api/saldos/acumulado?ateAno=2026&ateMes=1");
    assertTransferencia(bodyJan, neia.nome(), natalia.nome(), new BigDecimal("500.00"));

    // 3) Fevereiro: Neia paga 600 para Natalia (quita 500 e sobra 100)
    // ⚠️ AJUSTE os campos do JSON conforme seu PagamentoCreateRequest
    String pagFev = """
        {
          "data": "2026-02-05",
          "valor": 600,
          "pagadorId": %d,
          "recebedorId": %d
        }
        """.formatted(neia.id(), natalia.id());

    given().contentType(ContentType.JSON)
        .body(pagFev)
        .when().post("/api/pagamentos")
        .then().statusCode(anyOf(is(200), is(201)));

    // 4) Consulta acumulado em Fevereiro:
    // pagou 600, devia 500 => zera a dívida Neia->Natalia e cria crédito invertido
    // Natalia->Neia de 100
    String bodyFev = getJson("/api/saldos/acumulado?ateAno=2026&ateMes=2");
    assertTransferencia(bodyFev, natalia.nome(), neia.nome(), new BigDecimal("100.00"));
  }

  private String getJson(String pathAndQuery) {
    URI uri = URI.create("http://localhost:" + port + pathAndQuery);
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

    assertEquals(200, response.statusCode(), "GET " + pathAndQuery + " deveria retornar 200");
    return response.body();
  }

  private void assertTransferencia(String jsonArrayBody, String devedor, String credor, BigDecimal valorEsperado) {
    List<Object> valores = JsonPath.read(
        jsonArrayBody,
        "$[?(@.devedor == '" + devedor + "' && @.credor == '" + credor + "')].valor");

    assertNotNull(valores);
    assertEquals(1, valores.size(), "Deveria existir exatamente 1 transferência para o par devedor/credor");

    BigDecimal valor = new BigDecimal(valores.get(0).toString()).setScale(2);
    assertEquals(valorEsperado.setScale(2), valor);
  }
}
