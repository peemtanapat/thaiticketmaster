package dev.peemtanapat.thaiticketmaster.event_api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration that provides Testcontainers for PostgreSQL and Redis
 * for integration tests. These containers will be shared across all tests
 * that use this configuration.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

  /**
   * PostgreSQL container for integration tests.
   * The @ServiceConnection annotation automatically configures Spring Boot
   * to use this container's connection details.
   */
  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true); // Reuse container across tests for faster execution
  }

  /**
   * Redis container for integration tests.
   * The @ServiceConnection annotation automatically configures Spring Boot
   * to use this container's connection details.
   */
  @Bean
  @ServiceConnection(name = "redis")
  GenericContainer<?> redisContainer() {
    return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withReuse(true); // Reuse container across tests for faster execution
  }
}
