package dev.brunohm.bv2_projeto_software_uepg.domain.enums;

/**
 * Para que serve um TokenVerificacao.
 *
 * <p>
 * Existe porque os dois fluxos compartilham a mesma mecanica (hash SHA-256, uso
 * unico, expiracao, invalidacao em cascata) e duas tabelas separadas divergiriam
 * na primeira correcao aplicada so de um lado.
 *
 * <p>
 * <b>A finalidade nunca e opcional na busca.</b> Um token de recuperacao de senha
 * nao pode ser aceito para trocar e-mail, e a invalidacao em cascata de um fluxo
 * nao pode derrubar os pendentes do outro.
 */
public enum FinalidadeToken {
    RECUPERACAO_SENHA,
    ALTERACAO_EMAIL
}
