CREATE TABLE usuario (
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    nome  VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    -- Unicidade garantida pelo banco, nao so pela aplicacao: e o que segura
    -- duas requisicoes concorrentes com o mesmo e-mail.
    CONSTRAINT uk_usuario_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
