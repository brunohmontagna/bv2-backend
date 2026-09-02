--- Impede servicos duplicados pelo par (nome, valor): pode existir "Troca de tela"
--- por 450 e outra por 320, mas nao duas "Troca de tela" por 320.
--- Indice funcional com LOWER(nome) para a unicidade ser case-insensitive,
--- acompanhando a checagem existsByNomeIgnoreCaseAndValor da aplicacao.
CREATE UNIQUE INDEX uq_servicos_nome_valor ON servicos (LOWER(nome), valor);
