package br.com.brumi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Usuario administrador inicial (brumi.admin.*), vindo das variaveis ADMIN_USERNAME e ADMIN_PASSWORD.
 * Ambos opcionais: sem eles, nenhum admin e criado (util quando o admin ja existe no banco).
 */
@ConfigurationProperties(prefix = "brumi.admin")
public record AdminProperties(String username, String password) {

	public boolean configurado() {
		return username != null && !username.isBlank() && password != null && !password.isBlank();
	}
}
