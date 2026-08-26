package dev.brunohm.bv2_projeto_software_uepg.dto.equipamento;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Cliente;

/**
 * Recorte minimo do cliente para aninhar no equipamento. Existe para nao
 * reaproveitar o ClienteResponse, que carrega o UsuarioResponse e vazaria
 * e-mail e papel de acesso dentro de cada item da listagem.
 */
public record ClienteResumoResponse(
        Long id,
        String nome) {

    public static ClienteResumoResponse fromEntity(Cliente cliente) {
        return new ClienteResumoResponse(cliente.getId(), cliente.getNome());
    }
}
