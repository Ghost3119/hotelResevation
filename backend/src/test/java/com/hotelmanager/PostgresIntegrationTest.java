package com.hotelmanager;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for PostgreSQL integration tests using Testcontainers.
 *
 * <p>Lanza un PostgreSQL 16 real mediante Testcontainers y redirige el
 * datasource de Spring hacia el. Flyway ejecuta las migraciones desde
 * cero, por lo que se validan restricciones GiST (EXCLUDE), CHECK constraints,
 * indices y la compatibilidad entidad/esquema contra PostgreSQL real. Las
 * pruebas unitarias existentes con H2 NO heredan de esta clase y siguen
 * ejecutandose de forma independiente.</p>
 *
 * <p>{@link EnabledIfDockerAvailable} omite (skip) toda la clase cuando el
 * demonio Docker no es alcanzable (p. ej., Maven corriendo dentro de un
 * contenedor sin {@code docker.sock} montado), de modo que la build no falla
 * por la ausencia de Docker; cuando Docker esta presente, las pruebas se
 * ejecutan contra la base real.</p>
 *
 * <p>NOTA: {@code spring.datasource.driver-class-name} se sobrescribe al driver
 * de PostgreSQL porque {@code application-test.yml} fija el de H2 para las
 * pruebas unitarias; sin este override HikariCP intentaria usar el driver H2
 * con una URL PostgreSQL y no conectaria.</p>
 */
@SpringBootTest
@EnabledIfDockerAvailable
public abstract class PostgresIntegrationTest {

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("hoteltest")
            .withUsername("test")
            .withPassword("test");

    private static synchronized void startPostgres() {
        if (!postgres.isRunning()) {
            postgres.start();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        startPostgres();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
