package com.mes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web/MVC configuration for the service.
 *
 * <p>Registers CORS so that the React UI (served from a different origin during
 * development and deployment) can call {@code POST /api/upload} from the
 * browser without being blocked by the same-origin policy.</p>
 *
 * <p>The allowed origins default to the common local React dev servers
 * (Create React App on {@code :3000} and Vite on {@code :5173}) and can be
 * overridden without code changes via the {@code mes.cors.allowed-origins}
 * property (comma-separated list) in {@code application.properties} or as an
 * environment variable / command-line argument, e.g.</p>
 *
 * <pre>
 *   --mes.cors.allowed-origins=https://my-ui.example.com,https://staging-ui.example.com
 * </pre>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebConfig(
            @Value("${mes.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
            List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
