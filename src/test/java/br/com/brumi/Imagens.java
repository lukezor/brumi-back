package br.com.brumi;

import java.util.Arrays;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Arquivos de teste. So os primeiros bytes (a assinatura) importam para a validacao, entao montamos
 * "imagens" minimas em vez de guardar arquivos binarios no repositorio.
 */
public final class Imagens {

	public static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
	public static final byte[] ASSINATURA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
	public static final byte[] ASSINATURA_WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
	public static final byte[] ASSINATURA_GIF = {'G', 'I', 'F', '8', '9', 'a'};

	private Imagens() {
	}

	/** Assinatura + "conteudo" qualquer, totalizando "tamanho" bytes. */
	public static byte[] bytes(byte[] assinatura, int tamanho) {
		byte[] dados = Arrays.copyOf(assinatura, Math.max(tamanho, assinatura.length));
		Arrays.fill(dados, assinatura.length, dados.length, (byte) 7);
		return dados;
	}

	public static MockMultipartFile png(String nome) {
		return arquivo(nome, "image/png", bytes(ASSINATURA_PNG, 100));
	}

	public static MockMultipartFile jpeg(String nome) {
		return arquivo(nome, "image/jpeg", bytes(ASSINATURA_JPEG, 100));
	}

	public static MockMultipartFile webp(String nome) {
		return arquivo(nome, "image/webp", bytes(ASSINATURA_WEBP, 100));
	}

	public static MockMultipartFile gif(String nome) {
		return arquivo(nome, "image/gif", bytes(ASSINATURA_GIF, 100));
	}

	/** O campo do formulario se chama "imagens", como no endpoint. */
	public static MockMultipartFile arquivo(String nome, String contentType, byte[] conteudo) {
		return new MockMultipartFile("imagens", nome, contentType, conteudo);
	}
}
