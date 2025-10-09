package dev.peemtanapat.thaiticketmaster.event_api.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventDTOTest {

  private Category testCategory;
  private Event testEvent;

  @BeforeEach
  void setUp() {
    testCategory = new Category("Concert", "Music concerts");
    testCategory.setId(1L);

    testEvent = new Event(
        "Test Concert",
        testCategory,
        Arrays.asList(OffsetDateTime.now().plusDays(30)),
        "Test Venue",
        LocalDateTime.now().plusDays(1),
        new BigDecimal("1500.00"),
        "Test details",
        "Test conditions",
        EventStatus.ON_SALE,
        "1 hour before");
    testEvent.setId(1L);
    testEvent.onCreate();
  }

  @Test
  void constructor_WithEvent_CreatesEventDTO() {
    // Act
    EventDTO dto = new EventDTO(testEvent);

    // Assert
    assertEquals(testEvent.getId(), dto.getId());
    assertEquals(testEvent.getName(), dto.getName());
    assertEquals(testEvent.getCategory().getId(), dto.getCategory().getId());
    assertEquals(testEvent.getCategory().getName(), dto.getCategory().getName());
    assertEquals(testEvent.getShowDateTimes().size(), dto.getShowDateTimes().size());
    assertEquals(testEvent.getLocation(), dto.getLocation());
    assertEquals(testEvent.getOnSaleDateTime(), dto.getOnSaleDateTime());
    assertEquals(testEvent.getTicketPrice(), dto.getTicketPrice());
    assertEquals(testEvent.getDetail(), dto.getDetail());
    assertEquals(testEvent.getCondition(), dto.getCondition());
    assertEquals(testEvent.getEventStatus(), dto.getEventStatus());
    assertEquals(testEvent.getGateOpen(), dto.getGateOpen());
    assertEquals(testEvent.getCreatedAt(), dto.getCreatedAt());
    assertEquals(testEvent.getUpdatedAt(), dto.getUpdatedAt());
  }

  @Test
  void constructor_WithNullShowDateTimes_CreatesEmptyList() {
    // Arrange
    testEvent.setShowDateTimes(null);

    // Act
    EventDTO dto = new EventDTO(testEvent);

    // Assert
    assertNotNull(dto.getShowDateTimes());
    assertTrue(dto.getShowDateTimes().isEmpty());
  }

  @Test
  void constructor_CopiesShowDateTimes_NotSameReference() {
    // Act
    EventDTO dto = new EventDTO(testEvent);

    // Assert
    assertNotSame(testEvent.getShowDateTimes(), dto.getShowDateTimes());
    assertEquals(testEvent.getShowDateTimes(), dto.getShowDateTimes());
  }

  @Test
  void defaultConstructor_CreatesEmptyDTO() {
    // Act
    EventDTO dto = new EventDTO();

    // Assert
    assertNull(dto.getId());
    assertNull(dto.getName());
    assertNull(dto.getCategory());
  }

  @Test
  void settersAndGetters_WorkCorrectly() {
    // Arrange
    EventDTO dto = new EventDTO();
    EventDTO.CategoryDTO categoryDTO = new EventDTO.CategoryDTO();
    categoryDTO.setId(2L);
    categoryDTO.setName("Sports");
    List<OffsetDateTime> showTimes = Arrays.asList(OffsetDateTime.now().plusDays(10));
    LocalDateTime now = LocalDateTime.now();

    // Act
    dto.setId(5L);
    dto.setName("Test Event");
    dto.setCategory(categoryDTO);
    dto.setShowDateTimes(showTimes);
    dto.setLocation("Location");
    dto.setOnSaleDateTime(now);
    dto.setTicketPrice(new BigDecimal("1000.00"));
    dto.setDetail("Details");
    dto.setCondition("Conditions");
    dto.setEventStatus(EventStatus.CANCELLED);
    dto.setGateOpen("Gates open at 6 PM");
    dto.setCreatedAt(now);
    dto.setUpdatedAt(now);

    // Assert
    assertEquals(5L, dto.getId());
    assertEquals("Test Event", dto.getName());
    assertEquals(categoryDTO, dto.getCategory());
    assertEquals(showTimes, dto.getShowDateTimes());
    assertEquals("Location", dto.getLocation());
    assertEquals(now, dto.getOnSaleDateTime());
    assertEquals(new BigDecimal("1000.00"), dto.getTicketPrice());
    assertEquals("Details", dto.getDetail());
    assertEquals("Conditions", dto.getCondition());
    assertEquals(EventStatus.CANCELLED, dto.getEventStatus());
    assertEquals("Gates open at 6 PM", dto.getGateOpen());
    assertEquals(now, dto.getCreatedAt());
    assertEquals(now, dto.getUpdatedAt());
  }
}
