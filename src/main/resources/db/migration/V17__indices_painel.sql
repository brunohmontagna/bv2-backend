--- Indices de apoio ao GET /painel. Ate aqui a unica coisa indexada alem das PKs
--- e das UNIQUEs era uq_servicos_nome_valor (V15): as agregacoes do painel e os
--- filtros de /ordens-servico nasciam todos em seq scan. Com o volume atual de um
--- MEI o planner ainda vai preferir seq scan; estes indices documentam o padrao de
--- acesso e evitam a degradacao silenciosa quando a base crescer.

--- Recorta a serie mensal, o faturamento em aberto, a contagem de valor manual e os
--- dois rankings que passam por itens_os. Serve tambem o ?dataInicio= de
--- GET /ordens-servico, que ja existia sem indice.
CREATE INDEX idx_ordens_servico_data_entrada ON ordens_servico (data_entrada);

--- Parcial de proposito: data_entregue so e preenchida na transicao para ENTREGUE, e
--- ENTREGUE e terminal. Logo "data_entregue IS NOT NULL" equivale a "status = ENTREGUE",
--- que e exatamente o recorte do faturamento realizado — o indice ignora toda OS em
--- andamento, concluida ou cancelada.
CREATE INDEX idx_ordens_servico_data_entregue ON ordens_servico (data_entregue)
    WHERE data_entregue IS NOT NULL;

--- O Postgres nao cria indice de chave estrangeira sozinho. Atende o ranking de
--- clientes por faturamento (join + group by) e o filtro ?clienteId= das listagens.
CREATE INDEX idx_ordens_servico_cliente ON ordens_servico (id_cliente);

--- Ranking de servicos mais executados (group by id_servico) e a checagem do
--- ON DELETE RESTRICT ao tentar excluir um servico do catalogo. A UNIQUE
--- uq_item_os_os_equip_servico nao cobre estas colunas: nela id_ordem_servico e a
--- coluna lider, e id_servico/id_equipamento sozinhos ficam de fora.
CREATE INDEX idx_itens_os_servico ON itens_os (id_servico);
CREATE INDEX idx_itens_os_equipamento ON itens_os (id_equipamento);

--- Ranking de marcas: o caminho e itens_os -> equipamentos -> marcas.
CREATE INDEX idx_equipamentos_marca ON equipamentos (id_marca);
