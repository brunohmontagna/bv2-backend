package dev.brunohm.bv2_projeto_software_uepg.dto.cliente;

import java.time.LocalDateTime;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Cliente;

public record ClienteResponse(
        Long id,
        String nome,
        String telefone,
        Boolean ativo,
        LocalDateTime criadoEm,
        UsuarioResponse usuario) {

    public static ClienteResponse fromEntity(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                cliente.getAtivo(),
                cliente.getCriadoEm(),
                UsuarioResponse.fromEntity(cliente.getUsuario()));
    }
}
