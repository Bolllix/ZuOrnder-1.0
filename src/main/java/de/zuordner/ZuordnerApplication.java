package de.zuordner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * ZuordnerApplication – hier startet das Programm
 * ============================================================================
 * {@code @SpringBootApplication} schaltet Spring Boot ein. Beim Start durchsucht
 * Spring automatisch alle Klassen im Paket "de.zuordner" (und darunter) und
 * erzeugt die mit @RestController, @Service, @Repository, @Configuration
 * markierten Klassen. Außerdem startet ein eingebauter Webserver auf Port 8080,
 * der sowohl die Webseite (React-Oberfläche) als auch die /api-Adressen bedient.
 */
@SpringBootApplication
public class ZuordnerApplication {

    /** Einstiegspunkt: Java ruft beim Start (java -jar zuordner-app.jar) diese Methode auf. */
    public static void main(String[] args) {
        SpringApplication.run(ZuordnerApplication.class, args);
    }
}
