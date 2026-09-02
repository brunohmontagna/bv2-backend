--- Corrige a nomenclatura dos papeis. O modelo anterior assumia que o cliente da
--- M2 faria login; nao faz. Quem loga e a equipe (MASTER) e a M2 (ADMIN).

--- A ordem importa: renomear CLIENTE -> ADMIN primeiro colidiria com o ADMIN
--- que ainda existe neste ponto.
ALTER TYPE role_usuario RENAME VALUE 'ADMIN' TO 'MASTER';
ALTER TYPE role_usuario RENAME VALUE 'CLIENTE' TO 'ADMIN';

--- O rename ja arrasta o default, porque o Postgres guarda o OID do label e nao
--- o texto. Explicito aqui so para o schema nao depender desse detalhe.
ALTER TABLE usuarios ALTER COLUMN role SET DEFAULT 'ADMIN';
