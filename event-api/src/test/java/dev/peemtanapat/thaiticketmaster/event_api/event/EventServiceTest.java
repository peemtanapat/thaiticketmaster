package dev.peemtanapat.thaiticketmaster.event_api.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

  @Mock
  private EventRepository eventRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  @InjectMocks
  private EventService eventService;

  private Category testCategory;
  private Event testEvent;
  private EventDTO testEventDTO;
  private EventCreateRequest createRequest;
  private EventUpdateRequest updateRequest;

  @BeforeEach
  void setUp() {
    // Mock RedisTemplate operations with lenient stubbing (not all tests need this)
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // Set up test data
    testCategory = new Category("Concert", "Music concerts");
    testCategory.setId(1L);

    testEvent = new Event(
        "Test Concert",
        testCategory,
        Arrays.asList(LocalDateTime.now().plusDays(30)),
        "Test Venue",
        LocalDateTime.now().plusDays(1),
        new BigDecimal("1500.00"),
        "Test event details",
        "Test conditions",
        EventStatus.ON_SALE,
        "1 hour before");
    testEvent.setId(1L);

    testEventDTO = new EventDTO(testEvent);

    createRequest = new EventCreateRequest();
    createRequest.setName("New Concert");
    createRequest.setCategoryId(1L);
    createRequest.setShowDateTimes(Arrays.asList(LocalDateTime.now().plusDays(30)));
    createRequest.setLocation("New Venue");
    createRequest.setOnSaleDateTime(LocalDateTime.now().plusDays(1));
    createRequest.setTicketPrice(new BigDecimal("2000.00"));
    createRequest.setDetail("New event details");
    createRequest.setCondition("New conditions");
    createRequest.setEventStatus(EventStatus.COMING_SOON);
    createRequest.setGateOpen("30 minutes before");

    updateRequest = new EventUpdateRequest();
    updateRequest.setName("Updated Concert");
    updateRequest.setTicketPrice(new BigDecimal("1800.00"));
  }

  // ========== GET ALL EVENTS TESTS ==========

  @Test
  void getAllEvents_WhenCacheHit_ReturnsCachedData() {
    // Arrange
    List<EventDTO> cachedEvents = Arrays.asList(testEventDTO);
    when(valueOperations.get("events:all")).thenReturn(cachedEvents);

    // Act
    List<EventDTO> result = eventService.getAllEvents();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(valueOperations, times(1)).get("events:all");
    verify(eventRepository, never()).findAll();
  }

  @Test
  void getAllEvents_WhenCacheMiss_LoadsFromDatabaseAndCaches() {
    // Arrange
    when(valueOperations.get("events:all")).thenReturn(null);
    when(eventRepository.findAll()).thenReturn(Arrays.asList(testEvent));

    // Act
    List<EventDTO> result = eventService.getAllEvents();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Test Concert", result.get(0).getName());
    verify(valueOperations, times(1)).get("events:all");
    verify(eventRepository, times(1)).findAll();
    verify(valueOperations, times(1)).set(eq("events:all"), anyList(), eq(1L), eq(TimeUnit.HOURS));
  }

  @Test
  void getAllEvents_WhenNoEvents_ReturnsEmptyList() {
    // Arrange
    when(valueOperations.get("events:all")).thenReturn(null);
    when(eventRepository.findAll()).thenReturn(Arrays.asList());

    // Act
    List<EventDTO> result = eventService.getAllEvents();

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ========== GET EVENT BY ID TESTS ==========

  @Test
  void getEventById_WhenCacheHit_ReturnsCachedData() {
    // Arrange
    when(valueOperations.get("event:1")).thenReturn(testEventDTO);

    // Act
    EventDTO result = eventService.getEventById(1L);

    // Assert
    assertNotNull(result);
    assertEquals("Test Concert", result.getName());
    verify(valueOperations, times(1)).get("event:1");
    verify(eventRepository, never()).findById(anyLong());
  }

  @Test
  void getEventById_WhenCacheMiss_LoadsFromDatabaseAndCaches() {
    // Arrange
    when(valueOperations.get("event:1")).thenReturn(null);
    when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

    // Act
    EventDTO result = eventService.getEventById(1L);

    // Assert
    assertNotNull(result);
    assertEquals("Test Concert", result.getName());
    verify(valueOperations, times(1)).get("event:1");
    verify(eventRepository, times(1)).findById(1L);
    verify(valueOperations, times(1)).set(eq("event:1"), any(EventDTO.class), eq(1L), eq(TimeUnit.HOURS));
  }

  @Test
  void getEventById_WhenEventNotFound_ThrowsException() {
    // Arrange
    when(valueOperations.get("event:999")).thenReturn(null);
    when(eventRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    EventNotFoundException exception = assertThrows(EventNotFoundException.class, () -> {
      eventService.getEventById(999L);
    });

    assertTrue(exception.getMessage().contains("Event not found with id: 999"));
  }

  // ========== GET ON SALE EVENTS TESTS ==========

  @Test
  void getOnSaleEvents_WhenCacheHit_ReturnsCachedData() {
    // Arrange
    List<EventDTO> cachedEvents = Arrays.asList(testEventDTO);
    when(valueOperations.get("events:onsale")).thenReturn(cachedEvents);

    // Act
    List<EventDTO> result = eventService.getOnSaleEvents();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(valueOperations, times(1)).get("events:onsale");
    verify(eventRepository, never()).findOnSaleEventsOrderByShowDate(any());
  }

  @Test
  void getOnSaleEvents_WhenCacheMiss_LoadsFromDatabaseAndCaches() {
    // Arrange
    when(valueOperations.get("events:onsale")).thenReturn(null);
    when(eventRepository.findOnSaleEventsOrderByShowDate(any(LocalDateTime.class)))
        .thenReturn(Arrays.asList(testEvent));

    // Act
    List<EventDTO> result = eventService.getOnSaleEvents();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(valueOperations, times(1)).get("events:onsale");
    verify(eventRepository, times(1)).findOnSaleEventsOrderByShowDate(any(LocalDateTime.class));
    verify(valueOperations, times(1)).set(eq("events:onsale"), anyList(), eq(15L), eq(TimeUnit.MINUTES));
  }

  // ========== GET EVENTS BY CATEGORY TESTS ==========

  @Test
  void getEventsByCategory_WhenCategoryExists_ReturnsEvents() {
    // Arrange
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
    when(eventRepository.findByCategory(testCategory)).thenReturn(Arrays.asList(testEvent));

    // Act
    List<EventDTO> result = eventService.getEventsByCategory(1L);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Test Concert", result.get(0).getName());
  }

  @Test
  void getEventsByCategory_WhenCategoryNotFound_ThrowsException() {
    // Arrange
    when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, () -> {
      eventService.getEventsByCategory(999L);
    });

    assertTrue(exception.getMessage().contains("Category not found with id: 999"));
  }

  // ========== GET EVENTS BY STATUS TESTS ==========

  @Test
  void getEventsByStatus_ReturnsFilteredEvents() {
    // Arrange
    when(eventRepository.findByEventStatus(EventStatus.ON_SALE))
        .thenReturn(Arrays.asList(testEvent));

    // Act
    List<EventDTO> result = eventService.getEventsByStatus(EventStatus.ON_SALE);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(EventStatus.ON_SALE, result.get(0).getEventStatus());
  }

  // ========== CREATE EVENT TESTS ==========

  @Test
  void createEvent_WhenValid_CreatesEventAndCaches() {
    // Arrange
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
    when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

    // Act
    EventDTO result = eventService.createEvent(createRequest);

    // Assert
    assertNotNull(result);
    assertEquals("Test Concert", result.getName());
    verify(categoryRepository, times(1)).findById(1L);
    verify(eventRepository, times(1)).save(any(Event.class));
    verify(valueOperations, times(1)).set(eq("event:1"), any(EventDTO.class), eq(1L), eq(TimeUnit.HOURS));
    verify(redisTemplate, times(1)).delete("events:all");
    verify(redisTemplate, times(1)).delete("events:onsale");
  }

  @Test
  void createEvent_WhenCategoryNotFound_ThrowsException() {
    // Arrange
    when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
    createRequest.setCategoryId(999L);

    // Act & Assert
    CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, () -> {
      eventService.createEvent(createRequest);
    });

    assertTrue(exception.getMessage().contains("Category not found with id: 999"));
    verify(eventRepository, never()).save(any(Event.class));
  }

  // ========== UPDATE EVENT TESTS ==========

  @Test
  void updateEvent_WhenValid_UpdatesEventAndCache() {
    // Arrange
    when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
    when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

    // Act
    EventDTO result = eventService.updateEvent(1L, updateRequest);

    // Assert
    assertNotNull(result);
    verify(eventRepository, times(1)).findById(1L);
    verify(eventRepository, times(1)).save(any(Event.class));
    verify(valueOperations, times(1)).set(eq("event:1"), any(EventDTO.class), eq(1L), eq(TimeUnit.HOURS));
    verify(redisTemplate, times(1)).delete("events:all");
    verify(redisTemplate, times(1)).delete("events:onsale");
  }

  @Test
  void updateEvent_WhenEventNotFound_ThrowsException() {
    // Arrange
    when(eventRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    EventNotFoundException exception = assertThrows(EventNotFoundException.class, () -> {
      eventService.updateEvent(999L, updateRequest);
    });

    assertTrue(exception.getMessage().contains("Event not found with id: 999"));
    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  void updateEvent_WhenCategoryIdProvided_UpdatesCategory() {
    // Arrange
    Category newCategory = new Category("Sports", "Sports events");
    newCategory.setId(2L);
    updateRequest.setCategoryId(2L);

    when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
    when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
    when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

    // Act
    EventDTO result = eventService.updateEvent(1L, updateRequest);

    // Assert
    assertNotNull(result);
    verify(categoryRepository, times(1)).findById(2L);
    verify(eventRepository, times(1)).save(any(Event.class));
  }

  @Test
  void updateEvent_WhenNewCategoryNotFound_ThrowsException() {
    // Arrange
    updateRequest.setCategoryId(999L);

    when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
    when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, () -> {
      eventService.updateEvent(1L, updateRequest);
    });

    assertTrue(exception.getMessage().contains("Category not found with id: 999"));
    verify(eventRepository, never()).save(any(Event.class));
  }

  // ========== DELETE EVENT TESTS ==========

  @Test
  void deleteEvent_WhenEventExists_DeletesEventAndEvictsCache() {
    // Arrange
    when(eventRepository.existsById(1L)).thenReturn(true);

    // Act
    eventService.deleteEvent(1L);

    // Assert
    verify(eventRepository, times(1)).existsById(1L);
    verify(eventRepository, times(1)).deleteById(1L);
    verify(redisTemplate, times(1)).delete("event:1");
    verify(redisTemplate, times(1)).delete("events:all");
    verify(redisTemplate, times(1)).delete("events:onsale");
  }

  @Test
  void deleteEvent_WhenEventNotFound_ThrowsException() {
    // Arrange
    when(eventRepository.existsById(999L)).thenReturn(false);

    // Act & Assert
    EventNotFoundException exception = assertThrows(EventNotFoundException.class, () -> {
      eventService.deleteEvent(999L);
    });

    assertTrue(exception.getMessage().contains("Event not found with id: 999"));
    verify(eventRepository, never()).deleteById(anyLong());
  }
}
