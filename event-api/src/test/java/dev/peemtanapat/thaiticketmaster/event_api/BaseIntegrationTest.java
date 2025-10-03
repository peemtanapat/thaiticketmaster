package dev.peemtanapat.thaiticketmaster.event_api;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.peemtanapat.thaiticketmaster.event_api.event.CategoryRepository;
import dev.peemtanapat.thaiticketmaster.event_api.event.EventRepository;

/**
 * Base class for integration tests that need a full Spring application context
 * with real database (PostgreSQL via Testcontainers) and Redis.
 * 
 * All integration tests should extend this class to get:
 * - Full Spring Boot context
 * - PostgreSQL database (via Testcontainers)
 * - Redis cache (via Testcontainers)
 * - Clean state before each test
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfiguration.class)
@Transactional
public abstract class BaseIntegrationTest {

  @Autowired
  protected EventRepository eventRepository;

  @Autowired
  protected CategoryRepository categoryRepository;

  @Autowired
  protected RedisTemplate<String, Object> redisTemplate;

  /**
   * Clean up data before each test to ensure test isolation.
   * Redis is cleared and database state is rolled back after each test
   * due to @Transactional annotation.
   */
  @BeforeEach
  void cleanUp() {
    // Clear Redis cache before each test
    if (redisTemplate.getConnectionFactory() != null
        && redisTemplate.getConnectionFactory().getConnection() != null) {
      try {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
      } catch (Exception e) {
        // Ignore if Redis is not available
      }
    }
  }
}
