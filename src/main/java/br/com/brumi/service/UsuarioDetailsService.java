package br.com.brumi.service;

import br.com.brumi.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponte entre a NOSSA tabela de usuarios e o Spring Security.
 * O Spring Security nao conhece a entidade Usuario: ele trabalha com a interface UserDetails.
 * Esta classe busca o Usuario no banco e o converte.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository repository;

	public UsuarioDetailsService(UsuarioRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		return repository.findByUsername(username)
			.map(u -> User.withUsername(u.getUsername())
				.password(u.getSenhaHash())
				.roles(u.getPapel().name())   // vira a autoridade ROLE_ADMIN
				.disabled(!u.isAtivo())
				.build())
			// A mensagem fica no log interno. Para o cliente, sempre "Usuário ou senha inválidos".
			.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
	}
}
