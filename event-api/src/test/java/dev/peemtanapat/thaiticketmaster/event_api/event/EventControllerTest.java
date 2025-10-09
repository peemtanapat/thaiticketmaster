package dev.peemtanapat.thaiticketmaster.event_api.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private EventService eventService;

  private EventDTO testEventDTO;
  private EventCreateRequest createRequest;
  private EventUpdateRequest updateRequest;
  private EventDTO.CategoryDTO testCategoryDTO;

  @BeforeEach
  void setUp() {
    testCategoryDTO = new EventDTO.CategoryDTO();
    testCategoryDTO.setId(1L);
    testCategoryDTO.setName("Concert");
    testCategoryDTO.setDescription("Music concerts");

    testEventDTO = new EventDTO();
    testEventDTO.setId(1L);
    testEventDTO.setName("Test Concert");
    testEventDTO.setCategory(testCategoryDTO);
    testEventDTO.setShowDateTimes(Arrays.asList(OffsetDateTime.now().plusDays(30)));
    testEventDTO.setLocation("Test Venue");
    testEventDTO.setOnSaleDateTime(LocalDateTime.now().plusDays(1));
    testEventDTO.setTicketPrice(new BigDecimal("1500.00"));
    testEventDTO.setDetail("Test details");
    testEventDTO.setCondition("Test conditions");
    testEventDTO.setEventStatus(EventStatus.ON_SALE);
    testEventDTO.setGateOpen("1 hour before");

    createRequest = new EventCreateRequest();
    createRequest.setName("New Concert");
    createRequest.setCategoryId(1L);
    createRequest.setShowDateTimes(Arrays.asList(OffsetDateTime.now().plusDays(30)));
    createRequest.setLocation("New Venue");
    createRequest.setOnSaleDateTime(LocalDateTime.now().plusDays(1));
    createRequest.setTicketPrice(new BigDecimal("2000.00"));
    createRequest.setDetail("New details");
    createRequest.setCondition("New conditions");
    createRequest.setEventStatus(EventStatus.COMING_SOON);
    createRequest.setGateOpen("30 minutes before");

    updateRequest = new EventUpdateRequest();
    updateRequest.setName("Updated Concert");
    updateRequest.setTicketPrice(new BigDecimal("1800.00"));
  }

  // ========== GET ALL EVENTS TESTS ==========

  @Test
  void getAllEvents_ReturnsListOfEvents() throws Exception {
    // Arrange
    List<EventDTO> events = Arrays.asList(testEventDTO);
    when(eventService.getAllEvents()).thenReturn(events);

    // Act & Assert
    mockMvc.perform(get("/api/v1/events"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").value("Test Concert"))
        .andExpect(jsonPath("$[0].category.name").value("Concert"));

    verify(eventService, times(1)).getAllEvents();
  }

  @Test
  void getAllEvents_WhenNoEvents_ReturnsEmptyList() throws Exception {
    // Arrange
    when(eventService.getAllEvents()).thenReturn(Arrays.asList());

    // Act & Assert
    mockMvc.perform(get("/api/v1/events"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(0)));

    verify(eventService, times(1)).getAllEvents();
  }

  // ========== GET EVENT BY ID TESTS ==========

  @Test
  void getEventById_WhenEventExists_ReturnsEvent() throws Exception {
    // Arrange
    when(eventService.getEventById(1L)).thenReturn(testEventDTO);

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Test Concert"))
        .andExpect(jsonPath("$.eventStatus").value("ON_SALE"));

    verify(eventService, times(1)).getEventById(1L);
  }

  @Test
  void getEventById_WhenEventNotFound_ReturnsNotFound() throws Exception {
    // Arrange
    when(eventService.getEventById(999L))
        .thenThrow(new EventNotFoundException("Event not found with id: 999"));

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/999"))
        .andExpect(status().isNotFound());

    verify(eventService, times(1)).getEventById(999L);
  }

  // ========== GET ON SALE EVENTS TESTS ==========

  @Test
  void getOnSaleEvents_ReturnsOnSaleEvents() throws Exception {
    // Arrange
    List<EventDTO> events = Arrays.asList(testEventDTO);
    when(eventService.getOnSaleEvents()).thenReturn(events);

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/on-sale"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].eventStatus").value("ON_SALE"));

    verify(eventService, times(1)).getOnSaleEvents();
  }

  // ========== GET EVENTS BY CATEGORY TESTS ==========

  @Test
  void getEventsByCategory_WhenCategoryExists_ReturnsEvents() throws Exception {
    // Arrange
    List<EventDTO> events = Arrays.asList(testEventDTO);
    when(eventService.getEventsByCategory(1L)).thenReturn(events);

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/category/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].category.id").value(1));

    verify(eventService, times(1)).getEventsByCategory(1L);
  }

  @Test
  void getEventsByCategory_WhenCategoryNotFound_ReturnsNotFound() throws Exception {
    // Arrange
    when(eventService.getEventsByCategory(999L))
        .thenThrow(new CategoryNotFoundException("Category not found with id: 999"));

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/category/999"))
        .andExpect(status().isNotFound());

    verify(eventService, times(1)).getEventsByCategory(999L);
  }

  // ========== GET EVENTS BY STATUS TESTS ==========

  @Test
  void getEventsByStatus_ReturnsFilteredEvents() throws Exception {
    // Arrange
    List<EventDTO> events = Arrays.asList(testEventDTO);
    when(eventService.getEventsByStatus(EventStatus.ON_SALE)).thenReturn(events);

    // Act & Assert
    mockMvc.perform(get("/api/v1/events/status/ON_SALE"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].eventStatus").value("ON_SALE"));

    verify(eventService, times(1)).getEventsByStatus(EventStatus.ON_SALE);
  }

  // ========== CREATE EVENT TESTS ==========

  @Test
  void createEvent_WhenValid_ReturnsCreatedEvent() throws Exception {
    // Arrange
    when(eventService.createEvent(any(EventCreateRequest.class))).thenReturn(testEventDTO);

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Test Concert"));

    verify(eventService, times(1)).createEvent(any(EventCreateRequest.class));
  }

  @Test
  void createEvent_WhenNameIsBlank_ReturnsBadRequest() throws Exception {
    // Arrange
    createRequest.setName("");

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());

    verify(eventService, never()).createEvent(any(EventCreateRequest.class));
  }

  @Test
  void createEvent_WhenCategoryIdIsNull_ReturnsBadRequest() throws Exception {
    // Arrange
    createRequest.setCategoryId(null);

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());

    verify(eventService, never()).createEvent(any(EventCreateRequest.class));
  }

  @Test
  void createEvent_WhenShowDateTimesIsEmpty_ReturnsBadRequest() throws Exception {
    // Arrange
    createRequest.setShowDateTimes(Arrays.asList());

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());

    verify(eventService, never()).createEvent(any(EventCreateRequest.class));
  }

  @Test
  void createEvent_WhenTicketPriceIsZero_ReturnsBadRequest() throws Exception {
    // Arrange
    createRequest.setTicketPrice(BigDecimal.ZERO);

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());

    verify(eventService, never()).createEvent(any(EventCreateRequest.class));
  }

  @Test
  void createEvent_WhenEventStatusIsNull_ReturnsBadRequest() throws Exception {
    // Arrange
    createRequest.setEventStatus(null);

    // Act & Assert
    mockMvc.perform(post("/api/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());

    verify(eventService, never()).createEvent(any(EventCreateRequest.class));
  }

  // ========== UPDATE EVENT TESTS ==========

  @Test
  void updateEvent_WhenValid_ReturnsUpdatedEvent() throws Exception {
    // Arrange
    EventDTO updatedDTO = new EventDTO();
    updatedDTO.setId(1L);
    updatedDTO.setName("Updated Concert");
    updatedDTO.setCategory(testCategoryDTO);
    updatedDTO.setTicketPrice(new BigDecimal("1800.00"));

    when(eventService.updateEvent(eq(1L), any(EventUpdateRequest.class))).thenReturn(updatedDTO);

    // Act & Assert
    mockMvc.perform(put("/api/v1/events/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Updated Concert"));

    verify(eventService, times(1)).updateEvent(eq(1L), any(EventUpdateRequest.class));
  }

  @Test
  void updateEvent_WhenEventNotFound_ReturnsNotFound() throws Exception {
    // Arrange
    when(eventService.updateEvent(eq(999L), any(EventUpdateRequest.class)))
        .thenThrow(new EventNotFoundException("Event not found with id: 999"));

    // Act & Assert
    mockMvc.perform(put("/api/v1/events/999")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());

    verify(eventService, times(1)).updateEvent(eq(999L), any(EventUpdateRequest.class));
  }

  @Test
  void updateEvent_WhenTicketPriceIsNegative_ReturnsBadRequest() throws Exception {
    // Arrange
    updateRequest.setTicketPrice(new BigDecimal("-100.00"));

    // Act & Assert
    mockMvc.perform(put("/api/v1/events/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isBadRequest());

    verify(eventService, never()).updateEvent(eq(1L), any(EventUpdateRequest.class));
  }

  // ========== DELETE EVENT TESTS ==========

  @Test
  void deleteEvent_WhenEventExists_ReturnsNoContent() throws Exception {
    // Arrange
    doNothing().when(eventService).deleteEvent(1L);

    // Act & Assert
    mockMvc.perform(delete("/api/v1/events/1"))
        .andExpect(status().isNoContent());

    verify(eventService, times(1)).deleteEvent(1L);
  }

  @Test
  void deleteEvent_WhenEventNotFound_ReturnsNotFound() throws Exception {
    // Arrange
    doThrow(new EventNotFoundException("Event not found with id: 999"))
        .when(eventService).deleteEvent(999L);

    // Act & Assert
    mockMvc.perform(delete("/api/v1/events/999"))
        .andExpect(status().isNotFound());

    verify(eventService, times(1)).deleteEvent(999L);
  }
}
