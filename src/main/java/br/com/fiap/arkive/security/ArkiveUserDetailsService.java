package br.com.fiap.arkive.security;

import br.com.fiap.arkive.repository.UsuarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class ArkiveUserDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public ArkiveUserDetailsService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		String identificador = username == null ? "" : username.trim();
		return usuarioRepository.findByLoginIgnoreCase(identificador)
				.or(() -> usuarioRepository.findVeterinarioByCrmvIgnoreCase(identificador))
				.map(UsuarioPrincipal::fromEntity)
				.orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas."));
	}

}
