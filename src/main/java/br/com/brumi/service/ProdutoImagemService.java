package br.com.brumi.service;

import br.com.brumi.domain.Produto;
import br.com.brumi.dto.ProdutoResponse;
import br.com.brumi.exception.RecursoNaoEncontradoException;
import br.com.brumi.exception.RequisicaoInvalidaException;
import br.com.brumi.repository.ProdutoRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Regras das imagens de um produto. (≈ adicionar_imagens_produto e remover_imagem_produto do crud.py.) */
@Service
public class ProdutoImagemService {

	private final ProdutoRepository produtos;
	private final ArmazenamentoImagens armazenamento;
	private final ValidadorImagens validador;

	public ProdutoImagemService(ProdutoRepository produtos, ArmazenamentoImagens armazenamento,
			ValidadorImagens validador) {
		this.produtos = produtos;
		this.armazenamento = armazenamento;
		this.validador = validador;
	}

	/**
	 * Ordem importa:
	 * 1. Valida TUDO (quantidade, tamanho, tipo) antes de gravar QUALQUER arquivo. Uma imagem ruim recusa o lote.
	 * 2. Agenda a limpeza: se o banco desfizer a transacao, os arquivos gravados sao apagados.
	 * 3. Grava os arquivos e liga ao produto. O INSERT acontece no commit, ao fim do metodo.
	 */
	@Transactional
	public ProdutoResponse adicionar(Long produtoId, List<MultipartFile> arquivos) {
		Produto produto = buscar(produtoId);
		if (arquivos == null || arquivos.isEmpty()) {
			throw new RequisicaoInvalidaException("Envie ao menos uma imagem");
		}
		int atuais = produto.getImagens().size();
		if (atuais + arquivos.size() > Produto.MAXIMO_IMAGENS) {
			throw new RequisicaoInvalidaException("O produto pode ter no máximo " + Produto.MAXIMO_IMAGENS
				+ " imagens. Ele já tem " + atuais + " e foram enviadas " + arquivos.size() + ".");
		}
		List<ImagemValidada> validadas = arquivos.stream().map(validador::validar).toList();

		List<String> gravados = new ArrayList<>();
		PosTransacao.aoDesfazer(() -> gravados.forEach(armazenamento::remover));
		for (ImagemValidada imagem : validadas) {
			gravados.add(armazenamento.salvar(imagem));
		}
		gravados.forEach(produto::adicionarImagem);
		return ProdutoResponse.de(produto);
	}

	/** Remove pela posicao (0 = primeira), como no sistema original. O arquivo so e apagado apos o commit. */
	@Transactional
	public ProdutoResponse remover(Long produtoId, int indice) {
		Produto produto = buscar(produtoId);
		int total = produto.getImagens().size();
		if (total == 0) {
			throw new RecursoNaoEncontradoException("O produto não tem imagens");
		}
		if (indice < 0 || indice >= total) {
			throw new RequisicaoInvalidaException("Índice de imagem inválido: use um valor entre 0 e " + (total - 1));
		}
		String arquivo = produto.removerImagem(indice);
		PosTransacao.aposCommit(() -> armazenamento.remover(arquivo));
		return ProdutoResponse.de(produto);
	}

	private Produto buscar(Long id) {
		return produtos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
	}
}
