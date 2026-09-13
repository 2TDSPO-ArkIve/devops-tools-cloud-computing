package br.com.fiap.arkive.service;

import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ResponsavelLookupServiceTest {
	private final ResponsavelRepository repository = mock(ResponsavelRepository.class);
	private final ResponsavelService service = new ResponsavelService(repository);
	@ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "ab"})
	void naoPermiteEnumeracaoSemBusca(String busca) {
		assertThrows(BusinessException.class, () -> service.buscarParaVinculo(busca, Pageable.unpaged()));
		verifyNoInteractions(repository);
	}
	@Test void limitaPaginaEOrdenacao() {
		when(repository.buscarParaVinculo(eq("Ana"), any())).thenReturn(Page.empty());
		service.buscarParaVinculo(" Ana ", PageRequest.of(2, 500));
		verify(repository).buscarParaVinculo(eq("Ana"), argThat(p -> p.getPageSize() == 20 && p.getPageNumber() == 2 && p.getSort().getOrderFor("nome") != null));
	}
}
