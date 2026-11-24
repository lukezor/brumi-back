package br.com.brumi.service;

import br.com.brumi.domain.Categoria;
import br.com.brumi.dto.CategoriaRequest;
import br.com.brumi.dto.CategoriaResponse;
import br.com.brumi.exception.ConflitoException;
import br.com.brumi.exception.RecursoNaoEncontradoException;
import br.com.brumi.repository.CategoriaRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Camada de REGRAS DE NEGOCIO (≈ crud.py). Nao sabe nada de HTTP: recebe e devolve objetos Java e,
 * quando algo da errado, lanca excecoes de negocio. Quem traduz isso para HTTP e o controller/handler.
 *
 * @Service: marca a classe como um bean gerenciado pelo Spring.
 */
@Service
public class CategoriaService {

	private final CategoriaRepository repository;

	/**
	 * INJECAO DE DEPENDENCIA: a classe declara no construtor o que precisa (um CategoriaRepository)
	 * e o Spring entrega uma instancia pronta. Voce nunca escreve "new CategoriaService(...)".
	 * (≈ repository: Repo = Depends(get_repo) do FastAPI.)
	 * Com um unico construtor, nao precisa nem de @Autowired.
	 */
	public CategoriaService(CategoriaRepository repository) {
		this.repository = repository;
	}

	/**
	 * @Transactional(readOnly = true): abre uma transacao so de leitura e a mantem aberta ate o fim
	 * do metodo. A conversao para DTO acontece DENTRO dela (importante por causa do lazy loading).
	 */
	@Transactional(readOnly = true)
	public List<CategoriaResponse> listar() {
		return repository.findAll(Sort.by("nome")).stream()
			.map(CategoriaResponse::de)
			.toList();
	}

	@Transactional(readOnly = true)
	public CategoriaResponse obter(Long id) {
		return repository.findById(id)
			.map(CategoriaResponse::de)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
	}

	@Transactional
	public CategoriaResponse criar(CategoriaRequest request) {
		String nome = request.nome().trim();
		if (repository.existsByNome(nome)) {
			throw new ConflitoException("Já existe uma categoria com o nome '" + nome + "'");
		}
		return CategoriaResponse.de(repository.save(new Categoria(nome)));
	}
}
