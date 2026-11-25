package br.com.brumi.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Filtros opcionais da listagem de produtos. Campo null (ou lista vazia) = "nao filtrar por isso".
 * "ativo" e obrigatorio aqui porque o controller aplica o padrao true (igual ao sistema original).
 */
public record FiltroProduto(String nome, List<Long> categorias, BigDecimal precoMin, BigDecimal precoMax, boolean ativo) {
}
