package com.hotelmanager;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Omite (skip) las pruebas si el demonio Docker no es alcanzable, para que las
 * pruebas de integracion con Testcontainers degraden de forma graciosa en
 * entornos sin acceso a Docker (p. ej., Maven dentro de un contenedor sin
 * docker.sock montado). Cuando Docker SI esta disponible, las pruebas se
 * ejecutan normalmente contra un PostgreSQL 16 real levantado por Testcontainers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(DockerAvailableCondition.class)
public @interface EnabledIfDockerAvailable {
}
