package dev.brunohm.bv2_projeto_software_uepg.domain.enums;

/**
 * Papeis de quem faz login. O cliente da M2 nao esta aqui de proposito: ele e um
 * cadastro, nao um usuario do sistema.
 */
public enum RoleUsuario {

    /** Equipe desenvolvedora. Unico papel que enxerga o cadastro de usuarios. */
    MASTER,

    /** M2 Equipamentos. Opera todo o resto do sistema. */
    ADMIN
}
