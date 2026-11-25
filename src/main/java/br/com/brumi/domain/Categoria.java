package br.com.brumi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidade JPA: cada instancia desta classe representa UMA LINHA da tabela "categoria".
 * (Equivale a um modelo do SQLAlchemy: class Categoria(Base).)
 *
 * @Entity  -> o Hibernate (implementacao do JPA) passa a gerenciar esta classe.
 * @Table   -> nome da tabela no banco.
 */
@Entity
@Table(name = "categoria")
public class Categoria {

	/**
	 * @Id: chave primaria.
	 * IDENTITY: quem gera o valor e o proprio banco (coluna GENERATED ... AS IDENTITY).
	 * Long (e nao long): fica null enquanto o objeto ainda nao foi salvo.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nome;

	/** O JPA exige um construtor sem argumentos (pode ser protected: so ele usa). */
	protected Categoria() {
	}

	public Categoria(String nome) {
		this.nome = nome;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	// Duas categorias sao "a mesma" se tiverem o mesmo id. Isso importa porque as categorias
	// ficam num Set. O hashCode constante e um padrao comum em entidades JPA: o id muda de null
	// para um numero ao salvar, e um hashCode que muda quebraria o Set.
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Categoria outra)) {
			return false;
		}
		return id != null && id.equals(outra.getId());
	}

	@Override
	public int hashCode() {
		return Categoria.class.hashCode();
	}
}
