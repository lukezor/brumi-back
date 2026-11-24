package br.com.brumi.service;

import br.com.brumi.config.AdminProperties;
import br.com.brumi.domain.Papel;
import br.com.brumi.domain.Usuario;
import br.com.brumi.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o administrador inicial na subida da aplicacao, a partir de ADMIN_USERNAME e ADMIN_PASSWORD.
 *
 * ApplicationRunner: o Spring executa o metodo run() uma vez, logo depois que a aplicacao terminou de subir
 * (e depois do Flyway, entao a tabela usuario ja existe).
 *
 * So CRIA. Se o usuario ja existe, nada muda, nem a senha. Para trocar a senha em dev, apague a linha da
 * tabela usuario e reinicie a aplicacao.
 */
@Component
public class AdminInicializador implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminInicializador.class);
	private static final int TAMANHO_MINIMO_SENHA = 8;

	private final AdminProperties admin;
	private final UsuarioRepository repository;
	private final PasswordEncoder passwordEncoder;

	public AdminInicializador(AdminProperties admin, UsuarioRepository repository, PasswordEncoder passwordEncoder) {
		this.admin = admin;
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!admin.configurado()) {
			log.warn("ADMIN_USERNAME/ADMIN_PASSWORD não definidos: nenhum administrador foi criado.");
			return;
		}
		String username = admin.username().trim();
		if (repository.existsByUsername(username)) {
			log.info("Administrador '{}' já existe. Nada foi alterado.", username);
			return;
		}
		if (admin.password().length() < TAMANHO_MINIMO_SENHA) {
			throw new IllegalStateException("ADMIN_PASSWORD deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres");
		}
		repository.save(new Usuario(username, passwordEncoder.encode(admin.password()), Papel.ADMIN));
		// Nunca registre a senha no log.
		log.info("Administrador '{}' criado.", username);
	}
}
