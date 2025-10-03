package dev.peemtanapat.thaiticketmaster.event_api.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryDTOTest {

  @Test
  void constructor_WithCategory_CreatesCategoryDTO() {
    // Arrange
    Category category = new Category("Concert", "Music concerts");
    category.setId(1L);

    // Act
    EventDTO.CategoryDTO dto = new EventDTO.CategoryDTO(category);

    // Assert
    assertEquals(category.getId(), dto.getId());
    assertEquals(category.getName(), dto.getName());
    assertEquals(category.getDescription(), dto.getDescription());
  }

  @Test
  void defaultConstructor_CreatesEmptyDTO() {
    // Act
    EventDTO.CategoryDTO dto = new EventDTO.CategoryDTO();

    // Assert
    assertNull(dto.getId());
    assertNull(dto.getName());
    assertNull(dto.getDescription());
  }

  @Test
  void settersAndGetters_WorkCorrectly() {
    // Arrange
    EventDTO.CategoryDTO dto = new EventDTO.CategoryDTO();

    // Act
    dto.setId(3L);
    dto.setName("Sports");
    dto.setDescription("All sports events");

    // Assert
    assertEquals(3L, dto.getId());
    assertEquals("Sports", dto.getName());
    assertEquals("All sports events", dto.getDescription());
  }

  @Test
  void constructor_WithNullDescription_HandlesCorrectly() {
    // Arrange
    Category category = new Category("Theater", null);
    category.setId(2L);

    // Act
    EventDTO.CategoryDTO dto = new EventDTO.CategoryDTO(category);

    // Assert
    assertEquals(2L, dto.getId());
    assertEquals("Theater", dto.getName());
    assertNull(dto.getDescription());
  }
}
