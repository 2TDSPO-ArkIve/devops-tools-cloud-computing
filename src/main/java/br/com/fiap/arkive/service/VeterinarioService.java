package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.UsuarioProvisioningRequest;
import br.com.fiap.arkive.dto.request.VeterinarioRequest;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class VeterinarioService {

	private final VeterinarioRepository veterinarioRepository;
	private final ClinicaService clinicaService;
	private final AccountProvisioningService accountProvisioningService;

	public VeterinarioService(
			VeterinarioRepository veterinarioRepository,
			ClinicaService clinicaService,
			AccountProvisioningService accountProvisioningService
	) {
		this.veterinarioRepository = veterinarioRepository;
		this.clinicaService = clinicaService;
		this.accountProvisioningService = accountProvisioningService;
	}

	@Transactional
	public VeterinarioResponse criar(VeterinarioRequest request) {
		validarEmailObrigatorioParaCriacao(request.email());
		validarCrmvDisponivel(request.crmv());
		Veterinario veterinario = new Veterinario();
		aplicarDados(veterinario, request, true);
		Veterinario salvo = veterinarioRepository.save(veterinario);
		accountProvisioningService.provisionar(new UsuarioProvisioningRequest(
				salvo.getNome(),
				TipoUsuario.VETERINARIO,
				salvo.getEmail(),
				null,
				salvo.getId(),
				null
		));
		return VeterinarioResponse.fromEntity(salvo);
	}

	@Transactional(readOnly = true)
	public Page<VeterinarioResponse> listar(String nome, String crmv, Long clinicaId, String ativo, Pageable pageable) {
		validarAtivoQuandoInformado(ativo);
		return veterinarioRepository.buscar(
				vazioParaNulo(nome),
				vazioParaNulo(crmv),
				clinicaId,
				vazioParaNulo(ativo),
				pageable
		).map(VeterinarioResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public Page<VeterinarioResponse> listarPorTexto(String busca, Long clinicaId, String ativo, Pageable pageable) {
		validarAtivoQuandoInformado(ativo);
		return veterinarioRepository.buscarPorTexto(
				vazioParaNulo(busca),
				clinicaId,
				vazioParaNulo(ativo),
				pageable
		).map(VeterinarioResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public VeterinarioResponse buscarPorId(Long id) {
		return VeterinarioResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public Long buscarClinicaId(Long id) {
		Veterinario veterinario = buscarEntidade(id);
		return veterinario.getClinica() == null ? null : veterinario.getClinica().getId();
	}

	@Transactional
	public VeterinarioResponse atualizar(Long id, VeterinarioRequest request) {
		Veterinario veterinario = buscarEntidade(id);
		aplicarDados(veterinario, request, false);
		return VeterinarioResponse.fromEntity(veterinarioRepository.save(veterinario));
	}

	@Transactional
	public void excluir(Long id) {
		Veterinario veterinario = buscarEntidade(id);
		veterinario.setAtivo("N");
		veterinarioRepository.save(veterinario);
	}

	@Transactional(readOnly = true)
	public Veterinario buscarEntidade(Long id) {
		return veterinarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Veterinario nao encontrado."));
	}

	@Transactional(readOnly = true)
	public Veterinario buscarEntidadeAtiva(Long id) {
		Veterinario veterinario = buscarEntidade(id);
		if (!"S".equals(veterinario.getAtivo())) {
			throw new BusinessException("Veterinario deve estar ativo.");
		}
		return veterinario;
	}

	private void aplicarDados(Veterinario veterinario, VeterinarioRequest request, boolean criando) {
		String ativo = criando && request.ativo() == null ? "S" : request.ativo();
		validarAtivoQuandoInformado(ativo);
		Clinica clinica = request.clinicaId() == null ? null : clinicaService.buscarEntidade(request.clinicaId());
		veterinario.setNome(request.nome());
		veterinario.setCrmv(request.crmv());
		veterinario.setEspecialidade(request.especialidade());
		veterinario.setEmail(request.email());
		veterinario.setClinica(clinica);
		veterinario.setAtivo(ativo == null ? veterinario.getAtivo() : ativo);
	}

	private void validarAtivoQuandoInformado(String valor) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException("Ativo deve ser S ou N.");
		}
	}

	private void validarEmailObrigatorioParaCriacao(String email) {
		if (email == null || email.isBlank()) {
			throw new BusinessException("E-mail do veterinario e obrigatorio para criar a conta de acesso.");
		}
	}

	private void validarCrmvDisponivel(String crmv) {
		if (veterinarioRepository.existsByCrmvIgnoreCase(crmv)) {
			throw new BusinessException("CRMV ja cadastrado.", HttpStatus.CONFLICT);
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
