-- =============================================================================
-- V12 — seed: templates de notificação
-- Os 3 templates de cada conta existente — na primeira subida, só a do MASTER
-- semeado em V11. Usuários criados depois recebem os mesmos textos pela
-- aplicação (TemplatesNotificacaoPadrao).
--
-- Três linhas por conta, não quatro: EM_ANDAMENTO fica de fora porque abrir OS
-- não notifica. Nascem DESLIGADAS para ninguém mandar WhatsApp a cliente real
-- por acidente na primeira subida; o texto já vem preenchido para o modal de
-- configuração nunca abrir vazio.
-- Depende de: usuarios (V1), templates_notificacao (V9), seed V11.
-- =============================================================================

INSERT INTO templates_notificacao (id_usuario, status, conteudo, ativo)
SELECT u.id, t.status::status_os, t.conteudo, FALSE
FROM usuarios u
CROSS JOIN (VALUES
    ('CONCLUIDA', 'Ola, {cliente}! O servico da ordem #{os} foi concluido e o equipamento ja pode ser retirado. Valor: R$ {valor}. - M2 Equipamentos'),
    ('ENTREGUE',  'Ola, {cliente}! Confirmamos a entrega do equipamento da ordem #{os}. Obrigado pela preferencia! - M2 Equipamentos'),
    ('CANCELADA', 'Ola, {cliente}! A ordem de servico #{os} foi cancelada. Qualquer duvida, e so chamar. - M2 Equipamentos')
) AS t (status, conteudo);
