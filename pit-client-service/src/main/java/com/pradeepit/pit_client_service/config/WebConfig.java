package com.pradeepit.pit_client_service.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${ui.url-port}")
    private String url;

    /**
     * Configures Cross-Origin Resource Sharing (CORS) settings.
     *
     * @param registry the CorsRegistry to add CORS mappings
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        /*
         * Allow requests from the specified origin (React app running on localhost:5173).
         * Specify the allowed HTTP methods for cross-origin requests: GET, POST, PUT, DELETE, OPTIONS.
         * Allow credentials for authentication.
         * Cache preflight response for 1 hour to optimize network performance.
         */
        registry.addMapping("/**")
                .allowedOrigins(url)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

    }
}
