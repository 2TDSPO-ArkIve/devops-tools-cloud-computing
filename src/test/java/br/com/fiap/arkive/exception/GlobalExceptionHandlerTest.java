package br.com.fiap.arkive.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

	@Test
	void accessDeniedRetornaForbidden() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/consultas/1/finalizar");

		var response = handler.handleAccessDenied(new AccessDeniedException("Usuario nao autorizado."), request);

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertEquals(403, response.getBody().status());
		assertEquals("Usuario nao autorizado.", response.getBody().message());
		assertEquals("/api/consultas/1/finalizar", response.getBody().path());
	}

	@Test
	void validacaoRetornaMapaDeCampos() throws NoSuchMethodException {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/prescricoes");
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "prescricaoRequest");
		bindingResult.addError(new FieldError("prescricaoRequest", "medicamento", "nao deve estar em branco"));
		MethodParameter parameter = new MethodParameter(
				GlobalExceptionHandlerTest.class.getDeclaredMethod("prescricaoRequest", String.class),
				0
		);

		var response = handler.handleValidation(new MethodArgumentNotValidException(parameter, bindingResult), request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(400, response.getBody().status());
		assertEquals("nao deve estar em branco", response.getBody().fields().get("medicamento"));
		assertEquals("/api/prescricoes", response.getBody().path());
	}

	@Test
	void metodoNaoSuportadoRetornaMethodNotAllowed() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/adesoes-prescricao/80");

		var response = handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("PUT"), request);

		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
		assertEquals(405, response.getBody().status());
		assertEquals("Metodo HTTP nao suportado para este recurso.", response.getBody().message());
		assertEquals("/api/adesoes-prescricao/80", response.getBody().path());
	}

	@Test
	void propriedadeDeOrdenacaoInvalidaRetornaBadRequestControlado() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/consultas");

		var response = handler.handlePropertyReference(mock(PropertyReferenceException.class), request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(400, response.getBody().status());
		assertEquals("Parametro de ordenacao invalido.", response.getBody().message());
		assertEquals("/api/consultas", response.getBody().path());
	}

	@SuppressWarnings("unused")
	private void prescricaoRequest(String medicamento) {
	}

}
