package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TokenRecuperacaoSenha;

@Repository
public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {

    /*
     * A busca e sempre pelo hash, nunca pelo token: o valor em claro nao existe no
     * banco. usuario e LAZY e o fluxo de redefinicao precisa dele logo em seguida,
     * entao o EntityGraph evita a segunda consulta.
     */
    @EntityGraph(attributePaths = { "usuario" })
    Optional<TokenRecuperacaoSenha> findByTokenHash(String tokenHash);

    /*
     * Invalida de uma vez todos os links pendentes do usuario. Roda ao pedir um link
     * novo e ao redefinir a senha: com dois e-mails na caixa de entrada, so o ultimo
     * vale, e depois da troca nenhum vale.
     *
     * Update em massa e nao laco de save() porque nao ha nada a carregar em memoria —
     * o alvo e definido pelo where, e o numero de linhas e desconhecido de antemao.
     */
    @Modifying
    @Query("""
            update TokenRecuperacaoSenha t
               set t.usadoEm = :agora
             where t.usuario.id = :usuarioId
               and t.usadoEm is null
            """)
    int invalidarPendentesDoUsuario(@Param("usuarioId") Long usuarioId,
            @Param("agora") LocalDateTime agora);
}
