package br.com.brumi.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.brumi.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @WebMvcTest sobe só a camada web (controllers + segurança), sem banco. É rápido.
 * MockMvc simula requisições HTTP sem abrir uma porta de verdade.
 */
@WebMvcTest(PingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PingControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void pingRetornaOk() throws Exception {
		mockMvc.perform(get("/api/ping"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void rotaDesconhecidaExigeAutenticacao() throws Exception {
		mockMvc.perform(get("/api/qualquer-coisa"))
			.andExpect(status().isUnauthorized());
	}
}
