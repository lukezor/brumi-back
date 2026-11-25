package br.com.brumi.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos de imagem aceitos, reconhecidos pela ASSINATURA do arquivo ("magic bytes"): os primeiros bytes
 * de cada formato sao sempre os mesmos. Isso e mais confiavel que o Content-Type ou a extensao, que o
 * cliente pode declarar como quiser.
 *
 * SVG fica de fora de proposito: SVG e texto e pode conter JavaScript (risco de XSS ao ser aberto).
 */
public enum TipoImagem {

	PNG("png", "image/png"),
	JPEG("jpg", "image/jpeg"),
	WEBP("webp", "image/webp");

	/** Quantos bytes iniciais precisamos ler para reconhecer qualquer um dos formatos. */
	public static final int BYTES_DA_ASSINATURA = 12;

	private final String extensao;
	private final String mimeType;

	TipoImagem(String extensao, String mimeType) {
		this.extensao = extensao;
		this.mimeType = mimeType;
	}

	public String extensao() {
		return extensao;
	}

	public String mimeType() {
		return mimeType;
	}

	public static Optional<TipoImagem> detectar(byte[] inicio) {
		return Arrays.stream(values()).filter(t -> t.reconhece(inicio)).findFirst();
	}

	private boolean reconhece(byte[] b) {
		return switch (this) {
			// 89 50 4E 47 0D 0A 1A 0A  ("\x89PNG\r\n\x1a\n")
			case PNG -> comeca(b, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
			// FF D8 FF
			case JPEG -> comeca(b, 0, 0xFF, 0xD8, 0xFF);
			// "RIFF" + 4 bytes de tamanho + "WEBP"
			case WEBP -> comeca(b, 0, 'R', 'I', 'F', 'F') && comeca(b, 8, 'W', 'E', 'B', 'P');
		};
	}

	private static boolean comeca(byte[] dados, int deslocamento, int... esperado) {
		if (dados.length < deslocamento + esperado.length) {
			return false;
		}
		for (int i = 0; i < esperado.length; i++) {
			if ((dados[deslocamento + i] & 0xFF) != esperado[i]) {
				return false;
			}
		}
		return true;
	}
}
