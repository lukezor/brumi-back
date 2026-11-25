package br.com.brumi.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracoes de seguranca, lidas de "brumi.seguranca.*" no application.yaml.
 *
 * @ConfigurationProperties: o Spring preenche este record com as propriedades de mesmo nome.
 * Ex.: brumi.seguranca.jwt.expiracao-minutos -> jwt().expiracaoMinutos().
 * (≈ uma classe Settings do pydantic-settings, que le e valida o .env.)
 *
 * @Validated: as anotacoes de validacao sao conferidas NA INICIALIZACAO. Se a chave do JWT estiver
 * ausente ou curta, a aplicacao NAO SOBE e mostra qual propriedade esta errada. Isso substitui o
 * "fallback-unsafe-key" do sistema Python, que deixava a API rodar com uma chave conhecida.
 */
@Validated
@ConfigurationProperties(prefix = "brumi.seguranca")
public record SegurancaProperties(
	@NotNull(message = "Defina a variável de ambiente JWT_SECRET (pelo menos 32 caracteres)") @Valid Jwt jwt,
	@NotEmpty(message = "Informe ao menos uma origem permitida no CORS") List<String> corsOrigens) {

	public record Jwt(
		/*
		 * HS256 assina com uma chave secreta compartilhada. A especificacao exige pelo menos 256 bits
		 * (32 bytes). Com menos, alguem poderia descobrir a chave por forca bruta e emitir tokens de admin.
		 */
		@NotBlank(message = "Defina a variável de ambiente JWT_SECRET (pelo menos 32 caracteres)")
		@Size(min = 32, message = "JWT_SECRET deve ter pelo menos 32 caracteres")
		String segredo,

		@Positive @DefaultValue("1440") long expiracaoMinutos) {
	}
}
