package br.com.brumi.service;

import br.com.brumi.config.UploadProperties;
import br.com.brumi.exception.RequisicaoInvalidaException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Confere os arquivos enviados ANTES de qualquer gravacao. Fica separado do armazenamento porque a regra
 * (tamanho e formato) e a mesma, seja o destino o disco local ou o Cloud Storage.
 */
@Component
public class ValidadorImagens {

	private final long tamanhoMaximo;

	public ValidadorImagens(UploadProperties props) {
		this.tamanhoMaximo = props.tamanhoMaximoPorArquivo().toBytes();
	}

	/**
	 * Nao vazio, dentro do tamanho e com assinatura de PNG, JPEG ou WEBP.
	 * O Content-Type e o nome enviados pelo cliente sao ignorados: so o conteudo decide.
	 */
	public ImagemValidada validar(MultipartFile arquivo) {
		String nome = nomeParaMensagem(arquivo);
		if (arquivo.isEmpty()) {
			throw new RequisicaoInvalidaException("A imagem '" + nome + "' está vazia");
		}
		if (arquivo.getSize() > tamanhoMaximo) {
			throw new RequisicaoInvalidaException("A imagem '" + nome + "' excede o tamanho máximo de "
				+ (tamanhoMaximo / (1024 * 1024)) + "MB");
		}
		byte[] inicio;
		try (InputStream in = arquivo.getInputStream()) {
			inicio = in.readNBytes(TipoImagem.BYTES_DA_ASSINATURA);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Falha ao ler o arquivo enviado", ex);
		}
		TipoImagem tipo = TipoImagem.detectar(inicio).orElseThrow(() ->
			new RequisicaoInvalidaException("A imagem '" + nome + "' não é PNG, JPEG nem WEBP"));
		return new ImagemValidada(arquivo, tipo);
	}

	/** Nome original so para mensagens de erro, limitado para nao devolver textos enormes. */
	private static String nomeParaMensagem(MultipartFile arquivo) {
		String nome = arquivo.getOriginalFilename() == null ? "sem nome" : arquivo.getOriginalFilename();
		return nome.length() > 80 ? nome.substring(0, 80) + "..." : nome;
	}
}
