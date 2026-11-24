package br.com.brumi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.brumi.TestcontainersConfiguration;
import br.com.brumi.config.SecurityConfig;
import br.com.brumi.domain.Usuario;
import br.com.brumi.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seguranca DE VERDADE: login real, tokens reais, filtros reais. Nada simulado.
 * O admin "admin-teste" e criado pelo AdminInicializador na subida (ver application-test.yaml).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AutenticacaoApiTest {

	private static final String CATEGORIA_JSON = "{\"nome\":\"Categoria do teste\"}";

	@Autowired
	MockMvc mvc;
	@Autowired
	JwtEncoder jwtEncoder;
	@Autowired
	UsuarioRepository usuarios;
	@Autowired
	PasswordEncoder passwordEncoder;

	private String login(String username, String password) throws Exception {
		String corpo = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
		return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
			.andReturn().getResponse().getContentAsString();
	}

	private String tokenDoAdmin() throws Exception {
		return JsonPath.read(login("admin-teste", "senha-teste-123"), "$.accessToken");
	}

	/** Monta um token com os dados que quisermos, assinado com a chave do projeto. */
	private String token(String emissor, String papel, Instant emitidoEm, Instant expiraEm) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(emissor).subject("admin-teste").issuedAt(emitidoEm).expiresAt(expiraEm)
			.claim(SecurityConfig.CLAIM_PAPEL, papel).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	private org.springframework.test.web.servlet.ResultActions criarCategoriaCom(String token) throws Exception {
		return mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_JSON)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	// ---------- admin inicial ----------

	@Test
	void adminInicialFoiCriadoComSenhaEmHashBcrypt() {
		Usuario admin = usuarios.findByUsername("admin-teste").orElseThrow();

		assertThat(admin.getSenhaHash()).startsWith("$2a$").isNotEqualTo("senha-teste-123");
		assertThat(passwordEncoder.matches("senha-teste-123", admin.getSenhaHash())).isTrue();
		assertThat(admin.getPapel().name()).isEqualTo("ADMIN");
	}

	// ---------- login ----------

	@Test
	void loginValidoDevolveTokenBearerComValidadeDe24Horas() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin-teste\",\"password\":\"senha-teste-123\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(86400))
			.andExpect(jsonPath("$.accessToken").isString());
	}

	@Test
	void senhaErradaEUsuarioInexistenteDaoAMesmaResposta401() throws Exception {
		for (String[] credenciais : new String[][] {{"admin-teste", "senha-errada"}, {"ninguem", "qualquer-coisa"}}) {
			mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"" + credenciais[0] + "\",\"password\":\"" + credenciais[1] + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.mensagem").value("Usuário ou senha inválidos"))
				.andExpect(jsonPath("$.accessToken").doesNotExist());
		}
	}

	@Test
	void loginComCamposVaziosDevolve400() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.length()").value(2));
	}

	// ---------- acesso as rotas ----------

	@Test
	void leituraContinuaPublica() throws Exception {
		mvc.perform(get("/api/produtos")).andExpect(status().isOk());
		mvc.perform(get("/api/categorias")).andExpect(status().isOk());
		mvc.perform(get("/api/ping")).andExpect(status().isOk());
	}

	@Test
	void escritaSemTokenDevolve401() throws Exception {
		mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
			.andExpect(jsonPath("$.mensagem").value("Autenticação necessária"));
		mvc.perform(put("/api/produtos/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/produtos/1"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void escritaComTokenDoLoginFunciona() throws Exception {
		criarCategoriaCom(tokenDoAdmin())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nome").value("Categoria do teste"));
	}

	@Test
	void tokenAdulteradoDevolve401() throws Exception {
		String valido = tokenDoAdmin();
		// Troca o ultimo caractere da assinatura: o conteudo nao bate mais com a assinatura.
		String adulterado = valido.substring(0, valido.length() - 1) + (valido.endsWith("A") ? "B" : "A");

		criarCategoriaCom(adulterado)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado"));
		criarCategoriaCom("isto-nao-e-um-jwt").andExpect(status().isUnauthorized());
	}

	@Test
	void tokenExpiradoDevolve401() throws Exception {
		Instant ha2Horas = Instant.now().minus(Duration.ofHours(2));
		String expirado = token(SecurityConfig.EMISSOR, "ADMIN", ha2Horas, ha2Horas.plus(Duration.ofHours(1)));

		criarCategoriaCom(expirado)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado"));
	}

	@Test
	void tokenDeOutroEmissorDevolve401() throws Exception {
		Instant agora = Instant.now();
		criarCategoriaCom(token("site-falso", "ADMIN", agora, agora.plus(Duration.ofHours(1))))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenAssinadoComOutraChaveDevolve401() throws Exception {
		JwtEncoder outraChave = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
			"uma-chave-diferente-com-mais-de-32-caracteres".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
		Instant agora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(SecurityConfig.EMISSOR).subject("admin-teste")
			.issuedAt(agora).expiresAt(agora.plus(Duration.ofHours(1))).claim(SecurityConfig.CLAIM_PAPEL, "ADMIN").build();
		String falsificado = outraChave.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();

		criarCategoriaCom(falsificado).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenValidoSemPapelDeAdminDevolve403() throws Exception {
		Instant agora = Instant.now();
		criarCategoriaCom(token(SecurityConfig.EMISSOR, "CLIENTE", agora, agora.plus(Duration.ofHours(1))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.mensagem").value("Você não tem permissão para esta operação"));
	}

	/**
	 * Comportamento que o Angular precisa conhecer (Task 11): se um token INVALIDO ou EXPIRADO for enviado,
	 * o filtro o recusa ANTES de olhar as regras, mesmo numa rota publica. Sem token, a rota publica funciona.
	 */
	@Test
	void tokenInvalidoEmRotaPublicaTambemDevolve401() throws Exception {
		mvc.perform(get("/api/produtos").header(HttpHeaders.AUTHORIZATION, "Bearer token-expirado-ou-falso"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void rotaNaoMapeadaSemTokenDevolve401() throws Exception {
		mvc.perform(get("/api/nao-existe")).andExpect(status().isUnauthorized());
	}

	// ---------- CORS ----------

	@Test
	void corsPermiteOAngularEBloqueiaOutrasOrigens() throws Exception {
		mvc.perform(options("/api/categorias")
				.header(HttpHeaders.ORIGIN, "http://localhost:4200")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));

		mvc.perform(options("/api/categorias")
				.header(HttpHeaders.ORIGIN, "http://site-malicioso.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}
