--- configuracao: uma linha por status de OS que notifica.
--- a PK e o proprio status_os, que existe desde a V6 — nao ha enum novo. conteudo
--- e ativo moram juntos porque o modal do front edita os dois no mesmo formulario.
CREATE TABLE templates_notificacao (
    status          status_os       NOT NULL,
    conteudo        VARCHAR(500)    NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT FALSE,
    atualizado_em   TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_templates_notificacao PRIMARY KEY (status)
);

--- tres linhas, nao quatro: EM_ANDAMENTO fica de fora porque abertura de OS nao
--- notifica. Se um dia notificar, e um INSERT — nao uma migration de enum.
--- Nascem desligadas: ninguem manda WhatsApp para cliente real por acidente na
--- primeira subida. O conteudo ja vem preenchido para o modal nunca abrir vazio.
INSERT INTO templates_notificacao (status, conteudo, ativo) VALUES
    ('CONCLUIDA', 'Ola, {cliente}! O servico da ordem #{os} foi concluido e o equipamento ja pode ser retirado. Valor: R$ {valor}. - M2 Equipamentos', FALSE),
    ('ENTREGUE',  'Ola, {cliente}! Confirmamos a entrega do equipamento da ordem #{os}. Obrigado pela preferencia! - M2 Equipamentos', FALSE),
    ('CANCELADA', 'Ola, {cliente}! A ordem de servico #{os} foi cancelada. Qualquer duvida, e so chamar. - M2 Equipamentos', FALSE);

--- notificacoes vira log de execucao puro. O tipo proprio some: o que dispara o
--- envio e o status da OS, e nao uma classificacao paralela. Coluna e nao FK para
--- templates_notificacao — e log, precisa sobreviver a edicao ou a remocao do
--- template. Nao conflita com notificacoes.status, que continua PENDENTE/ENVIADO/FALHOU.
---
--- ADD COLUMN NOT NULL sem default so passa em tabela vazia, que e o caso: nunca
--- houve endpoint escrevendo em notificacoes.
ALTER TABLE notificacoes DROP COLUMN tipo;
ALTER TABLE notificacoes ADD COLUMN status_os status_os NOT NULL;
DROP TYPE tipo_notificacao;

--- conteudo passa a guardar o texto renderizado que foi ao cliente. O template
--- cabe em 500, mas a substituicao dos placeholders cresce o texto — sem folga,
--- um nome de cliente longo estouraria o insert.
ALTER TABLE notificacoes ALTER COLUMN conteudo TYPE VARCHAR(1000);

--- as FKs de notificacoes nao tinham indice; as duas consultas do GET /notificacoes
--- filtram exatamente por elas.
CREATE INDEX idx_notificacoes_ordem_servico ON notificacoes (id_ordem_servico);
CREATE INDEX idx_notificacoes_cliente       ON notificacoes (id_cliente);
