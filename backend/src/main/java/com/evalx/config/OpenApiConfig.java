package com.evalx.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI evalxOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EvalX API")
                        .description("AI-Ready Exam Analytics Platform")
                        .version("1.0.0"));
    }
}
