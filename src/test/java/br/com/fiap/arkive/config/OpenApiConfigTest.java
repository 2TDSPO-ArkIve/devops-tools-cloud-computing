package br.com.fiap.arkive.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenApiConfigTest {

	@Test
	void normalizaSortPageableParaStringSimplesSemSintaxeJsonArray() {
		OpenAPI openApi = new OpenAPI()
				.path("/api/consultas", new PathItem().get(new Operation()
						.addParametersItem(new QueryParameter()
								.name("sort")
								.schema(new ArraySchema().items(new StringSchema().example("string")))
								.explode(true))));

		new OpenApiConfig().pageableQueryParameterCustomizer().customise(openApi);

		var sort = openApi.getPaths().get("/api/consultas").getGet().getParameters().get(0);
		assertEquals("query", sort.getIn());
		assertEquals("string", sort.getSchema().getType());
		assertEquals("dataHora,desc", sort.getSchema().getExample());
		assertEquals("form", sort.getStyle().toString());
		assertFalse(sort.getExplode());
		assertFalse(sort.getSchema() instanceof ArraySchema);
		assertFalse(sort.toString().contains("[\"string\"]"));
		assertFalse(sort.toString().contains("[]"));
	}

}
