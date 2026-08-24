CREATE TYPE role_usuario AS ENUM ('ADMIN', 'CLIENTE');

ALTER TABLE usuarios
    ADD COLUMN role role_usuario NOT NULL DEFAULT 'CLIENTE';

-- Usuario semeado na V9 e o administrador do sistema
UPDATE usuarios SET role = 'ADMIN' WHERE id = 1;
