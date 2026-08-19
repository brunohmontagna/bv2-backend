INSERT INTO usuarios (id, nome, email, senha)
VALUES (1,
    'BV2 Admin',
    'bv2uepg2026@gmail.com',
    '$2b$12$CHV.Xo4RKN4Ju78cgdeKS.W9CyBLboJCZSWksq/xyH9sGjQRmcWJS'
);

-- Reajusta a sequência de identidade para o maior id existente,
-- evitando conflito de PK no próximo insert gerado automaticamente
SELECT setval(pg_get_serial_sequence('usuarios', 'id'), (SELECT MAX(id) FROM usuarios));
