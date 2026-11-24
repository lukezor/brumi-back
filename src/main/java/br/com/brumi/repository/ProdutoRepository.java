package br.com.brumi.repository;

import br.com.brumi.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco para Produto. Os filtros de busca entram na Task 6. */
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
