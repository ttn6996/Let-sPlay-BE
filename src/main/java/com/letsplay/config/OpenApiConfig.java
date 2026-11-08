package com.letsplay.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI letsPlayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Let's Play API")
                        .version("v1")
                        .description("API được chia theo nghiệp vụ: tài khoản, phòng, và hệ thống. Trong nhóm phòng, các endpoint được gom tiếp theo quản lý phòng, đề xuất, khung giờ, kế hoạch, chi tiêu, lịch và thông báo."))
                .components(new Components().addSecuritySchemes("GuestSession",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("Opaque session token")));
    }

    @Bean
    GroupedOpenApi accountsApi() {
        return GroupedOpenApi.builder()
                .group("01 - Tài khoản")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    GroupedOpenApi roomsApi() {
        return GroupedOpenApi.builder()
                .group("02 - Phòng")
                .pathsToMatch("/api/rooms/**")
                .build();
    }

    @Bean
    GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("03 - Hệ thống & Cron")
                .pathsToMatch("/api/system/**")
                .build();
    }
}