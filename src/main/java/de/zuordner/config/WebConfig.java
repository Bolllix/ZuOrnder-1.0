package de.zuordner.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * ============================================================================
 * WebConfig – liefert die React-Oberfläche aus
 * ============================================================================
 * Die fertig gebaute Oberfläche (HTML, JavaScript, CSS) liegt im Ordner
 * src/main/resources/static und landet beim Bauen in der JAR.
 *
 * Diese Konfiguration sorgt dafür, dass:
 *  - vorhandene Dateien (z.B. /assets/index-xyz.js) direkt ausgeliefert werden,
 *  - für alle anderen Adressen die index.html geliefert wird. Das ist für eine
 *    "Single Page Application" (SPA) nötig: Die Seite wird nur einmal geladen
 *    und React kümmert sich danach selbst um die Navigation.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // Gibt es die angefragte Datei? Dann diese, sonst index.html
                        Resource requestedResource = location.createRelative(resourcePath);
                        return requestedResource.exists() && requestedResource.isReadable()
                                ? requestedResource
                                : new ClassPathResource("/static/index.html");
                    }
                });
    }
}
