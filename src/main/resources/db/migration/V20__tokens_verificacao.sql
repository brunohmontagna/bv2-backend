--- O mecanismo de token nasceu na V19 para recuperacao de senha e agora serve
--- tambem a alteracao de e-mail: mesmo hash SHA-256, mesmo uso unico, mesma
--- expiracao, mesma invalidacao em cascata. Uma segunda tabela duplicaria tudo
--- isso, e as duas divergiriam na primeira correcao aplicada so de um lado.
CREATE TYPE finalidade_token AS ENUM ('RECUPERACAO_SENHA', 'ALTERACAO_EMAIL');

ALTER TABLE tokens_recuperacao_senha RENAME TO tokens_verificacao;

--- Toda linha existente e de recuperacao de senha, por definicao: e o unico
--- fluxo que existia. A coluna nasce com esse default e logo o perde, para o
--- codigo ser obrigado a declarar a finalidade em cada insert novo.
ALTER TABLE tokens_verificacao
    ADD COLUMN finalidade finalidade_token NOT NULL DEFAULT 'RECUPERACAO_SENHA';
ALTER TABLE tokens_verificacao ALTER COLUMN finalidade DROP DEFAULT;

--- O endereco novo viaja no token, e nao na URL nem no corpo da confirmacao: ele
--- e escolhido no pedido, e a tela de confirmacao so precisa clicar. Nulo para
--- token de recuperacao de senha, por isso nao e NOT NULL.
ALTER TABLE tokens_verificacao ADD COLUMN email_novo VARCHAR(50);

--- As constraints e o indice carregam o nome antigo; renomear evita que o
--- proximo a ler o schema ache que sobrou lixo da V19.
ALTER TABLE tokens_verificacao RENAME CONSTRAINT pk_token_recuperacao_senha TO pk_token_verificacao;
ALTER TABLE tokens_verificacao RENAME CONSTRAINT uq_token_recuperacao_senha_hash TO uq_token_verificacao_hash;
ALTER TABLE tokens_verificacao RENAME CONSTRAINT fk_token_recuperacao_senha_usuario TO fk_token_verificacao_usuario;
ALTER INDEX idx_tokens_recuperacao_usuario RENAME TO idx_tokens_verificacao_usuario;

--- A invalidacao em cascata passa a filtrar por finalidade: sem isso, pedir uma
--- troca de e-mail mataria em silencio um link de recuperacao de senha recem
--- pedido. O indice parcial cobre exatamente essa consulta.
CREATE INDEX idx_tokens_verificacao_usuario_finalidade
    ON tokens_verificacao (id_usuario, finalidade) WHERE usado_em IS NULL;
