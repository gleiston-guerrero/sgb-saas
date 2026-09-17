package com.uteq.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación Spring Boot para el servicio backend.
 *
 * Esta clase inicializa el contexto de Spring y activa la programación
 * de tareas con {@link EnableScheduling} cuando corresponde. Mantenerla
 * ligera evita dependencias de inicialización innecesarias en el arranque
 * de los tests de integración.
 */
@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    /**
     * Constructor sin argumentos para el contenedor de Spring.
     */
    public BackendApplication() {
    }

	/**
     * Punto de entrada de la aplicación Spring Boot.
     *
     * @param args argumentos de línea de comandos
     */

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
