--- valor_total_manual: quando TRUE, o valorTotal foi definido a mao (desconto,
--- preco fechado) e para de ser recalculado a partir dos itens. Um reset volta a
--- coluna para FALSE e o valorTotal passa a refletir a soma dos servicos de novo.
ALTER TABLE ordens_servico
    ADD COLUMN valor_total_manual BOOLEAN NOT NULL DEFAULT FALSE;
