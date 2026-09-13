package br.com.fiap.arkive.bootstrap;

import br.com.fiap.arkive.dto.request.UsuarioRequest;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local-nodb")
public class InitialSysAdminBootstrap implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(InitialSysAdminBootstrap.class);

	private final BootstrapSysAdminProperties properties;
	private final UsuarioRepository usuarioRepository;
	private final UsuarioService usuarioService;

	public InitialSysAdminBootstrap(
			BootstrapSysAdminProperties properties,
			UsuarioRepository usuarioRepository,
			UsuarioService usuarioService
	) {
		this.properties = properties;
		this.usuarioRepository = usuarioRepository;
		this.usuarioService = usuarioService;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.isEnabled()) {
			LOGGER.info("Initial SysAdmin bootstrap disabled.");
			return;
		}

		if (usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S") > 0) {
			LOGGER.info("Active SysAdmin already exists; initial bootstrap skipped.");
			return;
		}

		validateConfiguration();
		usuarioService.criar(new UsuarioRequest(
				properties.getName(),
				TipoUsuario.SYSADMIN,
				properties.getLogin(),
				properties.getPassword(),
				null,
				null,
				null,
				"S"
		));
		LOGGER.info("Initial SysAdmin created successfully.");
	}

	private void validateConfiguration() {
		validateRequired(properties.getName(), "name", "ARKIVE_BOOTSTRAP_SYSADMIN_NAME");
		validateRequired(properties.getLogin(), "login", "ARKIVE_BOOTSTRAP_SYSADMIN_LOGIN");
		validateRequired(properties.getPassword(), "password", "ARKIVE_BOOTSTRAP_SYSADMIN_PASSWORD");
	}

	private void validateRequired(String value, String propertyName, String environmentVariable) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Initial SysAdmin bootstrap requires " + propertyName
					+ " to be configured with " + environmentVariable + ".");
		}
	}

}
