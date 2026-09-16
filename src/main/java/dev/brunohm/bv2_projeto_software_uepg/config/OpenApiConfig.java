package dev.brunohm.bv2_projeto_software_uepg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BV2 - API da assistência técnica")
                        .description("""
                                Sistema de ordens de serviço da M2 Equipamentos. \
                                Projeto de Engenharia de Software - UEPG.

                                **Papéis.** Quem faz login é a equipe desenvolvedora (MASTER) \
                                e a M2 (ADMIN). Os dois operam todo o sistema por igual; a única \
                                diferença é que só o MASTER enxerga o cadastro de usuários (/usuarios).

                                **Cliente não é usuário.** O que /clientes lista são os clientes \
                                da M2 - quem leva o equipamento para consertar. Eles não fazem login.

                                **Erros** seguem RFC 7807 (ProblemDetail), com `timestamp` e, na \
                                validação, um mapa `erros` de campo para mensagem.
                                """)
                        .version("v1"))
                // Habilita o botao "Authorize" do Swagger UI para colar o JWT.
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
