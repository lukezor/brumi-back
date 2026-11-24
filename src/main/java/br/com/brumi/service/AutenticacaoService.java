package br.com.brumi.service;

import br.com.brumi.config.SecurityConfig;
import br.com.brumi.config.SegurancaProperties;
import br.com.brumi.dto.LoginRequest;
import br.com.brumi.dto.LoginResponse;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Login: confere usuario e senha e emite o token JWT. (≈ authenticate_user + create_access_token do auth.py.) */
@Service
public class AutenticacaoService {

	private static final Logger log = LoggerFactory.getLogger(AutenticacaoService.class);

	private final AuthenticationManager authenticationManager;
	private final JwtEncoder jwtEncoder;
	private final Duration validade;

	public AutenticacaoService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder,
			SegurancaProperties props) {
		this.authenticationManager = authenticationManager;
		this.jwtEncoder = jwtEncoder;
		this.validade = Duration.ofMinutes(props.jwt().expiracaoMinutos());
	}

	public LoginResponse login(LoginRequest request) {
		Authentication autenticado;
		try {
			// Se usuario ou senha estiverem errados, lanca BadCredentialsException (uma AuthenticationException).
			autenticado = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.username().trim(), request.password()));
		}
		catch (AuthenticationException ex) {
			log.info("Falha de login para o usuário '{}': {}", request.username(), ex.getClass().getSimpleName());
			throw ex; // o GlobalExceptionHandler transforma em 401
		}
		return new LoginResponse(gerarToken(autenticado), "Bearer", validade.toSeconds());
	}

	/**
	 * Um JWT tem 3 partes separadas por ponto: cabecalho.conteudo.assinatura (cada uma em Base64URL).
	 * O conteudo (claims) NAO e criptografado, so assinado: qualquer um le, ninguem altera sem a chave.
	 * Por isso nunca coloque dados sensiveis no token.
	 */
	private String gerarToken(Authentication autenticado) {
		Instant agora = Instant.now();
		String papel = autenticado.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.filter(a -> a.startsWith("ROLE_"))
			.map(a -> a.substring("ROLE_".length()))
			.findFirst()
			.orElseThrow();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(SecurityConfig.EMISSOR)              // iss: quem emitiu
			.subject(autenticado.getName())              // sub: de quem e o token
			.issuedAt(agora)                             // iat: quando foi emitido
			.expiresAt(agora.plus(validade))             // exp: quando expira
			.claim(SecurityConfig.CLAIM_PAPEL, papel)    // papel: ADMIN
			.build();
		JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
	}
}
