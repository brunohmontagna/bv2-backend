--- Usuario nao se apaga: desativa-se, como cliente e servico. Preserva o registro
--- de quem operou o sistema.
ALTER TABLE usuarios ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
