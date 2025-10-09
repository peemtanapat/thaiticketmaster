package dev.peemtanapat.thaiticketmaster.event_api.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

  @Test
  void constructor_WithAllParameters_CreatesEvent() {
    // Arrange
    Category category = new Category("Concert", "Music concerts");
    category.setId(1L);
    List<OffsetDateTime> showTimes = Arrays.asList(OffsetDateTime.now().plusDays(30));
    LocalDateTime onSaleDate = LocalDateTime.now().plusDays(1);
    BigDecimal price = new BigDecimal("1500.00");

    // Act
    Event event = new Event(
        "Test Concert",
        category,
        showTimes,
        "Test Venue",
        onSaleDate,
        price,
        "Test details",
        "Test conditions",
        EventStatus.ON_SALE,
        "1 hour before");

    // Assert
    assertNull(event.getId()); // ID not set until persisted
    assertEquals("Test Concert", event.getName());
    assertEquals(category, event.getCategory());
    assertEquals(showTimes, event.getShowDateTimes());
    assertEquals("Test Venue", event.getLocation());
    assertEquals(onSaleDate, event.getOnSaleDateTime());
    assertEquals(price, event.getTicketPrice());
    assertEquals("Test details", event.getDetail());
    assertEquals("Test conditions", event.getCondition());
    assertEquals(EventStatus.ON_SALE, event.getEventStatus());
    assertEquals("1 hour before", event.getGateOpen());
  }

  @Test
  void defaultConstructor_CreatesEmptyEvent() {
    // Act
    Event event = new Event();

    // Assert
    assertNull(event.getId());
    assertNull(event.getName());
    assertNull(event.getCategory());
    assertNotNull(event.getShowDateTimes());
    assertTrue(event.getShowDateTimes().isEmpty());
  }

  @Test
  void settersAndGetters_WorkCorrectly() {
    // Arrange
    Event event = new Event();
    Category category = new Category("Sports", "Sports events");
    category.setId(2L);
    List<OffsetDateTime> showTimes = Arrays.asList(OffsetDateTime.now().plusDays(15));
    LocalDateTime onSaleDate = LocalDateTime.now();
    BigDecimal price = new BigDecimal("2000.00");

    // Act
    event.setId(1L);
    event.setName("Test Event");
    event.setCategory(category);
    event.setShowDateTimes(showTimes);
    event.setLocation("Test Location");
    event.setOnSaleDateTime(onSaleDate);
    event.setTicketPrice(price);
    event.setDetail("Details");
    event.setCondition("Conditions");
    event.setEventStatus(EventStatus.SOLD_OUT);
    event.setGateOpen("30 minutes before");

    // Assert
    assertEquals(1L, event.getId());
    assertEquals("Test Event", event.getName());
    assertEquals(category, event.getCategory());
    assertEquals(showTimes, event.getShowDateTimes());
    assertEquals("Test Location", event.getLocation());
    assertEquals(onSaleDate, event.getOnSaleDateTime());
    assertEquals(price, event.getTicketPrice());
    assertEquals("Details", event.getDetail());
    assertEquals("Conditions", event.getCondition());
    assertEquals(EventStatus.SOLD_OUT, event.getEventStatus());
    assertEquals("30 minutes before", event.getGateOpen());
  }

  @Test
  void onCreate_SetsCreatedAtAndUpdatedAt() {
    // Arrange
    Event event = new Event();
    LocalDateTime before = LocalDateTime.now().minusSeconds(1);

    // Act
    event.onCreate();
    LocalDateTime after = LocalDateTime.now().plusSeconds(1);

    // Assert
    assertNotNull(event.getCreatedAt());
    assertNotNull(event.getUpdatedAt());
    assertTrue(event.getCreatedAt().isAfter(before));
    assertTrue(event.getCreatedAt().isBefore(after));
    // Check timestamps are within 1 second of each other (to handle microsecond
    // differences)
    assertTrue(Math.abs(java.time.Duration.between(event.getCreatedAt(), event.getUpdatedAt()).toMillis()) < 1000);
  }

  @Test
  void onUpdate_UpdatesUpdatedAtOnly() throws InterruptedException {
    // Arrange
    Event event = new Event();
    event.onCreate();
    LocalDateTime originalCreatedAt = event.getCreatedAt();
    LocalDateTime originalUpdatedAt = event.getUpdatedAt();

    // Sleep to ensure time difference
    Thread.sleep(10);

    // Act
    event.onUpdate();

    // Assert
    assertEquals(originalCreatedAt, event.getCreatedAt()); // createdAt unchanged
    assertNotEquals(originalUpdatedAt, event.getUpdatedAt()); // updatedAt changed
    assertTrue(event.getUpdatedAt().isAfter(originalUpdatedAt));
  }

  @Test
  void multipleShowTimes_CanBeAdded() {
    // Arrange
    Event event = new Event();
    List<OffsetDateTime> showTimes = Arrays.asList(
        OffsetDateTime.now().plusDays(1),
        OffsetDateTime.now().plusDays(2),
        OffsetDateTime.now().plusDays(3));

    // Act
    event.setShowDateTimes(showTimes);

    // Assert
    assertEquals(3, event.getShowDateTimes().size());
    assertEquals(showTimes, event.getShowDateTimes());
  }
}
