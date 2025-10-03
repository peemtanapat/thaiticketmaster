package dev.peemtanapat.thaiticketmaster.event_api.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class GlobalExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private EventService eventService;

  // ========== EVENT NOT FOUND EXCEPTION TESTS ==========

  @Test
  void handleEventNotFoundException_ReturnsNotFoundWithErrorResponse() throws Exception {
    // Arrange
    when(eventService.getEventById(999L))
        .thenThrow(new EventNotFoundException("Event not found with id: 999"));

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/999"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("Event not found with id: 999"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  // ========== CATEGORY NOT FOUND EXCEPTION TESTS ==========

  @Test
  void handleCategoryNotFoundException_ReturnsNotFoundWithErrorResponse() throws Exception {
    // Arrange
    when(eventService.getEventsByCategory(999L))
        .thenThrow(new CategoryNotFoundException("Category not found with id: 999"));

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/category/999"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("Category not found with id: 999"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  // ========== VALIDATION EXCEPTION TESTS ==========

  @Test
  void handleValidationException_WhenNameIsBlank_ReturnsBadRequestWithErrors() throws Exception {
    // Arrange
    EventCreateRequest request = new EventCreateRequest();
    request.setName(""); // Invalid: blank
    request.setCategoryId(1L);
    request.setShowDateTimes(Arrays.asList(LocalDateTime.now().plusDays(30)));
    request.setLocation("Test Venue");
    request.setOnSaleDateTime(LocalDateTime.now().plusDays(1));
    request.setTicketPrice(new BigDecimal("1500.00"));
    request.setEventStatus(EventStatus.ON_SALE);

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.errors").exists())
        .andExpect(jsonPath("$.errors.name").exists());

    verify(eventService, never()).createEvent(any(EventCreateRequest.class));
  }

  @Test
  void handleValidationException_WhenMultipleFieldsInvalid_ReturnsAllErrors() throws Exception {
    // Arrange
    EventCreateRequest request = new EventCreateRequest();
    request.setName(""); // Invalid: blank
    request.setCategoryId(null); // Invalid: null
    request.setShowDateTimes(Arrays.asList()); // Invalid: empty
    request.setLocation(""); // Invalid: blank
    request.setOnSaleDateTime(null); // Invalid: null
    request.setTicketPrice(null); // Invalid: null
    request.setEventStatus(null); // Invalid: null

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors").isMap())
        .andExpect(jsonPath("$.errors.name").exists())
        .andExpect(jsonPath("$.errors.categoryId").exists())
        .andExpect(jsonPath("$.errors.showDateTimes").exists())
        .andExpect(jsonPath("$.errors.location").exists())
        .andExpect(jsonPath("$.errors.onSaleDateTime").exists())
        .andExpect(jsonPath("$.errors.ticketPrice").exists())
        .andExpect(jsonPath("$.errors.eventStatus").exists());

    verify(eventService, never()).createEvent(any(EventCreateRequest.class));
  }

  @Test
  void handleValidationException_WhenTicketPriceIsZero_ReturnsError() throws Exception {
    // Arrange
    EventCreateRequest request = new EventCreateRequest();
    request.setName("Test Event");
    request.setCategoryId(1L);
    request.setShowDateTimes(Arrays.asList(LocalDateTime.now().plusDays(30)));
    request.setLocation("Test Venue");
    request.setOnSaleDateTime(LocalDateTime.now().plusDays(1));
    request.setTicketPrice(BigDecimal.ZERO); // Invalid: must be > 0
    request.setEventStatus(EventStatus.ON_SALE);

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors.ticketPrice").exists());

    verify(eventService, never()).createEvent(any(EventCreateRequest.class));
  }

  @Test
  void handleValidationException_WhenNameTooLong_ReturnsError() throws Exception {
    // Arrange
    EventCreateRequest request = new EventCreateRequest();
    request.setName("A".repeat(201)); // Invalid: exceeds 200 characters
    request.setCategoryId(1L);
    request.setShowDateTimes(Arrays.asList(LocalDateTime.now().plusDays(30)));
    request.setLocation("Test Venue");
    request.setOnSaleDateTime(LocalDateTime.now().plusDays(1));
    request.setTicketPrice(new BigDecimal("1500.00"));
    request.setEventStatus(EventStatus.ON_SALE);

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors.name").exists());

    verify(eventService, never()).createEvent(any(EventCreateRequest.class));
  }

  // ========== GENERAL EXCEPTION TESTS ==========

  @Test
  void handleGeneralException_ReturnsInternalServerError() throws Exception {
    // Arrange
    when(eventService.getAllEvents())
        .thenThrow(new RuntimeException("Unexpected database error"));

    // Act & Assert
    mockMvc.perform(get("/api/v1/events"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.message").value(containsString("An unexpected error occurred")))
        .andExpect(jsonPath("$.timestamp").exists());
  }
}
