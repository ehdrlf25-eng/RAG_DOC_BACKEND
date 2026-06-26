package com.ragdoc.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 문서 설정.
 * 전역 Bearer JWT 스킴을 선언해 인증 API 외 엔드포인트에 토큰 입력을 유도한다.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "Bearer Authentication";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAG Doc Platform API")
                        .description("RAG Doc Platform REST API 문서")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인/회원가입 응답의 accessToken을 입력하세요.")));
    }
}
