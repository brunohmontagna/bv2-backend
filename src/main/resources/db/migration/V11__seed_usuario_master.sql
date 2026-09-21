-- =============================================================================
-- V11 — seed: usuário MASTER inicial
-- Credencial de desenvolvimento: bv2uepg2026@gmail.com / admin123.
-- Troque a senha em qualquer ambiente de produção.
-- Depende de: usuarios (V1).
-- =============================================================================

-- Hash BCrypt ($2b$, custo 12) de 'admin123'.
INSERT INTO usuarios (id, nome, email, senha, role)
VALUES (1,
    'BV2 Admin',
    'bv2uepg2026@gmail.com',
    '$2b$12$dHxNQJxzxBLQWEn0gPDnCekpxkf24vGB/9YrQlPb3GqhrWljgqgTC',
    'MASTER'
);

-- O id foi informado explicitamente; ajusta a identity para o próximo insert
-- automático não colidir com a PK 1.
SELECT setval(pg_get_serial_sequence('usuarios', 'id'), (SELECT MAX(id) FROM usuarios));
