package br.com.neia.divisaocontas;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

@OpenAPIDefinition(info = @Info(title = "Divisão de Contas API", version = "0.0.1", description = "API para registrar lançamentos (com divisão automática ou rateio manual), pagamentos e gerar saldos/transferências sugeridas.\n\nPrincipais regras:\n- Fechamento de mês: quando um mês está fechado, não é permitido criar/alterar/excluir lançamentos com data nesse mês.\n- Lançamento com divide=true: o valor é rateado igualmente entre os participantes (pagador incluído).\n- Lançamento com divide=false: é um empréstimo/rateio manual; informe devedores com valores e a soma deve bater exatamente com o valor do lançamento.", contact = @Contact(name = "Equipe Divisão de Contas")), security = {
		@SecurityRequirement(name = "bearerAuth") }, tags = {
				@Tag(name = "Pessoas", description = "Cadastro de pessoas participantes."),
				@Tag(name = "Lançamentos", description = "Criação/edição de lançamentos (despesas) com divisão automática ou rateio manual."),
				@Tag(name = "Pagamentos", description = "Registro de pagamentos (acertos) entre pessoas."),
				@Tag(name = "Fechamentos", description = "Fechamento/reabertura de mês para travar alterações em lançamentos."),
				@Tag(name = "Saldos", description = "Consulta de saldos e transferências sugeridas."),
				@Tag(name = "Relatórios", description = "Exportação de relatórios CSV.")
		})
public class DivisaoContasApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DivisaoContasApiApplication.class, args);
	}

}
