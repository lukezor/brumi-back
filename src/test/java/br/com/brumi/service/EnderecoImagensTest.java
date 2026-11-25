package br.com.brumi.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.brumi.config.ArmazenamentoProperties;
import org.junit.jupiter.api.Test;

class EnderecoImagensTest {

	@Test
	void semUrlPublicaUsaOCaminhoDaApiEComUrlPublicaUsaOEnderecoAbsoluto() {
		assertThat(new EnderecoImagens(new ArmazenamentoProperties("gcs", "", null)).de("a.png"))
			.isEqualTo("/uploads/a.png");
		assertThat(new EnderecoImagens(new ArmazenamentoProperties("gcs", "https://fotos.exemplo.com", null)).de("a.png"))
			.isEqualTo("https://fotos.exemplo.com/a.png");
	}
}
