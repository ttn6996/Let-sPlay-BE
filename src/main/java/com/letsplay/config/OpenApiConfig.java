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
                        .description("API cho tạo và tham gia phòng, kiểm tra tình trạng hệ thống và nhận cron ping."))
                .components(new Components().addSecuritySchemes("GuestSession",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("Opaque session token")));
    }

    @Bean
    GroupedOpenApi roomsApi() {
        return GroupedOpenApi.builder().group("01 - Rooms").pathsToMatch("/api/rooms/**").build();
    }

    @Bean
    GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder().group("02 - System and Cron").pathsToMatch("/api/system/**").build();
    }
}
