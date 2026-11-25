package br.com.brumi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Para testes que verificam o CONTRATO da API (filtros, JSON, erros), e nao a seguranca:
 * toda requisicao do MockMvc passa a sair "logada como admin".
 *
 * jwt() simula um token ja validado (nao passa pelo login nem pela assinatura). A seguranca de verdade
 * e testada em AutenticacaoApiTest.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ComoAdminConfig {

	@Bean
	MockMvcBuilderCustomizer requisicoesComoAdmin() {
		return builder -> builder.defaultRequest(get("/")
			.with(jwt().jwt(j -> j.subject("admin-teste")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));
	}
}
