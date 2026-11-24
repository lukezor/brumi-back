package br.com.brumi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Monta o "verificador de usuario e senha" usado pelo login.
 *
 * DaoAuthenticationProvider: busca o usuario (UserDetailsService) e compara a senha com o hash
 * (PasswordEncoder). Ele tambem se protege de um ataque sutil: quando o usuario NAO existe, ainda assim
 * executa um BCrypt, para a resposta demorar o mesmo tempo e ninguem descobrir quais usuarios existem.
 *
 * Fica separado do SecurityConfig para que os testes de controller (@WebMvcTest), que nao carregam os
 * services, nao precisem dele.
 */
@Configuration
public class AutenticacaoConfig {

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService usuarios, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(usuarios);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}
}
