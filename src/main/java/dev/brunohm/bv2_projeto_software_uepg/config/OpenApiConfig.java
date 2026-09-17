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

                                **Contas.** Cada usuário tem a própria base: clientes, serviços, \
                                equipamentos, OS, notificações, templates e painel. O ADMIN opera \
                                só a conta dele. O MASTER (equipe desenvolvedora) escolhe a conta \
                                pelo header `X-Conta-Id` - sem ele, opera a própria - e é o único \
                                que enxerga o cadastro de usuários (/usuarios). Registro de outra \
                                conta responde 404; ADMIN enviando `X-Conta-Id` de outra conta, 403. \
                                Marcas são um catálogo global.

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
