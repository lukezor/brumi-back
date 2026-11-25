package br.com.brumi.service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Onde os arquivos das fotos ficam guardados. E uma INTERFACE: o resto do codigo (services, controller) so
 * conhece estes metodos e nao sabe se o destino e o disco ou a nuvem.
 *
 * A implementacao e escolhida pela propriedade brumi.armazenamento.tipo:
 * - "local" (padrao): ArmazenamentoLocal, pasta uploads/ no disco. Usada em dev e nos testes.
 * - "gcs": Cloud Storage do Google (producao).
 *
 * Paralelo com FastAPI: e como declarar uma dependencia abstrata e trocar a implementacao no Depends()
 * conforme o ambiente. No AngularJS, seria registrar um service diferente com o mesmo nome.
 */
public interface ArmazenamentoImagens {

	/** Nomes que nos mesmos geramos: UUID + extensao. Qualquer outra coisa e recusada antes de chegar ao destino. */
	Pattern NOME_VALIDO = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|webp)");

	/** Conteudo de uma foto lida do armazenamento, com o tipo para o cabecalho Content-Type. */
	record ImagemArmazenada(byte[] conteudo, TipoImagem tipo) {
	}

	/**
	 * Grava o arquivo com um nome NOVO e aleatorio (UUID) e devolve esse nome. O nome original e descartado:
	 * ele poderia conter "../../" (path traversal), caracteres invalidos ou coincidir com outro arquivo.
	 */
	String salvar(ImagemValidada imagem);

	/** Le a foto pelo nome. Vazio se nao existir ou se o nome for invalido. */
	Optional<ImagemArmazenada> ler(String nome);

	/**
	 * Apaga o arquivo, se existir. Nunca lanca excecao: e chamado DEPOIS de o banco confirmar a operacao, e
	 * uma falha aqui nao pode desfazer o que ja foi confirmado. So registra no log.
	 */
	void remover(String nome);

	static String novoNome(TipoImagem tipo) {
		return UUID.randomUUID() + "." + tipo.extensao();
	}

	static boolean nomeValido(String nome) {
		return nome != null && NOME_VALIDO.matcher(nome).matches();
	}

	/** Tipo da imagem pela extensao do nome (que nos mesmos geramos a partir do conteudo). */
	static TipoImagem tipoPeloNome(String nome) {
		String extensao = nome.substring(nome.lastIndexOf('.') + 1);
		for (TipoImagem tipo : TipoImagem.values()) {
			if (tipo.extensao().equals(extensao)) {
				return tipo;
			}
		}
		throw new IllegalArgumentException("Extensão desconhecida: " + nome);
	}
}
