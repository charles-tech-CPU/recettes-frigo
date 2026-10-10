package com.charles.recettesfrigo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebConfigTest {

    private static final String[] DEFAULTS = {"http://*:5178", "https://*.ts.net:[*]"};

    /** Expose la configuration CORS enregistree (methode protegee de CorsRegistry). */
    private static final class InspectableCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }

    private static CorsConfiguration apiCors(String allowedOrigins) {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        new WebConfig(allowedOrigins).addCorsMappings(registry);
        return registry.configurations().get("/api/**");
    }

    @Test
    void sansVariableLeFrontendViteEtTailscaleSontAutorises() {
        CorsConfiguration api = apiCors("");

        assertThat(api.getAllowedOriginPatterns()).containsExactly(DEFAULTS);
        assertThat(api.checkOrigin("http://localhost:5178")).isEqualTo("http://localhost:5178");
        assertThat(api.checkOrigin("http://192.168.1.20:5178")).isEqualTo("http://192.168.1.20:5178");
        assertThat(api.checkOrigin("http://autre-site.example")).isNull();
        assertThat(api.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE");
    }

    @Test
    void uneListeRemplaceLesMotifsParDefaut() {
        CorsConfiguration api = apiCors("http://*:8180, https://*:8280");

        assertThat(api.getAllowedOriginPatterns()).containsExactly("http://*:8180", "https://*:8280");
        assertThat(api.checkOrigin("http://localhost:8180")).isEqualTo("http://localhost:8180");
        assertThat(api.checkOrigin("http://localhost:5178")).isNull();
    }

    @Test
    void decoupageDeLaListe() {
        assertThat(WebConfig.parseOrigins(" http://*:8180 , ,https://*:8280, "))
                .containsExactly("http://*:8180", "https://*:8280");
        assertThat(WebConfig.parseOrigins(null)).containsExactly(DEFAULTS);
        assertThat(WebConfig.parseOrigins(" , ")).containsExactly(DEFAULTS);
    }
}
