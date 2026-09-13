package br.com.fiap.arkive.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class OpenApiConfig {

	private static final String BASIC_AUTH = "basicAuth";

	@Bean
	public OpenAPI arkiveOpenAPI() {
		return new OpenAPI()
				.components(new Components()
						.addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("basic")
								.description("HTTP Basic com usuarios ArkIve autenticados pelo Spring Security.")))
				.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
				.info(new Info()
						.title("ArkIve API")
						.description("API REST para a jornada clinica veterinaria do ArkIve. O suporte por IA oferece apoio investigativo e nao substitui o julgamento nem a confirmacao do veterinario.")
						.version("0.0.1-SNAPSHOT"))
				.addTagsItem(new Tag().name("Consultas").description("Cadastro, leitura e fluxo clinico das consultas."))
				.addTagsItem(new Tag().name("Suporte Clinico").description("Suporte clinico por IA, sempre provisorio e solicitado por veterinario."))
				.addTagsItem(new Tag().name("Diagnosticos").description("Diagnosticos e conclusoes clinicas dentro do escopo autorizado."))
				.addTagsItem(new Tag().name("Prescricoes").description("Prescricoes criadas exclusivamente por veterinarios apos consulta finalizada."))
				.addTagsItem(new Tag().name("Adesao").description("Registro de adesao terapeutica pelo responsavel vinculado ao animal."))
				.addTagsItem(new Tag().name("Animais").description("Animais acompanhados pelo ArkIve."))
				.addTagsItem(new Tag().name("Veterinarios").description("Profissionais veterinarios."))
				.addTagsItem(new Tag().name("Responsaveis").description("Responsaveis por animais."))
				.addTagsItem(new Tag().name("Clinicas").description("Clinicas e contexto administrativo."));
	}

	@Bean
	public OpenApiCustomizer pageableQueryParameterCustomizer() {
		return openApi -> {
			if (openApi.getPaths() == null) {
				return;
			}
			openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
				if (operation.getParameters() == null) {
					return;
				}
				operation.getParameters().forEach(this::normalizarParametroPageable);
			}));
		};
	}

	private void normalizarParametroPageable(Parameter parameter) {
		if (!"query".equals(parameter.getIn())) {
			return;
		}
		if ("page".equals(parameter.getName())) {
			parameter
					.schema(new IntegerSchema().minimum(BigDecimal.ZERO).example(0))
					.required(false)
					.description("Pagina solicitada, iniciando em 0.");
			return;
		}
		if ("size".equals(parameter.getName())) {
			parameter
					.schema(new IntegerSchema().minimum(BigDecimal.ONE).example(20))
					.required(false)
					.description("Quantidade de itens por pagina.");
			return;
		}
		if ("sort".equals(parameter.getName())) {
			parameter
					.schema(new StringSchema().example("dataHora,desc"))
					.required(false)
					.description("Ordenacao no formato propriedade,direcao. Ex.: dataHora,desc")
					.style(Parameter.StyleEnum.FORM)
					.explode(false)
					.content(null);
		}
	}

}
