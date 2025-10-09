package dev.peemtanapat.thaiticketmaster.event_api.event;

import dev.peemtanapat.thaiticketmaster.event_api.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for EventService with real database and Redis.
 * Tests the complete flow including persistence, caching, and business logic.
 */
class EventServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private EventService eventService;

  private Category testCategory;
  private OffsetDateTime futureDate;
  private LocalDateTime pastDate;

  @BeforeEach
  void setUp() {
    // Create test category
    testCategory = new Category("Concert", "Music concerts and shows");
    testCategory = categoryRepository.save(testCategory);

    futureDate = OffsetDateTime.now().plusDays(30);
    pastDate = LocalDateTime.now().minusDays(1);
  }

  // ========== CREATE EVENT TESTS ==========

  @Test
  void createEvent_WithValidData_SavesAndReturnsEvent() {
    // Arrange
    EventCreateRequest request = new EventCreateRequest();
    request.setName("Rock Concert");
    request.setCategoryId(testCategory.getId());
    request.setShowDateTimes(Arrays.asList(futureDate));
    request.setLocation("Stadium Arena");
    request.setOnSaleDateTime(LocalDateTime.now().plusHours(1));
    request.setTicketPrice(new BigDecimal("1500.00"));
    request.setDetail("Amazing rock concert");
    request.setCondition("No outside food");
    request.setEventStatus(EventStatus.COMING_SOON);
    request.setGateOpen("1 hour before");

    // Act
    EventDTO result = eventService.createEvent(request);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getId());
    assertEquals("Rock Concert", result.getName());
    assertEquals(testCategory.getId(), result.getCategory().getId());
    assertEquals(EventStatus.COMING_SOON, result.getEventStatus());

    // Verify it's in the database
    Event saved = eventRepository.findById(result.getId()).orElse(null);
    assertNotNull(saved);
    assertEquals("Rock Concert", saved.getName());
  }

  @Test
  void createEvent_InvalidCategory_ThrowsException() {
    // Arrange
    EventCreateRequest request = new EventCreateRequest();
    request.setName("Test Event");
    request.setCategoryId(999L); // Non-existent category
    request.setShowDateTimes(Arrays.asList(futureDate));
    request.setLocation("Test Location");
    request.setOnSaleDateTime(LocalDateTime.now().plusHours(1));
    request.setTicketPrice(new BigDecimal("1000.00"));
    request.setEventStatus(EventStatus.ON_SALE);

    // Act & Assert
    assertThrows(CategoryNotFoundException.class, () -> {
      eventService.createEvent(request);
    });
  }

  // ========== GET EVENT TESTS ==========

  @Test
  void getEventById_ExistingEvent_ReturnsEvent() {
    // Arrange
    Event event = createTestEvent("Test Event", EventStatus.ON_SALE);
    Event saved = eventRepository.save(event);

    // Act - First call (cache miss)
    EventDTO result1 = eventService.getEventById(saved.getId());
    // Second call (cache hit)
    EventDTO result2 = eventService.getEventById(saved.getId());

    // Assert
    assertNotNull(result1);
    assertEquals(saved.getId(), result1.getId());
    assertEquals("Test Event", result1.getName());

    // Both calls should return the same data
    assertEquals(result1.getId(), result2.getId());
    assertEquals(result1.getName(), result2.getName());
  }

  @Test
  void getEventById_NonExistentEvent_ThrowsException() {
    // Act & Assert
    assertThrows(EventNotFoundException.class, () -> {
      eventService.getEventById(999L);
    });
  }

  @Test
  void getAllEvents_ReturnsAllEvents() {
    // Arrange
    Event event1 = createTestEvent("Event 1", EventStatus.ON_SALE);
    Event event2 = createTestEvent("Event 2", EventStatus.SOLD_OUT);
    eventRepository.save(event1);
    eventRepository.save(event2);

    // Act
    List<EventDTO> result = eventService.getAllEvents();

    // Assert
    assertNotNull(result);
    assertTrue(result.size() >= 2);
    assertTrue(result.stream().anyMatch(e -> e.getName().equals("Event 1")));
    assertTrue(result.stream().anyMatch(e -> e.getName().equals("Event 2")));
  }

  @Test
  void getOnSaleEvents_ReturnsOnlyOnSaleEvents() {
    // Arrange
    Event onSaleEvent = createTestEvent("On Sale Event", EventStatus.ON_SALE);
    onSaleEvent.setOnSaleDateTime(pastDate); // Already on sale
    Event soldOutEvent = createTestEvent("Sold Out Event", EventStatus.SOLD_OUT);
    soldOutEvent.setOnSaleDateTime(pastDate);
    Event comingSoonEvent = createTestEvent("Coming Soon Event", EventStatus.COMING_SOON);

    eventRepository.save(onSaleEvent);
    eventRepository.save(soldOutEvent);
    eventRepository.save(comingSoonEvent);

    // Act
    List<EventDTO> result = eventService.getOnSaleEvents();

    // Assert
    assertNotNull(result);
    assertTrue(result.stream().anyMatch(e -> e.getName().equals("On Sale Event")));
    assertFalse(result.stream().anyMatch(e -> e.getName().equals("Sold Out Event")));
    assertFalse(result.stream().anyMatch(e -> e.getName().equals("Coming Soon Event")));
  }

  @Test
  void getEventsByCategory_ReturnsEventsInCategory() {
    // Arrange
    Category sportCategory = new Category("Sports", "Sports events");
    sportCategory = categoryRepository.save(sportCategory);

    Event concertEvent = createTestEvent("Concert Event", EventStatus.ON_SALE);
    Event sportEvent = createTestEvent("Sport Event", EventStatus.ON_SALE);
    sportEvent.setCategory(sportCategory);

    eventRepository.save(concertEvent);
    eventRepository.save(sportEvent);

    // Act
    List<EventDTO> concertEvents = eventService.getEventsByCategory(testCategory.getId());
    List<EventDTO> sportEvents = eventService.getEventsByCategory(sportCategory.getId());

    // Assert
    assertTrue(concertEvents.stream().anyMatch(e -> e.getName().equals("Concert Event")));
    assertFalse(concertEvents.stream().anyMatch(e -> e.getName().equals("Sport Event")));

    assertTrue(sportEvents.stream().anyMatch(e -> e.getName().equals("Sport Event")));
    assertFalse(sportEvents.stream().anyMatch(e -> e.getName().equals("Concert Event")));
  }

  @Test
  void getEventsByStatus_ReturnsEventsWithStatus() {
    // Arrange
    Event onSaleEvent = createTestEvent("On Sale", EventStatus.ON_SALE);
    Event soldOutEvent = createTestEvent("Sold Out", EventStatus.SOLD_OUT);
    eventRepository.save(onSaleEvent);
    eventRepository.save(soldOutEvent);

    // Act
    List<EventDTO> onSaleEvents = eventService.getEventsByStatus(EventStatus.ON_SALE);
    List<EventDTO> soldOutEvents = eventService.getEventsByStatus(EventStatus.SOLD_OUT);

    // Assert
    assertTrue(onSaleEvents.stream().anyMatch(e -> e.getName().equals("On Sale")));
    assertFalse(onSaleEvents.stream().anyMatch(e -> e.getName().equals("Sold Out")));

    assertTrue(soldOutEvents.stream().anyMatch(e -> e.getName().equals("Sold Out")));
    assertFalse(soldOutEvents.stream().anyMatch(e -> e.getName().equals("On Sale")));
  }

  // ========== UPDATE EVENT TESTS ==========

  @Test
  void updateEvent_ValidChanges_UpdatesAndInvalidatesCache() {
    // Arrange
    Event event = createTestEvent("Original Name", EventStatus.ON_SALE);
    Event saved = eventRepository.save(event);

    // Cache the event first
    eventService.getEventById(saved.getId());

    EventUpdateRequest request = new EventUpdateRequest();
    request.setName("Updated Name");
    request.setTicketPrice(new BigDecimal("2000.00"));
    request.setEventStatus(EventStatus.SOLD_OUT);

    // Act
    EventDTO updated = eventService.updateEvent(saved.getId(), request);

    // Assert
    assertEquals("Updated Name", updated.getName());
    assertEquals(new BigDecimal("2000.00"), updated.getTicketPrice());
    assertEquals(EventStatus.SOLD_OUT, updated.getEventStatus());

    // Verify in database
    Event fromDb = eventRepository.findById(saved.getId()).orElseThrow();
    assertEquals("Updated Name", fromDb.getName());
    assertEquals(EventStatus.SOLD_OUT, fromDb.getEventStatus());
  }

  @Test
  void updateEvent_ChangeCategory_UpdatesCategory() {
    // Arrange
    Category newCategory = new Category("Theater", "Theater shows");
    newCategory = categoryRepository.save(newCategory);

    Event event = createTestEvent("Test Event", EventStatus.ON_SALE);
    Event saved = eventRepository.save(event);

    EventUpdateRequest request = new EventUpdateRequest();
    request.setCategoryId(newCategory.getId());

    // Act
    EventDTO updated = eventService.updateEvent(saved.getId(), request);

    // Assert
    assertEquals(newCategory.getId(), updated.getCategory().getId());
    assertEquals("Theater", updated.getCategory().getName());
  }

  @Test
  void updateEvent_NonExistentEvent_ThrowsException() {
    // Arrange
    EventUpdateRequest request = new EventUpdateRequest();
    request.setName("New Name");

    // Act & Assert
    assertThrows(EventNotFoundException.class, () -> {
      eventService.updateEvent(999L, request);
    });
  }

  // ========== DELETE EVENT TESTS ==========

  @Test
  void deleteEvent_ExistingEvent_DeletesFromDatabaseAndCache() {
    // Arrange
    Event event = createTestEvent("To Delete", EventStatus.ON_SALE);
    Event saved = eventRepository.save(event);
    Long eventId = saved.getId();

    // Cache the event first
    eventService.getEventById(eventId);

    // Act
    eventService.deleteEvent(eventId);

    // Assert
    assertFalse(eventRepository.existsById(eventId));

    // Should throw exception when trying to get deleted event
    assertThrows(EventNotFoundException.class, () -> {
      eventService.getEventById(eventId);
    });
  }

  @Test
  void deleteEvent_NonExistentEvent_ThrowsException() {
    // Act & Assert
    assertThrows(EventNotFoundException.class, () -> {
      eventService.deleteEvent(999L);
    });
  }

  // ========== CACHE BEHAVIOR TESTS ==========

  @Test
  void createEvent_InvalidatesAllEventsCache() {
    // Arrange
    Event event1 = createTestEvent("Event 1", EventStatus.ON_SALE);
    eventRepository.save(event1);

    // Cache all events
    List<EventDTO> beforeCreate = eventService.getAllEvents();
    int countBefore = beforeCreate.size();

    // Act - Create new event
    EventCreateRequest request = new EventCreateRequest();
    request.setName("New Event");
    request.setCategoryId(testCategory.getId());
    request.setShowDateTimes(Arrays.asList(futureDate));
    request.setLocation("New Location");
    request.setOnSaleDateTime(LocalDateTime.now().plusHours(1));
    request.setTicketPrice(new BigDecimal("1000.00"));
    request.setEventStatus(EventStatus.ON_SALE);

    eventService.createEvent(request);

    // Assert - Cache should be invalidated, new list should include new event
    List<EventDTO> afterCreate = eventService.getAllEvents();
    assertTrue(afterCreate.size() > countBefore);
    // TODO: add verifying call time of get-cache and get-db
  }

  // ========== HELPER METHODS ==========

  private Event createTestEvent(String name, EventStatus status) {
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
    return event;
  }
}
