-- V2: usuarios que podem administrar o catalogo.
-- A senha NUNCA e guardada: so o hash BCrypt dela (60 caracteres, comeca com "$2a$").

CREATE TABLE usuario (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    papel      VARCHAR(20)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_usuario_username UNIQUE (username),
    -- Por enquanto so existe o papel ADMIN. Um papel novo exige uma nova migration.
    CONSTRAINT ck_usuario_papel CHECK (papel IN ('ADMIN'))
);
