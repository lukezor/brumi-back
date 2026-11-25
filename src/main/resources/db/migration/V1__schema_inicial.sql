-- V1: schema inicial do catalogo Brumi.
-- ATENCAO: depois de aplicada (rodada em qualquer banco), esta migration NAO pode mais ser editada.
-- Para mudar algo, crie uma nova migration (V3__..., V4__...).

CREATE TABLE categoria (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    CONSTRAINT uq_categoria_nome UNIQUE (nome)
);

CREATE TABLE produto (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome      VARCHAR(200)  NOT NULL,
    descricao VARCHAR(1000),
    -- NUMERIC(10,2): ate 99.999.999,99 com exatamente 2 casas. Nunca use float para dinheiro.
    preco     NUMERIC(10,2),
    ativo     BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_produto_nome  ON produto (nome);
CREATE INDEX idx_produto_ativo ON produto (ativo);

-- Tabela de juncao da relacao N:N entre produto e categoria.
CREATE TABLE produto_categoria (
    produto_id   BIGINT NOT NULL REFERENCES produto (id)   ON DELETE CASCADE,
    categoria_id BIGINT NOT NULL REFERENCES categoria (id) ON DELETE RESTRICT,
    PRIMARY KEY (produto_id, categoria_id)
);

CREATE INDEX idx_produto_categoria_categoria ON produto_categoria (categoria_id);

-- Antes: coluna ARRAY(String) em produto. Agora: uma linha por imagem, com a posicao na lista.
CREATE TABLE produto_imagem (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produto_id BIGINT       NOT NULL REFERENCES produto (id) ON DELETE CASCADE,
    posicao    INTEGER      NOT NULL,
    caminho    VARCHAR(500) NOT NULL,
    -- Maximo de 5 imagens por produto => posicoes 0 a 4.
    CONSTRAINT ck_produto_imagem_posicao CHECK (posicao BETWEEN 0 AND 4),
    -- DEFERRABLE: a unicidade so e conferida no fim da transacao, o que permite reordenar posicoes.
    CONSTRAINT uq_produto_imagem_posicao UNIQUE (produto_id, posicao) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_produto_imagem_produto ON produto_imagem (produto_id);
