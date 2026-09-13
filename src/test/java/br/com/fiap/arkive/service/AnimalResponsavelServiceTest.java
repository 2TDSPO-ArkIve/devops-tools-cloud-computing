package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AnimalResponsavelRequest;
import br.com.fiap.arkive.entity.*;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.*;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AnimalResponsavelServiceTest {
	private final AnimalResponsavelRepository vinculos = mock(AnimalResponsavelRepository.class);
	private final AnimalRepository animais = mock(AnimalRepository.class);
	private final ResponsavelRepository responsaveis = mock(ResponsavelRepository.class);
	private final ConsultaRepository consultas = mock(ConsultaRepository.class);
	private final VeterinarioService veterinarios = mock(VeterinarioService.class);
	private final ClinicalAccessService access = new ClinicalAccessService(vinculos, consultas, veterinarios);
	private final AnimalResponsavelService service = new AnimalResponsavelService(vinculos, animais, responsaveis, mock(EventoJornadaService.class), access);
	private Animal animal;
	private Responsavel tutor;
	private final LocalDate hoje = LocalDate.now();
	private final UsuarioPrincipal vet = principal(TipoUsuario.VETERINARIO, null, 10L, null);

	@BeforeEach void setup() {
		animal = new Animal(); animal.setId(1L); animal.setNome("Paciente");
		Veterinario dono = new Veterinario(); dono.setId(10L); animal.setVeterinarioCadastro(dono);
		tutor = new Responsavel(); tutor.setId(20L); tutor.setNome("Tutor");
		when(animais.findById(1L)).thenReturn(Optional.of(animal));
		when(animais.buscarParaAtualizarVinculos(1L)).thenReturn(Optional.of(animal));
		when(responsaveis.findById(20L)).thenReturn(Optional.of(tutor));
		when(vinculos.save(any())).thenAnswer(i -> i.getArgument(0));
	}
	@Test void zeroTutoresEhValido() {
		assertTrue(service.listarAtivosPorAnimal(1L, vet).isEmpty());
		verify(vinculos, never()).save(any());
	}
	@Test void veterinarioCadastranteSemClinicaPodeVincularEListar() {
		var response = service.criar(request("S"), vet);
		assertEquals(20L, response.responsavelId()); assertEquals("S", response.principal());
		verify(animais).buscarParaAtualizarVinculos(1L);
		when(vinculos.listarPorAnimalEAtivo(1L, "S")).thenReturn(List.of(vinculo(20L)));
		assertEquals(1, service.listarAtivosPorAnimal(1L, vet).size());
	}
	@Test void veterinarioNaoAcessaNemModificaPacienteAlheio() {
		var outro = principal(TipoUsuario.VETERINARIO, null, 99L, null);
		assertThrows(AccessDeniedException.class, () -> service.listarAtivosPorAnimal(1L, outro));
		assertThrows(AccessDeniedException.class, () -> service.criar(request("S"), outro));
		assertThrows(AccessDeniedException.class, () -> service.atualizar(request("S"), outro));
		assertThrows(AccessDeniedException.class, () -> service.encerrar(request("S"), outro));
		assertThrows(AccessDeniedException.class, () -> service.excluir(1L, 20L, hoje, outro));
		verify(animais, never()).buscarParaAtualizarVinculos(any());
		verify(vinculos, never()).save(any());
	}
	@Test void novoPrincipalRebaixaAnteriorDentroDaTransacao() {
		AnimalResponsavel anterior = vinculo(21L);
		when(vinculos.listarPrincipaisAtivos(1L, "S", "S")).thenReturn(List.of(anterior));
		service.criar(request("S"), vet);
		assertEquals("N", anterior.getPrincipal());
		var ordem = inOrder(animais, vinculos);
		ordem.verify(animais).buscarParaAtualizarVinculos(1L);
		ordem.verify(vinculos).listarPrincipaisAtivos(1L, "S", "S");
		ordem.verify(vinculos).saveAll(List.of(anterior));
		ordem.verify(vinculos).save(any());
	}
	@Test void atualizacaoPrincipalTambemRebaixaAnterior() {
		AnimalResponsavel atual = vinculo(20L), anterior = vinculo(21L);
		when(vinculos.findById(atual.getId())).thenReturn(Optional.of(atual));
		when(vinculos.listarPrincipaisAtivos(1L, "S", "S")).thenReturn(List.of(atual, anterior));
		service.atualizar(request("S"), vet);
		assertEquals("S", atual.getPrincipal()); assertEquals("N", anterior.getPrincipal());
	}
	@Test void veterinarioPodeEncerrarERemoverVinculo() {
		AnimalResponsavel atual = vinculo(20L);
		when(vinculos.findById(atual.getId())).thenReturn(Optional.of(atual));
		var encerramento = new AnimalResponsavelRequest(1L, 20L, "TUTOR_LEGAL", hoje, hoje, "S", "S");
		assertEquals("N", service.encerrar(encerramento, vet).ativo());
		atual.setAtivo("S"); service.excluir(1L, 20L, hoje, vet); assertEquals("N", atual.getAtivo());
	}
	@Test void sysadminEAdminDaClinicaPodemGerenciar() {
		service.criar(request("N"), principal(TipoUsuario.SYSADMIN, null, null, null));
		Clinica clinica = new Clinica(); clinica.setId(30L); animal.setClinica(clinica);
		service.criar(request("N"), principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L));
		assertThrows(AccessDeniedException.class, () -> service.criar(request("N"), principal(TipoUsuario.ADMIN_CLINICA, null, null, 31L)));
	}
	@Test void responsavelLeSeusVinculosMasNaoConcedeAcesso() {
		var principal = principal(TipoUsuario.RESPONSAVEL, 20L, null, null);
		when(vinculos.existsVinculoAtivoVigente(eq(1L), eq(20L), any())).thenReturn(true);
		assertTrue(service.listarAtivosPorAnimal(1L, principal).isEmpty());
		when(vinculos.buscar(null, 20L, null, null, Pageable.unpaged())).thenReturn(Page.empty());
		service.listar(null, null, null, null, Pageable.unpaged(), principal);
		verify(vinculos).buscar(null, 20L, null, null, Pageable.unpaged());
		assertThrows(AccessDeniedException.class, () -> service.criar(request("S"), principal));
		assertThrows(AccessDeniedException.class, () -> service.listarPorResponsavel(21L, Pageable.unpaged(), principal));
	}
	@Test void vetEAdminPrecisamAnimalNaListagemGeral() {
		assertThrows(BusinessException.class, () -> service.listar(null, null, null, null, Pageable.unpaged(), vet));
		assertThrows(AccessDeniedException.class, () -> service.listarPorResponsavel(20L, Pageable.unpaged(), vet));
	}
	@Test void tutorInativoNaoPodeSerVinculado() {
		tutor.setAtivo("N"); assertThrows(BusinessException.class, () -> service.criar(request("N"), vet));
	}
	@Test void listaAtivosIgnoraVinculosFuturosEExpirados() {
		AnimalResponsavel futuro = vinculo(20L), expirado = vinculo(21L);
		futuro.getId().setDataInicio(hoje.plusDays(1)); expirado.setDataFim(hoje.minusDays(1));
		when(vinculos.listarPorAnimalEAtivo(1L, "S")).thenReturn(List.of(futuro, expirado));
		assertTrue(service.listarAtivosPorAnimal(1L, vet).isEmpty());
	}
	private AnimalResponsavelRequest request(String principal) {
		return new AnimalResponsavelRequest(1L, 20L, "TUTOR_LEGAL", hoje, null, principal, "S");
	}
	private AnimalResponsavel vinculo(Long responsavelId) {
		AnimalResponsavelId id = new AnimalResponsavelId(); id.setAnimalId(1L); id.setResponsavelId(responsavelId); id.setDataInicio(hoje);
		AnimalResponsavel ar = new AnimalResponsavel(); ar.setId(id); ar.setAnimal(animal); ar.setResponsavel(tutor); ar.setPrincipal("S"); ar.setTipoVinculo("TUTOR_LEGAL"); return ar;
	}
	private static UsuarioPrincipal principal(TipoUsuario tipo, Long responsavel, Long vet, Long clinica) {
		return new UsuarioPrincipal(1L, "Usuario", "user@example.test", "hash", tipo, "S", false, responsavel, vet, clinica);
	}
}
