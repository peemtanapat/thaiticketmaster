package dev.peemtanapat.thaiticketmaster.event_api.event;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

  private final CategoryRepository categoryRepository;

  public CategoryController(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  /**
   * Get all categories
   */
  @GetMapping
  public ResponseEntity<List<CategoryDTO>> getAllCategories() {
    List<CategoryDTO> categories = categoryRepository.findAll().stream()
        .map(CategoryDTO::new)
        .collect(Collectors.toList());
    return ResponseEntity.ok(categories);
  }

  /**
   * Get category by ID
   */
  @GetMapping("/{id}")
  public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
    Category category = categoryRepository.findById(id)
        .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
    return ResponseEntity.ok(new CategoryDTO(category));
  }

  /**
   * Get category by name
   */
  @GetMapping("/name/{name}")
  public ResponseEntity<CategoryDTO> getCategoryByName(@PathVariable String name) {
    Category category = categoryRepository.findByName(name)
        .orElseThrow(() -> new CategoryNotFoundException("Category not found with name: " + name));
    return ResponseEntity.ok(new CategoryDTO(category));
  }

  // Simple DTO for Category
  static class CategoryDTO {
    private Long id;
    private String name;
    private String description;

    public CategoryDTO() {
    }

    public CategoryDTO(Category category) {
      this.id = category.getId();
      this.name = category.getName();
      this.description = category.getDescription();
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }
}
