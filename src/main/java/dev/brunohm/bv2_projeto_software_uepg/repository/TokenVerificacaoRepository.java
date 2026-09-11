package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TokenVerificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.FinalidadeToken;

@Repository
public interface TokenVerificacaoRepository extends JpaRepository<TokenVerificacao, Long> {

    /*
     * A busca e sempre pelo hash, nunca pelo token: o valor em claro nao existe no
     * banco. usuario e LAZY e os dois fluxos precisam dele logo em seguida, entao o
     * EntityGraph evita a segunda consulta.
     *
     * A finalidade nao entra aqui: quem chama confere e responde a mesma mensagem
     * generica de token invalido, para nao revelar que o token existe com outro
     * proposito.
     */
    @EntityGraph(attributePaths = { "usuario" })
    Optional<TokenVerificacao> findByTokenHash(String tokenHash);

    /*
     * Invalida de uma vez os links pendentes do usuario para uma finalidade. Roda ao
     * pedir um link novo e ao consumir um: com dois e-mails na caixa de entrada so o
     * ultimo vale, e depois de consumido nenhum vale.
     *
     * O filtro por finalidade nao e detalhe: sem ele, pedir uma troca de e-mail
     * mataria em silencio um link de recuperacao de senha recem pedido, e vice-versa.
     *
     * Update em massa e nao laco de save() porque nao ha nada a carregar em memoria —
     * o alvo e definido pelo where, e o numero de linhas e desconhecido de antemao.
     */
    @Modifying
    @Query("""
            update TokenVerificacao t
               set t.usadoEm = :agora
             where t.usuario.id = :usuarioId
               and t.finalidade = :finalidade
               and t.usadoEm is null
            """)
    int invalidarPendentesDoUsuario(@Param("usuarioId") Long usuarioId,
            @Param("finalidade") FinalidadeToken finalidade,
            @Param("agora") LocalDateTime agora);
}
