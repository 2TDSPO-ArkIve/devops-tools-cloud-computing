package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.UsuarioProvisioningRequest;
import br.com.fiap.arkive.dto.request.UsuarioRequest;
import br.com.fiap.arkive.dto.response.AccountProvisioningResult;
import br.com.fiap.arkive.dto.response.UsuarioResponse;
import br.com.fiap.arkive.exception.BusinessException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Service
@Profile("!local-nodb")
public class AccountProvisioningService {

	private static final int MAX_INITIAL_CREDENTIAL_BYTES = 72;
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	private final UsuarioService usuarioService;

	public AccountProvisioningService(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@Transactional
	public AccountProvisioningResult provisionar(UsuarioProvisioningRequest request) {
		String loginNormalizado = normalizarLogin(request.login());
		validarTamanhoCredencialInicial(loginNormalizado);
		UsuarioRequest requestProvisionado = new UsuarioRequest(
				request.nome(),
				request.tipo(),
				loginNormalizado,
				loginNormalizado,
				request.responsavelId(),
				request.veterinarioId(),
				request.clinicaId(),
				"S"
		);
		UsuarioResponse usuario = usuarioService.criarProvisionado(requestProvisionado);
		return new AccountProvisioningResult(usuario);
	}

	private String normalizarLogin(String login) {
		return login == null ? null : login.trim();
	}

	private void validarTamanhoCredencialInicial(String login) {
		if (login == null || login.isBlank()) {
			throw new BusinessException("E-mail/login e obrigatorio para criar a conta de acesso.");
		}
		if (!EMAIL_PATTERN.matcher(login).matches()) {
			throw new BusinessException("E-mail/login deve ter formato valido.");
		}
		if (login.getBytes(StandardCharsets.UTF_8).length > MAX_INITIAL_CREDENTIAL_BYTES) {
			throw new BusinessException("E-mail/login deve ter no maximo 72 bytes para uso como credencial inicial.");
		}
	}
}
