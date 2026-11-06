-- =============================================================================
-- V14 — serviço com nome único na conta
-- A unicidade deixa de ser (nome, valor) e passa a ser só o nome: dois serviços
-- "Rebobinamento de estator" com preços diferentes confundem quem lança a OS.
-- Depende de: servicos (V4).
-- =============================================================================

-- Os repetidos que já existem ganham o valor no nome, para o índice novo poder
-- nascer sem apagar nada: o mais antigo de cada grupo fica com o nome original.
-- LEFT(nome, 34) garante espaço para o sufixo " (R$ 9999999,99)" nos 50.
-- Não há colisão de valor dentro do grupo: o índice antigo já a impedia.
UPDATE servicos s
   SET nome = LEFT(s.nome, 34) || ' (R$ ' || REPLACE(s.valor::text, '.', ',') || ')'
  FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY id_usuario, LOWER(nome) ORDER BY id) AS ordem
          FROM servicos
       ) d
 WHERE d.id = s.id
   AND d.ordem > 1;

DROP INDEX uq_servicos_usuario_nome_valor;

CREATE UNIQUE INDEX uq_servicos_usuario_nome ON servicos (id_usuario, LOWER(nome));
