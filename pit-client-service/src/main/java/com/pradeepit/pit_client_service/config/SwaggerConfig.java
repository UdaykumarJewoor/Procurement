package com.pradeepit.pit_client_service.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    /**
     * Configures Swagger/OpenAPI documentation for the Talent Acquisition Portal API.
     *
     * @return an OpenAPI object containing metadata about the API
     */
    @Bean
    public OpenAPI customOpenAPI() {
        /*
         * Create and return an OpenAPI object with metadata such as the title, description,
         * and version of the API.
         */
        return new OpenAPI()
                .info(new Info().title("Talent Acquisition Portal")
                        .description("Talent Acquisition Portal Service API")
                        .version("1.0"));
    }
}