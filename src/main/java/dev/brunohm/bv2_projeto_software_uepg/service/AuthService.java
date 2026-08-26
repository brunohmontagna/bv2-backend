package dev.brunohm.bv2_projeto_software_uepg.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.LoginRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.LoginResponse;
import dev.brunohm.bv2_projeto_software_uepg.repository.UsuarioRepository;
import dev.brunohm.bv2_projeto_software_uepg.security.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest request) {
        // Lanca BadCredentialsException (-> 401) quando e-mail ou senha nao conferem.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"));

        JwtService.TokenGerado gerado = jwtService.gerarToken(usuario);
        return LoginResponse.bearer(gerado.token(), gerado.expiraEm());
    }
}
