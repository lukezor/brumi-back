package br.com.brumi.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Prova que a aplicacao NAO SOBE sem uma chave JWT valida.
 *
 * ApplicationContextRunner monta um mini-contexto Spring so com o que pedimos (aqui, as propriedades de
 * seguranca). E rapido e serve exatamente para testar configuracao.
 */
class SegurancaPropertiesTest {

	@Configuration
	@EnableConfigurationProperties(SegurancaProperties.class)
	static class Config {
	}

	private final ApplicationContextRunner contexto = new ApplicationContextRunner()
		.withUserConfiguration(Config.class)
		.withPropertyValues("brumi.seguranca.cors-origens=http://localhost:4200");

	@Test
	void naoSobeSemSegredo() {
		// Igual a aplicacao real sem JWT_SECRET: o application.yaml define "segredo: ${JWT_SECRET:}" (vazio).
		contexto.withPropertyValues("brumi.seguranca.jwt.segredo=").run(ctx -> {
			assertThat(ctx).hasFailed();
			assertThat(ctx.getStartupFailure()).rootCause()
				.hasMessageContaining("jwt.segredo")
				.hasMessageContaining("JWT_SECRET");
		});
		// Mesmo sem nenhuma propriedade jwt, a mensagem aponta o que fazer.
		contexto.run(ctx -> assertThat(ctx.getStartupFailure()).rootCause().hasMessageContaining("JWT_SECRET"));
	}

	@Test
	void naoSobeComSegredoCurto() {
		contexto.withPropertyValues("brumi.seguranca.jwt.segredo=curto-demais").run(ctx -> {
			assertThat(ctx).hasFailed();
			assertThat(ctx.getStartupFailure()).rootCause().hasMessageContaining("pelo menos 32 caracteres");
		});
	}

	@Test
	void sobeComSegredoValidoEUsaValidadePadrao() {
		contexto.withPropertyValues("brumi.seguranca.jwt.segredo=" + "x".repeat(32)).run(ctx -> {
			assertThat(ctx).hasNotFailed();
			assertThat(ctx.getBean(SegurancaProperties.class).jwt().expiracaoMinutos()).isEqualTo(1440);
		});
	}
}
