package dev.peemtanapat.thaiticketmaster.event_api.event;

import dev.peemtanapat.thaiticketmaster.event_api.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CategoryController with real database and full Spring
 * context.
 * Tests the complete REST API flow for category operations.
 */
@AutoConfigureMockMvc
class CategoryControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  // ========== GET ALL CATEGORIES TESTS ==========

  @Test
  void getAllCategories_WhenCategoriesExist_ReturnsAllCategories() throws Exception {
    // Arrange
    Category concert = new Category("Concert", "Music concerts and shows");
    Category sports = new Category("Sports", "Sports events and matches");
    categoryRepository.save(concert);
    categoryRepository.save(sports);

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
        .andExpect(jsonPath("$[?(@.name == 'Concert')]").exists())
        .andExpect(jsonPath("$[?(@.name == 'Sports')]").exists())
        .andExpect(jsonPath("$[?(@.name == 'Concert')].description").value(hasItem("Music concerts and shows")));
  }

  @Test
  void getAllCategories_WhenNoCategories_ReturnsEmptyList() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/v1/categories"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", isA(java.util.List.class)));
  }

  // ========== GET CATEGORY BY ID TESTS ==========

  @Test
  void getCategoryById_WhenCategoryExists_ReturnsCategory() throws Exception {
    // Arrange
    Category category = new Category("Theater", "Theater and drama shows");
    category = categoryRepository.save(category);

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/{id}", category.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(category.getId()))
        .andExpect(jsonPath("$.name").value("Theater"))
        .andExpect(jsonPath("$.description").value("Theater and drama shows"));
  }

  @Test
  void getCategoryById_WhenCategoryNotFound_Returns404() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/{id}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value(containsString("Category not found with id: 999")))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  // ========== GET CATEGORY BY NAME TESTS ==========

  @Test
  void getCategoryByName_WhenCategoryExists_ReturnsCategory() throws Exception {
    // Arrange
    Category category = new Category("Festival", "Music and cultural festivals");
    categoryRepository.save(category);

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/name/{name}", "Festival"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("Festival"))
        .andExpect(jsonPath("$.description").value("Music and cultural festivals"));
  }

  @Test
  void getCategoryByName_WhenCategoryNotFound_Returns404() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/name/{name}", "NonExistent"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value(containsString("Category not found with name: NonExistent")));
  }

  @Test
  void getCategoryByName_WithSpecialCharacters_HandlesCorrectly() throws Exception {
    // Arrange
    Category category = new Category("Arts & Crafts", "Arts, crafts, and DIY events");
    categoryRepository.save(category);

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/name/{name}", "Arts & Crafts"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("Arts & Crafts"))
        .andExpect(jsonPath("$.description").value("Arts, crafts, and DIY events"));
  }

  @Test
  void getCategoryByName_CaseSensitive_FindsExactMatch() throws Exception {
    // Arrange
    Category category = new Category("CONCERT", "Uppercase concert category");
    categoryRepository.save(category);

    // Act & Assert - Should find exact match
    mockMvc.perform(get("/api/v1/categories/name/{name}", "CONCERT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("CONCERT"));

    // Different case should not find it (assuming case-sensitive search)
    mockMvc.perform(get("/api/v1/categories/name/{name}", "concert"))
        .andExpect(status().isNotFound());
  }

  // ========== CATEGORIES WITH EVENTS TESTS ==========

  @Test
  void getCategoryById_WithAssociatedEvents_ReturnsCategory() throws Exception {
    // Arrange
    Category category = new Category("Concert", "Music concerts");
    category = categoryRepository.save(category);

    // Create events in this category
    Event event1 = new Event();
    event1.setName("Rock Concert");
    event1.setCategory(category);
    event1.setShowDateTimes(java.util.Arrays.asList(java.time.LocalDateTime.now().plusDays(30)));
    event1.setLocation("Stadium");
    event1.setOnSaleDateTime(java.time.LocalDateTime.now().plusDays(1));
    event1.setTicketPrice(java.math.BigDecimal.valueOf(1500));
    event1.setEventStatus(EventStatus.ON_SALE);
    eventRepository.save(event1);

    Event event2 = new Event();
    event2.setName("Pop Concert");
    event2.setCategory(category);
    event2.setShowDateTimes(java.util.Arrays.asList(java.time.LocalDateTime.now().plusDays(45)));
    event2.setLocation("Arena");
    event2.setOnSaleDateTime(java.time.LocalDateTime.now().plusDays(2));
    event2.setTicketPrice(java.math.BigDecimal.valueOf(2000));
    event2.setEventStatus(EventStatus.COMING_SOON);
    eventRepository.save(event2);

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/{id}", category.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(category.getId()))
        .andExpect(jsonPath("$.name").value("Concert"));

    // Verify we can get events by this category
    mockMvc.perform(get("/api/v1/events/category/{categoryId}", category.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[?(@.name == 'Rock Concert')]").exists())
        .andExpect(jsonPath("$[?(@.name == 'Pop Concert')]").exists());
  }

  // ========== DATA INTEGRITY TESTS ==========

  @Test
  void getAllCategories_ReturnsDistinctCategories() throws Exception {
    // Arrange - Create categories with same name should fail due to unique
    // constraint
    Category category1 = new Category("Unique", "First one");
    categoryRepository.save(category1);

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == 'Unique')]", hasSize(1)));
  }

  @Test
  void getCategoryById_ReturnsCompleteData() throws Exception {
    // Arrange
    Category category = new Category("Complete Test", "This is a complete category");
    category = categoryRepository.save(category);

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/{id}", category.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.description").exists())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").isString())
        .andExpect(jsonPath("$.description").isString());
  }
}
