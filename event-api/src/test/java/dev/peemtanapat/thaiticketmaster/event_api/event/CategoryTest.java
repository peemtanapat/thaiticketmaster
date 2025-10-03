package dev.peemtanapat.thaiticketmaster.event_api.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

  @Test
  void constructor_WithParameters_CreatesCategory() {
    // Act
    Category category = new Category("Concert", "Music concerts and shows");

    // Assert
    assertNull(category.getId()); // ID not set until persisted
    assertEquals("Concert", category.getName());
    assertEquals("Music concerts and shows", category.getDescription());
  }

  @Test
  void defaultConstructor_CreatesEmptyCategory() {
    // Act
    Category category = new Category();

    // Assert
    assertNull(category.getId());
    assertNull(category.getName());
    assertNull(category.getDescription());
  }

  @Test
  void settersAndGetters_WorkCorrectly() {
    // Arrange
    Category category = new Category();

    // Act
    category.setId(1L);
    category.setName("Sports");
    category.setDescription("All sports events");

    // Assert
    assertEquals(1L, category.getId());
    assertEquals("Sports", category.getName());
    assertEquals("All sports events", category.getDescription());
  }

  @Test
  void onCreate_SetsCreatedAtAndUpdatedAt() {
    // Arrange
    Category category = new Category();
    java.time.LocalDateTime before = java.time.LocalDateTime.now().minusSeconds(1);

    // Act
    category.onCreate();
    java.time.LocalDateTime after = java.time.LocalDateTime.now().plusSeconds(1);

    // Assert
    assertNotNull(category.getCreatedAt());
    assertNotNull(category.getUpdatedAt());
    assertTrue(category.getCreatedAt().isAfter(before));
    assertTrue(category.getCreatedAt().isBefore(after));
    // Check timestamps are within 1 second of each other (to handle microsecond
    // differences)
    assertTrue(
        Math.abs(java.time.Duration.between(category.getCreatedAt(), category.getUpdatedAt()).toMillis()) < 1000);
  }

  @Test
  void onUpdate_UpdatesUpdatedAtOnly() throws InterruptedException {
    // Arrange
    Category category = new Category();
    category.onCreate();
    java.time.LocalDateTime originalCreatedAt = category.getCreatedAt();
    java.time.LocalDateTime originalUpdatedAt = category.getUpdatedAt();

    // Sleep to ensure time difference
    Thread.sleep(10);

    // Act
    category.onUpdate();

    // Assert
    assertEquals(originalCreatedAt, category.getCreatedAt()); // createdAt unchanged
    assertNotEquals(originalUpdatedAt, category.getUpdatedAt()); // updatedAt changed
    assertTrue(category.getUpdatedAt().isAfter(originalUpdatedAt));
  }

  @Test
  void description_CanBeNull() {
    // Arrange & Act
    Category category = new Category("Theater", null);

    // Assert
    assertEquals("Theater", category.getName());
    assertNull(category.getDescription());
  }
}
