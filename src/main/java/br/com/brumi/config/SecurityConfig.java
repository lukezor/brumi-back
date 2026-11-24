package br.com.brumi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança TEMPORÁRIA (será reescrita na Task 7 com JWT).
 *
 * Só de ter o Spring Security no projeto, TODAS as rotas passam a exigir login.
 * Por isso liberamos explicitamente o que precisa ser público agora.
 *
 * Um SecurityFilterChain é uma cadeia de filtros que roda ANTES do controller,
 * igual a um middleware no FastAPI/Express.
 */
@Configuration
public class SecurityConfig {

	// @Bean: o Spring chama este método uma vez e guarda o resultado para injetar onde for preciso.
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// CSRF protege formulários com cookie de sessão. Em API com token no header não se aplica.
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/ping", "/actuator/health", "/actuator/health/**").permitAll()
				.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults());
		return http.build();
	}
}
