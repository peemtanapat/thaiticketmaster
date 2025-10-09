package dev.peemtanapat.thaiticketmaster.event_api.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.peemtanapat.thaiticketmaster.event_api.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EventController with real database and full Spring
 * context.
 * Tests the complete REST API flow including controller, service, repository,
 * and database.
 */
@AutoConfigureMockMvc
class EventControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private Category testCategory;
  private OffsetDateTime futureDate;

  @BeforeEach
  void setUp() {
    testCategory = new Category("Concert", "Music concerts");
    testCategory = categoryRepository.save(testCategory);
    futureDate = OffsetDateTime.now().plusDays(30);
  }

  // ========== GET ALL EVENTS TESTS ==========

  @Test
  void getAllEvents_WhenEventsExist_ReturnsEventsList() throws Exception {
    // Arrange
    Event event1 = createAndSaveEvent("Event 1", EventStatus.ON_SALE);
    Event event2 = createAndSaveEvent("Event 2", EventStatus.COMING_SOON);

    // Act & Assert
    mockMvc.perform(get("/api/v1/events"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
        .andExpect(jsonPath("$[?(@.name == 'Event 1')]").exists())
        .andExpect(jsonPath("$[?(@.name == 'Event 2')]").exists());
  }

  @Test
  void getAllEvents_WhenNoEvents_ReturnsEmptyList() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/v1/events"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", isA(java.util.List.class)));
  }

  // ========== GET EVENT BY ID TESTS ==========

  @Test
  void getEventById_WhenEventExists_ReturnsEvent() throws Exception {
    // Arrange
    Event event = createAndSaveEvent("Test Concert", EventStatus.ON_SALE);

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/{id}", event.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(event.getId()))
        .andExpect(jsonPath("$.name").value("Test Concert"))
        .andExpect(jsonPath("$.eventStatus").value("ON_SALE"))
        .andExpect(jsonPath("$.category.name").value("Concert"));
  }

  @Test
  void getEventById_WhenEventNotFound_Returns404() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/v1/events/{id}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value(containsString("Event not found")));
  }

  // ========== GET ON SALE EVENTS TESTS ==========

  @Test
  void getOnSaleEvents_ReturnsOnlyOnSaleEvents() throws Exception {
    // Arrange
    Event onSaleEvent = createAndSaveEvent("On Sale Event", EventStatus.ON_SALE);
    onSaleEvent.setOnSaleDateTime(LocalDateTime.now().minusDays(1));
    eventRepository.save(onSaleEvent);

    Event comingSoonEvent = createAndSaveEvent("Coming Soon Event", EventStatus.COMING_SOON);

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/on-sale"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[?(@.name == 'On Sale Event')]").exists())
        .andExpect(jsonPath("$[?(@.name == 'Coming Soon Event')]").doesNotExist());
  }

  // ========== GET EVENTS BY CATEGORY TESTS ==========

  @Test
  void getEventsByCategory_WhenCategoryExists_ReturnsEvents() throws Exception {
    // Arrange
    Event event = createAndSaveEvent("Concert Event", EventStatus.ON_SALE);

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/category/{categoryId}", testCategory.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[?(@.name == 'Concert Event')]").exists())
        .andExpect(jsonPath("$[0].category.id").value(testCategory.getId()));
  }

  @Test
  void getEventsByCategory_WhenCategoryNotFound_Returns404() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/v1/events/category/{categoryId}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value(containsString("Category not found")));
  }

  // ========== GET EVENTS BY STATUS TESTS ==========

  @Test
  void getEventsByStatus_ReturnsFilteredEvents() throws Exception {
    // Arrange
    createAndSaveEvent("Sold Out Event", EventStatus.SOLD_OUT);
    createAndSaveEvent("On Sale Event", EventStatus.ON_SALE);

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/status/{status}", "SOLD_OUT"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[?(@.name == 'Sold Out Event')]").exists())
        .andExpect(jsonPath("$[?(@.name == 'On Sale Event')]").doesNotExist());
  }

  // ========== CREATE EVENT TESTS ==========

  @Test
  void createEvent_WithValidData_ReturnsCreatedEvent() throws Exception {
    // Arrange
    EventCreateRequest request = new EventCreateRequest();
    request.setName("New Concert");
    request.setCategoryId(testCategory.getId());
    request.setShowDateTimes(Arrays.asList(futureDate));
    request.setLocation("New Arena");
    request.setOnSaleDateTime(LocalDateTime.now().plusDays(1));
    request.setTicketPrice(new BigDecimal("2000.00"));
    request.setDetail("Amazing concert");
    request.setCondition("No refunds");
    request.setEventStatus(EventStatus.COMING_SOON);
    request.setGateOpen("2 hours before");

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("New Concert"))
        .andExpect(jsonPath("$.ticketPrice").value(2000.00))
        .andExpect(jsonPath("$.eventStatus").value("COMING_SOON"));
  }

  @Test
  void createEvent_WithInvalidData_Returns400() throws Exception {
    // Arrange - Missing required fields
    EventCreateRequest request = new EventCreateRequest();
    request.setName(""); // Invalid: blank

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors").isMap());
  }

  @Test
  void createEvent_WithNonExistentCategory_Returns404() throws Exception {
    // Arrange
    EventCreateRequest request = new EventCreateRequest();
    request.setName("Test Event");
    request.setCategoryId(999L); // Non-existent
    request.setShowDateTimes(Arrays.asList(futureDate));
    request.setLocation("Test Location");
    request.setOnSaleDateTime(LocalDateTime.now().plusDays(1));
    request.setTicketPrice(new BigDecimal("1000.00"));
    request.setEventStatus(EventStatus.ON_SALE);

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value(containsString("Category not found")));
  }

  // ========== UPDATE EVENT TESTS ==========

  @Test
  void updateEvent_WithValidData_ReturnsUpdatedEvent() throws Exception {
    // Arrange
    Event event = createAndSaveEvent("Original Name", EventStatus.ON_SALE);

    EventUpdateRequest request = new EventUpdateRequest();
    request.setName("Updated Name");
    request.setTicketPrice(new BigDecimal("1800.00"));
    request.setEventStatus(EventStatus.SOLD_OUT);

    // Act & Assert
    mockMvc.perform(put("/api/v1/events/{id}", event.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(event.getId()))
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.ticketPrice").value(1800.00))
        .andExpect(jsonPath("$.eventStatus").value("SOLD_OUT"));
  }

  @Test
  void updateEvent_WhenEventNotFound_Returns404() throws Exception {
    // Arrange
    EventUpdateRequest request = new EventUpdateRequest();
    request.setName("Updated Name");

    // Act & Assert
    mockMvc.perform(put("/api/v1/events/{id}", 999L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value(containsString("Event not found")));
  }

  @Test
  void updateEvent_WithInvalidPrice_Returns400() throws Exception {
    // Arrange
    Event event = createAndSaveEvent("Test Event", EventStatus.ON_SALE);

    EventUpdateRequest request = new EventUpdateRequest();
    request.setTicketPrice(new BigDecimal("-100.00")); // Invalid: negative

    // Act & Assert
    mockMvc.perform(put("/api/v1/events/{id}", event.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").exists());
  }

  // ========== DELETE EVENT TESTS ==========

  @Test
  void deleteEvent_WhenEventExists_ReturnsNoContent() throws Exception {
    // Arrange
    Event event = createAndSaveEvent("To Delete", EventStatus.ON_SALE);
    Long eventId = event.getId();

    // Act & Assert
    mockMvc.perform(delete("/api/v1/events/{id}", eventId))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc.perform(get("/api/v1/events/{id}", eventId))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteEvent_WhenEventNotFound_Returns404() throws Exception {
    // Act & Assert
    mockMvc.perform(delete("/api/v1/events/{id}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value(containsString("Event not found")));
  }

  // ========== END-TO-END FLOW TEST ==========

  @Test
  void eventLifecycle_CreateReadUpdateDelete_WorksCorrectly() throws Exception {
    // 1. Create
    EventCreateRequest createRequest = new EventCreateRequest();
    createRequest.setName("Lifecycle Test Event");
    createRequest.setCategoryId(testCategory.getId());
    createRequest.setShowDateTimes(Arrays.asList(futureDate));
    createRequest.setLocation("Test Arena");
    createRequest.setOnSaleDateTime(LocalDateTime.now().plusDays(1));
    createRequest.setTicketPrice(new BigDecimal("1500.00"));
    createRequest.setEventStatus(EventStatus.COMING_SOON);

    String createResponse = mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long eventId = objectMapper.readTree(createResponse).get("id").asLong();

    // 2. Read
    mockMvc.perform(get("/api/v1/events/{id}", eventId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Lifecycle Test Event"))
        .andExpect(jsonPath("$.eventStatus").value("COMING_SOON"));

    // 3. Update
    EventUpdateRequest updateRequest = new EventUpdateRequest();
    updateRequest.setEventStatus(EventStatus.ON_SALE);

    mockMvc.perform(put("/api/v1/events/{id}", eventId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventStatus").value("ON_SALE"));

    // 4. Delete
    mockMvc.perform(delete("/api/v1/events/{id}", eventId))
        .andExpect(status().isNoContent());

    // 5. Verify deletion
    mockMvc.perform(get("/api/v1/events/{id}", eventId))
        .andExpect(status().isNotFound());
  }

  // ========== HELPER METHODS ==========

  private Event createAndSaveEvent(String name, EventStatus status) {
    Event event = new Event();
    event.setName(name);
    event.setCategory(testCategory);
    event.setShowDateTimes(Arrays.asList(futureDate));
    event.setLocation("Test Venue");
    event.setOnSaleDateTime(LocalDateTime.now().plusDays(1));
    event.setTicketPrice(new BigDecimal("1500.00"));
    event.setDetail("Test details");
    event.setCondition("Test conditions");
    event.setEventStatus(status);
    event.setGateOpen("1 hour before");
    return eventRepository.save(event);
  }
}
