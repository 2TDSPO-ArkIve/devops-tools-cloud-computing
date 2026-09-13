package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.ClinicaRequest;
import br.com.fiap.arkive.dto.request.UsuarioProvisioningRequest;
import br.com.fiap.arkive.dto.response.ClinicaResponse;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.ClinicaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class ClinicaService {

	private final ClinicaRepository clinicaRepository;
	private final AccountProvisioningService accountProvisioningService;

	public ClinicaService(ClinicaRepository clinicaRepository, AccountProvisioningService accountProvisioningService) {
		this.clinicaRepository = clinicaRepository;
		this.accountProvisioningService = accountProvisioningService;
	}

	@Transactional
	@CacheEvict(value = "clinicas", allEntries = true)
	public ClinicaResponse criar(ClinicaRequest request) {
		validarEmailObrigatorioParaCriacao(request.email());
		Clinica clinica = new Clinica();
		aplicarDados(clinica, request, true);
		Clinica salva = clinicaRepository.save(clinica);
		accountProvisioningService.provisionar(new UsuarioProvisioningRequest(
				salva.getNome(),
				TipoUsuario.ADMIN_CLINICA,
				salva.getEmail(),
				null,
				null,
				salva.getId()
		));
		return ClinicaResponse.fromEntity(salva);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "clinicas", key = "'listar:' + (#nome == null ? '' : #nome) + ':' + (#ativo == null ? '' : #ativo) + ':' + #pageable")
	public Page<ClinicaResponse> listar(String nome, String ativo, Pageable pageable) {
		validarAtivoQuandoInformado(ativo);
		return clinicaRepository.buscar(vazioParaNulo(nome), vazioParaNulo(ativo), pageable).map(ClinicaResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "clinicas", key = "'buscarTexto:' + (#busca == null ? '' : #busca) + ':' + (#ativo == null ? '' : #ativo) + ':' + #pageable")
	public Page<ClinicaResponse> listarPorTexto(String busca, String ativo, Pageable pageable) {
		validarAtivoQuandoInformado(ativo);
		return clinicaRepository.buscarPorTexto(vazioParaNulo(busca), vazioParaNulo(ativo), pageable).map(ClinicaResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "clinicas", key = "'id:' + #id")
	public ClinicaResponse buscarPorId(Long id) {
		return ClinicaResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	@CacheEvict(value = "clinicas", allEntries = true)
	public ClinicaResponse atualizar(Long id, ClinicaRequest request) {
		Clinica clinica = buscarEntidade(id);
		aplicarDados(clinica, request, false);
		return ClinicaResponse.fromEntity(clinicaRepository.save(clinica));
	}

	@Transactional
	@CacheEvict(value = "clinicas", allEntries = true)
	public void excluir(Long id) {
		Clinica clinica = buscarEntidade(id);
		clinica.setAtivo("N");
		clinicaRepository.save(clinica);
	}

	@Transactional(readOnly = true)
	public Clinica buscarEntidade(Long id) {
		return clinicaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada."));
	}

	private void aplicarDados(Clinica clinica, ClinicaRequest request, boolean criando) {
		String ativo = criando && request.ativo() == null ? "S" : request.ativo();
		validarAtivoQuandoInformado(ativo);
		clinica.setNome(request.nome());
		clinica.setCnpj(request.cnpj());
		clinica.setEndereco(request.endereco());
		clinica.setTelefone(request.telefone());
		clinica.setEmail(request.email());
		clinica.setAtivo(ativo == null ? clinica.getAtivo() : ativo);
	}

	private void validarAtivoQuandoInformado(String valor) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException("Ativo deve ser S ou N.");
		}
	}

	private void validarEmailObrigatorioParaCriacao(String email) {
		if (email == null || email.isBlank()) {
			throw new BusinessException("E-mail da clinica e obrigatorio para criar a conta de acesso.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
