package dev.brunohm.bv2_projeto_software_uepg.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.RoleUsuario;
import lombok.Getter;

/**
 * Adaptador entre a entidade Usuario e o contrato UserDetails do Spring Security.
 * Existe para nao poluir a entidade JPA com responsabilidades de seguranca.
 */
@Getter
public class UsuarioAutenticado implements UserDetails {

    private final Long id;
    private final String email;
    private final String senha;
    private final RoleUsuario role;
    private final boolean ativo;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.senha = usuario.getSenha();
        this.role = usuario.getRole();
        this.ativo = Boolean.TRUE.equals(usuario.getAtivo());
    }

    /** MASTER e o unico papel que enxerga o cadastro de usuarios do sistema. */
    public boolean isMaster() {
        return RoleUsuario.MASTER.equals(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // O prefixo ROLE_ e o que permite usar hasRole('MASTER') nas anotacoes.
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Usuario desativado nao autentica: o DaoAuthenticationProvider recusa o login
     * sozinho a partir daqui. Tokens ja emitidos sao barrados no JwtAuthFilter.
     */
    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
