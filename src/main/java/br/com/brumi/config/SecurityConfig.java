package br.com.brumi.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Configuracao de seguranca da API.
 *
 * Como funciona, em resumo:
 * 1. O admin faz POST /api/auth/login com usuario e senha e recebe um token JWT.
 * 2. Nas proximas requisicoes, envia o cabecalho "Authorization: Bearer <token>".
 * 3. Um filtro do Spring Security (o "resource server") confere a assinatura e a validade do token
 *    ANTES de a requisicao chegar ao controller. (≈ get_current_user do auth.py, so que como middleware.)
 * 4. As regras abaixo decidem: leitura publica, escrita so para ROLE_ADMIN.
 */
@Configuration
@EnableConfigurationProperties({SegurancaProperties.class, AdminProperties.class})
public class SecurityConfig {

	/** Valor do campo "iss" (emissor) do token. Tokens de outro emissor sao recusados. */
	public static final String EMISSOR = "brumi-api";

	/** Nome do campo do token que carrega o papel do usuario. */
	public static final String CLAIM_PAPEL = "papel";

	private static final String[] ROTAS_DE_CATALOGO = {
		"/api/categorias", "/api/categorias/**", "/api/produtos", "/api/produtos/**"
	};

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {

		// 401 e 403 sao gerados aqui nos filtros, ANTES dos controllers. Para sairem no mesmo formato JSON
		// dos outros erros, repassamos a excecao ao GlobalExceptionHandler (via o resolver do Spring MVC).
		AuthenticationEntryPoint naoAutenticado = (req, res, ex) -> resolver.resolveException(req, res, null, ex);
		AccessDeniedHandler semPermissao = (req, res, ex) -> resolver.resolveException(req, res, null, ex);

		http
			// CSRF ataca quem usa COOKIE de sessao: o navegador envia o cookie sozinho. Aqui o token vai num
			// cabecalho que o JavaScript precisa colocar de proposito, entao o ataque nao se aplica.
			.csrf(AbstractHttpConfigurer::disable)
			// Sem sessao no servidor: cada requisicao se autentica pelo token (API "stateless").
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// Usa o bean corsConfigurationSource definido abaixo.
			.cors(Customizer.withDefaults())
			.authorizeHttpRequests(auth -> auth
				// As regras sao lidas de cima para baixo. Vale a PRIMEIRA que combinar.
				.requestMatchers("/api/ping", "/actuator/health", "/actuator/health/**", "/error").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.requestMatchers(HttpMethod.GET, ROTAS_DE_CATALOGO).permitAll()
				.requestMatchers(HttpMethod.POST, ROTAS_DE_CATALOGO).hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, ROTAS_DE_CATALOGO).hasRole("ADMIN")
				.requestMatchers(HttpMethod.DELETE, ROTAS_DE_CATALOGO).hasRole("ADMIN")
				// Qualquer outra rota: precisa estar autenticado. (Rota nova esquecida fica protegida, nao aberta.)
				.anyRequest().authenticated())
			.oauth2ResourceServer(rs -> rs
				.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDePapeis()))
				.authenticationEntryPoint(naoAutenticado)
				.accessDeniedHandler(semPermissao))
			.exceptionHandling(e -> e
				.authenticationEntryPoint(naoAutenticado)
				.accessDeniedHandler(semPermissao));
		return http.build();
	}

	/**
	 * Transforma o campo "papel" do token em autoridade do Spring: "ADMIN" -> "ROLE_ADMIN".
	 * E isso que o hasRole("ADMIN") confere.
	 */
	private JwtAuthenticationConverter conversorDePapeis() {
		JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
		papeis.setAuthoritiesClaimName(CLAIM_PAPEL);
		papeis.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
		conversor.setJwtGrantedAuthoritiesConverter(papeis);
		return conversor;
	}

	/** A chave secreta compartilhada por quem assina (encoder) e quem confere (decoder). */
	private static SecretKey chave(SegurancaProperties props) {
		return new SecretKeySpec(props.jwt().segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	/** Assina tokens. Usado no login. */
	@Bean
	JwtEncoder jwtEncoder(SegurancaProperties props) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chave(props)));
	}

	/**
	 * Confere tokens recebidos: assinatura HS256, validade (exp, com 60 s de tolerancia de relogio)
	 * e emissor (iss = brumi-api).
	 */
	@Bean
	JwtDecoder jwtDecoder(SegurancaProperties props) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(props)).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR));
		return decoder;
	}

	/**
	 * BCrypt: hash lento e com "sal" aleatorio. Lento de proposito, para dificultar a forca bruta se o
	 * banco vazar. O mesmo texto gera hashes diferentes a cada vez, e matches() sabe compara-los.
	 * (≈ CryptContext(schemes=["bcrypt"]) do passlib no auth.py.)
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * CORS: o navegador so deixa o Angular (localhost:4200) chamar a API (localhost:8080) se a API autorizar
	 * aquela origem. Antes era "*" (qualquer site). Agora so as origens configuradas.
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(SegurancaProperties props) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(props.corsOrigens());
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		cors.setExposedHeaders(List.of("Location"));
		cors.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cors);
		return source;
	}
}
