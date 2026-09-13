package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.UsuarioRequest;
import br.com.fiap.arkive.dto.request.UsuarioEditRequest;
import br.com.fiap.arkive.dto.response.UsuarioContextOption;
import br.com.fiap.arkive.dto.response.UsuarioResponse;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("!local-nodb")
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final ResponsavelRepository responsavelRepository;
	private final VeterinarioRepository veterinarioRepository;
	private final ClinicaRepository clinicaRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(
			UsuarioRepository usuarioRepository,
			ResponsavelRepository responsavelRepository,
			VeterinarioRepository veterinarioRepository,
			ClinicaRepository clinicaRepository,
			PasswordEncoder passwordEncoder
	) {
		this.usuarioRepository = usuarioRepository;
		this.responsavelRepository = responsavelRepository;
		this.veterinarioRepository = veterinarioRepository;
		this.clinicaRepository = clinicaRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UsuarioResponse criar(UsuarioRequest request) {
		return criarInterno(request, false);
	}

	@Transactional
	public UsuarioResponse criarProvisionado(UsuarioRequest request) {
		return criarInterno(request, true);
	}

	private UsuarioResponse criarInterno(UsuarioRequest request, boolean trocaSenhaObrigatoria) {
		validarCamposObrigatorios(request);
		validarSNQuandoInformado(request.ativo(), "Ativo");
		String login = normalizarLogin(request.login());
		validarLoginDisponivel(login);
		AssociacoesUsuario associacoes = validarAssociacoes(request, null);
		Usuario usuario = new Usuario();
		usuario.setNome(request.nome());
		usuario.setTipo(request.tipo());
		usuario.setLogin(login);
		usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
		usuario.setResponsavel(associacoes.responsavel());
		usuario.setVeterinario(associacoes.veterinario());
		usuario.setClinica(associacoes.clinica());
		usuario.setAtivo(request.ativo() == null ? "S" : request.ativo());
		usuario.setTrocaSenha(trocaSenhaObrigatoria ? "S" : "N");
		return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
	}

	@Transactional(readOnly = true)
	public Page<UsuarioResponse> listar(Pageable pageable) {
		return listarPorTipos(List.of(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<UsuarioResponse> listarPorTipos(List<TipoUsuario> tipos, Pageable pageable) {
		if (tipos == null || tipos.isEmpty()) {
			return usuarioRepository.findAll(pageable).map(UsuarioResponse::fromEntity);
		}
		return usuarioRepository.findByTipoIn(tipos, pageable).map(UsuarioResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorId(Long id) {
		return UsuarioResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorLogin(String login) {
		return UsuarioResponse.fromEntity(usuarioRepository.findByLogin(login)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado.")));
	}

	@Transactional
	public void desativar(Long id, Long usuarioAtualId) {
		Usuario usuario = buscarEntidade(id);
		if (usuarioAtualId != null && usuarioAtualId.equals(usuario.getId())) {
			throw new BusinessException("Voce nao pode desativar sua propria conta.");
		}
		if ("N".equals(usuario.getAtivo())) {
			throw new BusinessException("Usuario ja esta inativo.");
		}
		if (TipoUsuario.SYSADMIN.equals(usuario.getTipo()) && usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S") <= 1) {
			throw new BusinessException("Nao e possivel desativar o ultimo SysAdmin ativo.");
		}
		usuario.setAtivo("N");
		usuarioRepository.save(usuario);
	}

	@Transactional
	public void ativar(Long id) {
		Usuario usuario = buscarEntidade(id);
		if ("S".equals(usuario.getAtivo())) {
			throw new BusinessException("Usuario ja esta ativo.");
		}
		usuario.setAtivo("S");
		usuarioRepository.save(usuario);
	}

	@Transactional
	public UsuarioResponse atualizar(Long id, UsuarioEditRequest request, Long usuarioAtualId) {
		validarCamposObrigatoriosEdicao(request);
		Usuario usuario = buscarEntidade(id);
		validarProtecaoAlteracaoPerfil(usuario, request.tipo(), usuarioAtualId);
		String login = normalizarLogin(request.login());
		validarLoginDisponivelParaEdicao(login, id);
		AssociacoesUsuario associacoes = validarAssociacoes(request, id);
		usuario.setNome(request.nome());
		usuario.setLogin(login);
		usuario.setTipo(request.tipo());
		usuario.setResponsavel(associacoes.responsavel());
		usuario.setVeterinario(associacoes.veterinario());
		usuario.setClinica(associacoes.clinica());
		return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
	}

	@Transactional(readOnly = true)
	public List<UsuarioContextOption> listarClinicasAtivas() {
		return clinicaRepository.findByAtivoOrderByNomeAsc("S").stream()
				.map(clinica -> new UsuarioContextOption(clinica.getId(), clinica.getNome()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<UsuarioContextOption> listarVeterinariosAtivos() {
		return veterinarioRepository.findByAtivoOrderByNomeAsc("S").stream()
				.map(veterinario -> new UsuarioContextOption(veterinario.getId(), veterinario.getNome()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<UsuarioContextOption> listarVeterinariosDisponiveisParaCriacao() {
		return veterinarioRepository.findAtivosSemUsuarioOrderByNomeAsc().stream()
				.map(veterinario -> new UsuarioContextOption(veterinario.getId(), veterinario.getNome()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<UsuarioContextOption> listarVeterinariosDisponiveisParaEdicao(Long usuarioId) {
		return veterinarioRepository.findDisponiveisParaUsuarioOrderByNomeAsc(usuarioId).stream()
				.map(veterinario -> new UsuarioContextOption(veterinario.getId(), veterinario.getNome()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<UsuarioContextOption> listarResponsaveisAtivos() {
		return responsavelRepository.findByAtivoOrderByNomeAsc("S").stream()
				.map(responsavel -> new UsuarioContextOption(responsavel.getId(), responsavel.getNome()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<UsuarioContextOption> listarResponsaveisDisponiveisParaCriacao() {
		return responsavelRepository.findAtivosSemUsuarioOrderByNomeAsc().stream()
				.map(responsavel -> new UsuarioContextOption(responsavel.getId(), responsavel.getNome()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<UsuarioContextOption> listarResponsaveisDisponiveisParaEdicao(Long usuarioId) {
		return responsavelRepository.findDisponiveisParaUsuarioOrderByNomeAsc(usuarioId).stream()
				.map(responsavel -> new UsuarioContextOption(responsavel.getId(), responsavel.getNome()))
				.toList();
	}

	@Transactional(readOnly = true)
	public void validarCriacao(UsuarioRequest request) {
		validarCamposObrigatorios(request);
		validarSNQuandoInformado(request.ativo(), "Ativo");
		validarLoginDisponivel(normalizarLogin(request.login()));
		validarAssociacoes(request, null);
	}

	@Transactional(readOnly = true)
	public void validarEdicao(Long id, UsuarioEditRequest request, Long usuarioAtualId) {
		validarCamposObrigatoriosEdicao(request);
		Usuario usuario = buscarEntidade(id);
		validarProtecaoAlteracaoPerfil(usuario, request.tipo(), usuarioAtualId);
		validarLoginDisponivelParaEdicao(normalizarLogin(request.login()), id);
		validarAssociacoes(request, id);
	}

	private String normalizarLogin(String login) {
		return login == null ? null : login.trim();
	}

	private Usuario buscarEntidade(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
	}

	private void validarCamposObrigatorios(UsuarioRequest request) {
		if (request.tipo() == null) {
			throw new BusinessException("Tipo de usuario e obrigatorio.");
		}
		if (request.nome() == null || request.nome().isBlank()) {
			throw new BusinessException("Nome do usuario e obrigatorio.");
		}
		if (request.login() == null || request.login().isBlank()) {
			throw new BusinessException("Login do usuario e obrigatorio.");
		}
		if (request.senha() == null || request.senha().isBlank()) {
			throw new BusinessException("Senha do usuario e obrigatoria.");
		}
	}

	private void validarCamposObrigatoriosEdicao(UsuarioEditRequest request) {
		if (request.tipo() == null) {
			throw new BusinessException("Tipo de usuario e obrigatorio.");
		}
		if (request.nome() == null || request.nome().isBlank()) {
			throw new BusinessException("Nome do usuario e obrigatorio.");
		}
		if (request.login() == null || request.login().isBlank()) {
			throw new BusinessException("Login do usuario e obrigatorio.");
		}
	}

	private void validarLoginDisponivel(String login) {
		if (usuarioRepository.existsByLogin(login)) {
			throw new BusinessException("Login de usuario ja cadastrado.");
		}
	}

	private void validarLoginDisponivelParaEdicao(String login, Long usuarioId) {
		if (usuarioRepository.existsByLoginAndIdNot(login, usuarioId)) {
			throw new BusinessException("Login de usuario ja cadastrado.");
		}
	}

	private AssociacoesUsuario validarAssociacoes(UsuarioRequest request, Long usuarioIdIgnorado) {
		return validarAssociacoes(request.tipo(), request.responsavelId(), request.veterinarioId(), request.clinicaId(), usuarioIdIgnorado);
	}

	private AssociacoesUsuario validarAssociacoes(UsuarioEditRequest request, Long usuarioIdIgnorado) {
		return validarAssociacoes(request.tipo(), request.responsavelId(), request.veterinarioId(), request.clinicaId(), usuarioIdIgnorado);
	}

	private AssociacoesUsuario validarAssociacoes(TipoUsuario tipo, Long responsavelId, Long veterinarioId, Long clinicaId, Long usuarioIdIgnorado) {
		return switch (tipo) {
			case SYSADMIN -> validarSysadmin(responsavelId, veterinarioId, clinicaId);
			case ADMIN_CLINICA -> validarAdminClinica(responsavelId, veterinarioId, clinicaId);
			case VETERINARIO -> validarVeterinario(responsavelId, veterinarioId, clinicaId, usuarioIdIgnorado);
			case RESPONSAVEL -> validarResponsavel(responsavelId, veterinarioId, clinicaId, usuarioIdIgnorado);
		};
	}

	private AssociacoesUsuario validarSysadmin(Long responsavelId, Long veterinarioId, Long clinicaId) {
		if (responsavelId != null || veterinarioId != null || clinicaId != null) {
			throw new BusinessException("Usuario SYSADMIN nao deve ter responsavel, veterinario ou clinica associados.");
		}
		return new AssociacoesUsuario(null, null, null);
	}

	private AssociacoesUsuario validarAdminClinica(Long responsavelId, Long veterinarioId, Long clinicaId) {
		if (clinicaId == null) {
			throw new BusinessException("Usuario ADMIN_CLINICA deve estar associado a uma clinica.");
		}
		if (responsavelId != null || veterinarioId != null) {
			throw new BusinessException("Usuario ADMIN_CLINICA nao deve ter responsavel ou veterinario associados.");
		}
		Clinica clinica = validarClinicaAtiva(clinicaId);
		return new AssociacoesUsuario(null, null, clinica);
	}

	private AssociacoesUsuario validarVeterinario(Long responsavelId, Long veterinarioId, Long clinicaId, Long usuarioIdIgnorado) {
		if (veterinarioId == null) {
			throw new BusinessException("Usuario VETERINARIO deve estar associado a um veterinario.");
		}
		if (responsavelId != null || clinicaId != null) {
			throw new BusinessException("Usuario VETERINARIO nao deve ter responsavel ou clinica associados.");
		}
		validarVeterinarioDisponivel(veterinarioId, usuarioIdIgnorado);
		Veterinario veterinario = validarVeterinarioAtivo(veterinarioId);
		return new AssociacoesUsuario(null, veterinario, null);
	}

	private AssociacoesUsuario validarResponsavel(Long responsavelId, Long veterinarioId, Long clinicaId, Long usuarioIdIgnorado) {
		if (responsavelId == null) {
			throw new BusinessException("Usuario RESPONSAVEL deve estar associado a um responsavel.");
		}
		if (veterinarioId != null || clinicaId != null) {
			throw new BusinessException("Usuario RESPONSAVEL nao deve ter veterinario ou clinica associados.");
		}
		validarResponsavelDisponivel(responsavelId, usuarioIdIgnorado);
		Responsavel responsavel = validarResponsavelAtivo(responsavelId);
		return new AssociacoesUsuario(responsavel, null, null);
	}

	private void validarVeterinarioDisponivel(Long veterinarioId, Long usuarioIdIgnorado) {
		boolean emUso = usuarioIdIgnorado == null
				? usuarioRepository.existsByVeterinarioId(veterinarioId)
				: usuarioRepository.existsByVeterinarioIdAndIdNot(veterinarioId, usuarioIdIgnorado);
		if (emUso) {
			throw new BusinessException("Veterinario ja esta associado a outro usuario.");
		}
	}

	private void validarResponsavelDisponivel(Long responsavelId, Long usuarioIdIgnorado) {
		boolean emUso = usuarioIdIgnorado == null
				? usuarioRepository.existsByResponsavelId(responsavelId)
				: usuarioRepository.existsByResponsavelIdAndIdNot(responsavelId, usuarioIdIgnorado);
		if (emUso) {
			throw new BusinessException("Responsavel ja esta associado a outro usuario.");
		}
	}

	private void validarProtecaoAlteracaoPerfil(Usuario usuario, TipoUsuario novoTipo, Long usuarioAtualId) {
		boolean mudandoSysadmin = TipoUsuario.SYSADMIN.equals(usuario.getTipo()) && !TipoUsuario.SYSADMIN.equals(novoTipo);
		if (!mudandoSysadmin) {
			return;
		}
		if (usuarioAtualId != null && usuarioAtualId.equals(usuario.getId())) {
			throw new BusinessException("Voce nao pode alterar seu proprio perfil SYSADMIN.");
		}
		if ("S".equals(usuario.getAtivo()) && usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S") <= 1) {
			throw new BusinessException("Nao e possivel alterar o perfil do ultimo SysAdmin ativo.");
		}
	}

	private Responsavel validarResponsavelAtivo(Long id) {
		Responsavel responsavel = responsavelRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado."));
		if (!"S".equals(responsavel.getAtivo())) {
			throw new BusinessException("Responsavel associado ao usuario deve estar ativo.");
		}
		return responsavel;
	}

	private Veterinario validarVeterinarioAtivo(Long id) {
		Veterinario veterinario = veterinarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Veterinario nao encontrado."));
		if (!"S".equals(veterinario.getAtivo())) {
			throw new BusinessException("Veterinario associado ao usuario deve estar ativo.");
		}
		return veterinario;
	}

	private Clinica validarClinicaAtiva(Long id) {
		Clinica clinica = clinicaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada."));
		if (!"S".equals(clinica.getAtivo())) {
			throw new BusinessException("Clinica associada ao usuario deve estar ativa.");
		}
		return clinica;
	}

	private void validarSNQuandoInformado(String valor, String campo) {
		if (valor != null && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException(campo + " deve ser S ou N.");
		}
	}

	private record AssociacoesUsuario(Responsavel responsavel, Veterinario veterinario, Clinica clinica) {
	}

}
