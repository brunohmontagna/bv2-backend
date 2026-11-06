-- =============================================================================
-- V13 — troca de senha verificada por e-mail
-- A troca de senha logada deixa de valer na hora: o pedido grava a senha nova
-- (já em BCrypt) num token de ALTERACAO_SENHA, e ela só passa a valer quando o
-- link enviado ao e-mail atual é clicado.
-- Depende de: tokens_verificacao (V2).
-- =============================================================================

ALTER TYPE finalidade_token ADD VALUE IF NOT EXISTS 'ALTERACAO_SENHA';

-- Hash BCrypt da senha pedida, só em tokens de ALTERACAO_SENHA (NULL nos
-- demais). Nunca a senha em claro: o token fica no banco até expirar.
ALTER TABLE tokens_verificacao ADD COLUMN senha_nova VARCHAR(60);
