package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.PasswordResetResult;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.PasswordPolicy;
import br.com.fiap.arkive.security.TemporaryPasswordGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Profile("!local-nodb")
public class PasswordLifecycleService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicy passwordPolicy;
	private final TemporaryPasswordGenerator temporaryPasswordGenerator;

	public PasswordLifecycleService(
			UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder,
			PasswordPolicy passwordPolicy,
			TemporaryPasswordGenerator temporaryPasswordGenerator
	) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.temporaryPasswordGenerator = temporaryPasswordGenerator;
	}

	@Transactional
	public Usuario alterarSenhaObrigatoria(Long usuarioId, String novaSenha, String confirmacao) {
		Usuario usuario = buscarUsuario(usuarioId);
		validarNovaSenha(novaSenha, confirmacao);
		substituirSenha(usuario, novaSenha, "N");
		return usuarioRepository.save(usuario);
	}

	@Transactional
	public Usuario alterarSenhaAutenticado(Long usuarioId, String senhaAtual, String novaSenha, String confirmacao) {
		Usuario usuario = buscarUsuario(usuarioId);
		if (senhaAtual == null || senhaAtual.isBlank()) {
			throw new BusinessException("Informe a senha atual.");
		}
		if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
			throw new BusinessException("Senha atual invalida.");
		}
		validarNovaSenha(novaSenha, confirmacao);
		substituirSenha(usuario, novaSenha, "N");
		return usuarioRepository.save(usuario);
	}

	@Transactional
	public PasswordResetResult resetarSenha(Long usuarioId) {
		Usuario usuario = buscarUsuario(usuarioId);
		String senhaTemporaria = temporaryPasswordGenerator.gerar();
		substituirSenha(usuario, senhaTemporaria, "S");
		usuarioRepository.save(usuario);
		return new PasswordResetResult(usuario.getId(), senhaTemporaria);
	}

	private Usuario buscarUsuario(Long usuarioId) {
		return usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
	}

	private void validarNovaSenha(String novaSenha, String confirmacao) {
		passwordPolicy.validar(novaSenha);
		passwordPolicy.validarConfirmacao(novaSenha, confirmacao);
	}

	private void substituirSenha(Usuario usuario, String senha, String trocaSenha) {
		usuario.setSenhaHash(passwordEncoder.encode(senha));
		usuario.setTrocaSenha(trocaSenha);
		usuario.setDataUltimaTrocaSenha(LocalDateTime.now());
	}
}
