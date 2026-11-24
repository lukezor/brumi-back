package br.com.brumi.repository;

import br.com.brumi.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository: acesso ao banco para a entidade Categoria.
 *
 * Voce escreve apenas esta INTERFACE, sem implementacao. Em tempo de execucao o Spring Data cria
 * a classe que faz save(), findById(), findAll(), delete(), count()... (equivale ao db.query()).
 * Os tipos genericos sao <Entidade, TipoDoId>.
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

	/**
	 * "Query derivada": o Spring le o NOME do metodo e gera o SQL sozinho.
	 * existsByNome -> select count(*) > 0 from categoria where nome = ?
	 */
	boolean existsByNome(String nome);
}
