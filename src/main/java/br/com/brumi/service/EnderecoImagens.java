package br.com.brumi.service;

import br.com.brumi.config.ArmazenamentoProperties;
import org.springframework.stereotype.Component;

/**
 * Monta o endereco publico de uma foto a partir do nome guardado no banco ("3f2a....png").
 *
 * - Sem url-publica: "/uploads/3f2a....png" (relativo; o site busca pela API, como sempre).
 * - Com url-publica: "https://fotos.exemplo.com/3f2a....png" (absoluto; o site busca direto no armazenamento).
 */
@Component
public class EnderecoImagens {

	public static final String PREFIXO_API = "/uploads/";

	private final String prefixo;

	public EnderecoImagens(ArmazenamentoProperties props) {
		String publica = props.urlPublica() == null ? "" : props.urlPublica().trim();
		if (publica.isEmpty()) {
			this.prefixo = PREFIXO_API;
		}
		else {
			this.prefixo = publica.endsWith("/") ? publica : publica + "/";
		}
	}

	public String de(String nomeArquivo) {
		return prefixo + nomeArquivo;
	}
}
