package dev.brunohm.bv2_projeto_software_uepg.dto.auth;

import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEm) {

    public static LoginResponse bearer(String token, Instant expiraEm) {
        return new LoginResponse(token, "Bearer", expiraEm);
    }
}
