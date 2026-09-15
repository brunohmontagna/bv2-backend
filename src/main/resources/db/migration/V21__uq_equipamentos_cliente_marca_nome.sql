--- Um mesmo cliente nao pode ter dois equipamentos com o mesmo nome E a mesma marca:
--- a M2 nao consegue distinguir "Furadeira Makita" de "Furadeira Makita" na hora de
--- abrir a OS. Mesmo nome com marca diferente, ou mesmo nome e marca em clientes
--- diferentes, continuam valendo.
--- LOWER(nome) deixa a unicidade case-insensitive, acompanhando a checagem
--- existsBy...NomeIgnoreCase do EquipamentoService (mesmo padrao da V15).
CREATE UNIQUE INDEX uq_equipamentos_cliente_marca_nome
    ON equipamentos (id_cliente, id_marca, LOWER(nome));
