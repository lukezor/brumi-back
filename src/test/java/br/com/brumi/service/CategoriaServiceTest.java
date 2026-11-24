package br.com.brumi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.brumi.domain.Categoria;
import br.com.brumi.dto.CategoriaRequest;
import br.com.brumi.dto.CategoriaResponse;
import br.com.brumi.exception.ConflitoException;
import br.com.brumi.exception.RecursoNaoEncontradoException;
import br.com.brumi.repository.CategoriaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

/**
 * Teste UNITARIO: sem Spring e sem banco. O repository e um "dublê" (mock) criado pelo Mockito,
 * onde nos dizemos o que ele responde. Testa so a regra de negocio do service. Roda em milissegundos.
 */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

	@Mock
	CategoriaRepository repository;

	@InjectMocks
	CategoriaService service;

	@Test
	void listarDevolveDtosOrdenadosPorNome() {
		when(repository.findAll(Sort.by("nome"))).thenReturn(List.of(new Categoria("Cestas"), new Categoria("Kits")));

		List<CategoriaResponse> resultado = service.listar();

		assertThat(resultado).extracting(CategoriaResponse::nome).containsExactly("Cestas", "Kits");
	}

	@Test
	void obterLancaNaoEncontradoQuandoNaoExiste() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.obter(99L))
			.isInstanceOf(RecursoNaoEncontradoException.class)
			.hasMessage("Categoria não encontrada");
	}

	@Test
	void criarSalvaComNomeSemEspacosNasPontas() {
		when(repository.existsByNome("Cestas")).thenReturn(false);
		when(repository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

		CategoriaResponse criada = service.criar(new CategoriaRequest("  Cestas  "));

		ArgumentCaptor<Categoria> capturada = ArgumentCaptor.forClass(Categoria.class);
		verify(repository).save(capturada.capture());
		assertThat(capturada.getValue().getNome()).isEqualTo("Cestas");
		assertThat(criada.nome()).isEqualTo("Cestas");
	}

	@Test
	void criarLancaConflitoQuandoNomeJaExiste() {
		when(repository.existsByNome("Cestas")).thenReturn(true);

		assertThatThrownBy(() -> service.criar(new CategoriaRequest("Cestas")))
			.isInstanceOf(ConflitoException.class)
			.hasMessageContaining("Cestas");
		verify(repository, never()).save(any());
	}
}
