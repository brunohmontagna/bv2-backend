--- Cliente e o cliente DA M2 (quem leva a furadeira para consertar): um cadastro,
--- nao um usuario do sistema. O vinculo 1:1 com usuarios era o erro de modelagem.

--- DROP COLUMN leva junto a FK fk_clientes_usuario e a UNIQUE uq_clientes_id_usuario.
--- A tabela usuarios nao e tocada: ela e o lado pai da FK, entao todo usuario
--- mantem id, e-mail, senha e criado_em.
ALTER TABLE clientes DROP COLUMN id_usuario;
