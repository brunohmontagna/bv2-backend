--- Padroniza a senha do usuario MASTER de teste em qualquer banco recriado do zero.
--- O hash da V9 codificava uma senha divergente da usada pela equipe (admin123);
--- aqui o seed passa a bater com a credencial documentada no CLAUDE.md.
--- Hash BCrypt ($2b$, custo 12) de 'admin123'.
UPDATE usuarios
SET senha = '$2b$12$dHxNQJxzxBLQWEn0gPDnCekpxkf24vGB/9YrQlPb3GqhrWljgqgTC'
WHERE email = 'bv2uepg2026@gmail.com';
