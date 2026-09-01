package br.com.portfolio.usuario.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * A origem permitida vem de variável de ambiente porque muda a cada ambiente:
 * o Vite dev server local, o Nginx no Compose e o Service/Ingress no
 * Kubernetes têm origens diferentes. Chumbar aqui obrigaria rebuild da imagem
 * a cada mudança de ambiente — o oposto do que o projeto quer demonstrar.
 *
 * Origem é sempre explícita: nunca "*".
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String[] METODOS_PERMITIDOS = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

    private final String[] origensPermitidas;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods(METODOS_PERMITIDOS);
    }
}
