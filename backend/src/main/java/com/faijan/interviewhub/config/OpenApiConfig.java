package com.faijan.interviewhub.config;
import io.swagger.v3.oas.models.OpenAPI;import io.swagger.v3.oas.models.info.Info;import io.swagger.v3.oas.models.Components;import io.swagger.v3.oas.models.security.SecurityScheme;import io.swagger.v3.oas.models.security.SecurityRequirement;import org.springframework.context.annotation.Bean;import org.springframework.context.annotation.Configuration;
@Configuration public class OpenApiConfig {
 @Bean OpenAPI interviewHubOpenApi(){return new OpenAPI().info(new Info().title("BackendHub API").version("1.0.0").description("Java backend interview preparation, account, practice and analytics APIs." )).components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))).addSecurityItem(new SecurityRequirement().addList("bearerAuth"));}
}
