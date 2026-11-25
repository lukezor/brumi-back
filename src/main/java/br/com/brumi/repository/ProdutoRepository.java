package br.com.brumi.repository;

import br.com.brumi.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Acesso ao banco para Produto.
 *
 * JpaSpecificationExecutor acrescenta findAll(Specification, Pageable): busca com filtros dinamicos
 * (veja ProdutoSpecs) e paginacao, devolvendo um Page com os itens e o total.
 */
public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {
}
