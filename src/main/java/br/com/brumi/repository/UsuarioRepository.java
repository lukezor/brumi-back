package br.com.brumi.repository;

import br.com.brumi.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	/** Query derivada: select ... from usuario where username = ? */
	Optional<Usuario> findByUsername(String username);

	boolean existsByUsername(String username);
}
