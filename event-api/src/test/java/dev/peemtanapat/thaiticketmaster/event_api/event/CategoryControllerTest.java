package dev.peemtanapat.thaiticketmaster.event_api.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CategoryRepository categoryRepository;

  private Category testCategory;
  private Category testCategory2;

  @BeforeEach
  void setUp() {
    testCategory = new Category("Concert", "Music concerts and shows");
    testCategory.setId(1L);

    testCategory2 = new Category("Sports", "Sports events");
    testCategory2.setId(2L);
  }

  // ========== GET ALL CATEGORIES TESTS ==========

  @Test
  void getAllCategories_ReturnsListOfCategories() throws Exception {
    // Arrange
    List<Category> categories = Arrays.asList(testCategory, testCategory2);
    when(categoryRepository.findAll()).thenReturn(categories);

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").value("Concert"))
        .andExpect(jsonPath("$[0].description").value("Music concerts and shows"))
        .andExpect(jsonPath("$[1].id").value(2))
        .andExpect(jsonPath("$[1].name").value("Sports"));

    verify(categoryRepository, times(1)).findAll();
  }

  @Test
  void getAllCategories_WhenNoCategories_ReturnsEmptyList() throws Exception {
    // Arrange
    when(categoryRepository.findAll()).thenReturn(Arrays.asList());

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(0)));

    verify(categoryRepository, times(1)).findAll();
  }

  // ========== GET CATEGORY BY ID TESTS ==========

  @Test
  void getCategoryById_WhenCategoryExists_ReturnsCategory() throws Exception {
    // Arrange
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Concert"))
        .andExpect(jsonPath("$.description").value("Music concerts and shows"));

    verify(categoryRepository, times(1)).findById(1L);
  }

  @Test
  void getCategoryById_WhenCategoryNotFound_ReturnsNotFound() throws Exception {
    // Arrange
    when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/999"))
        .andExpect(status().isNotFound());

    verify(categoryRepository, times(1)).findById(999L);
  }

  // ========== GET CATEGORY BY NAME TESTS ==========

  @Test
  void getCategoryByName_WhenCategoryExists_ReturnsCategory() throws Exception {
    // Arrange
    when(categoryRepository.findByName("Concert")).thenReturn(Optional.of(testCategory));

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/name/Concert"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Concert"))
        .andExpect(jsonPath("$.description").value("Music concerts and shows"));

    verify(categoryRepository, times(1)).findByName("Concert");
  }

  @Test
  void getCategoryByName_WhenCategoryNotFound_ReturnsNotFound() throws Exception {
    // Arrange
    when(categoryRepository.findByName("NonExistent")).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/name/NonExistent"))
        .andExpect(status().isNotFound());

    verify(categoryRepository, times(1)).findByName("NonExistent");
  }

  @Test
  void getCategoryByName_WithSpecialCharacters_HandlesCorrectly() throws Exception {
    // Arrange
    Category specialCategory = new Category("Arts & Crafts", "Arts and crafts events");
    specialCategory.setId(3L);
    when(categoryRepository.findByName("Arts & Crafts")).thenReturn(Optional.of(specialCategory));

    // Act & Assert
    mockMvc.perform(get("/api/v1/categories/name/Arts & Crafts"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(3))
        .andExpect(jsonPath("$.name").value("Arts & Crafts"));

    verify(categoryRepository, times(1)).findByName("Arts & Crafts");
  }
}
