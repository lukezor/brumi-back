-- Dados de EXEMPLO para desenvolvimento (so carregado no perfil "dev").
-- Migration repetivel (prefixo R__): o Flyway reexecuta se este arquivo mudar.
-- Por isso o script e idempotente: rodar duas vezes nao duplica nada.

INSERT INTO categoria (nome) VALUES
    ('Cestas'),
    ('Kits'),
    ('Presentes Especiais')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO produto (nome, descricao, preco, ativo)
SELECT v.nome, v.descricao, v.preco, v.ativo
FROM (VALUES
    ('Cesta Especial',     'Uma seleção especial de produtos para presentear quem você ama.', 199.90, TRUE),
    ('Kit Perfumado',      'Kit com produtos perfumados para todos os gostos.',               159.90, TRUE),
    ('Cesta Premium',      'Produtos selecionados com o melhor custo-benefício.',             249.90, TRUE),
    ('Caneca Personalizada', 'Caneca com nome ou foto escolhidos por você.',                   39.90, TRUE),
    ('Kit Descontinuado',  'Produto inativo, usado para testar o filtro de ativos.',           89.90, FALSE)
) AS v(nome, descricao, preco, ativo)
WHERE NOT EXISTS (SELECT 1 FROM produto p WHERE p.nome = v.nome);

INSERT INTO produto_categoria (produto_id, categoria_id)
SELECT p.id, c.id
FROM produto p
JOIN categoria c ON (p.nome, c.nome) IN (
    ('Cesta Especial',       'Cestas'),
    ('Cesta Premium',        'Cestas'),
    ('Kit Perfumado',        'Kits'),
    ('Kit Descontinuado',    'Kits'),
    ('Caneca Personalizada', 'Presentes Especiais'),
    ('Cesta Especial',       'Presentes Especiais')
)
ON CONFLICT DO NOTHING;
