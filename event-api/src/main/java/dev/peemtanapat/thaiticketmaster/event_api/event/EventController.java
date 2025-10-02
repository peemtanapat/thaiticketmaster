package dev.peemtanapat.thaiticketmaster.event_api.event;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

  private final EventService eventService;

  public EventController(EventService eventService) {
    this.eventService = eventService;
  }

  /**
   * Get all events
   * User endpoint
   */
  @GetMapping
  public ResponseEntity<List<EventDTO>> getAllEvents() {
    List<EventDTO> events = eventService.getAllEvents();
    return ResponseEntity.ok(events);
  }

  /**
   * Get event by ID
   * User endpoint
   */
  @GetMapping("/{id}")
  public ResponseEntity<EventDTO> getEventById(@PathVariable Long id) {
    EventDTO event = eventService.getEventById(id);
    return ResponseEntity.ok(event);
  }

  /**
   * Get events that are open to buy tickets
   * Ordered by show date (earliest first)
   * User endpoint
   */
  @GetMapping("/on-sale")
  public ResponseEntity<List<EventDTO>> getOnSaleEvents() {
    List<EventDTO> events = eventService.getOnSaleEvents();
    return ResponseEntity.ok(events);
  }

  /**
   * Get events by category
   * User endpoint
   */
  @GetMapping("/category/{categoryId}")
  public ResponseEntity<List<EventDTO>> getEventsByCategory(@PathVariable Long categoryId) {
    List<EventDTO> events = eventService.getEventsByCategory(categoryId);
    return ResponseEntity.ok(events);
  }

  /**
   * Get events by status
   * User endpoint
   */
  @GetMapping("/status/{status}")
  public ResponseEntity<List<EventDTO>> getEventsByStatus(@PathVariable EventStatus status) {
    List<EventDTO> events = eventService.getEventsByStatus(status);
    return ResponseEntity.ok(events);
  }

  /**
   * Create a new event
   * Admin endpoint - TODO: Add admin role security
   */
  @PostMapping
  public ResponseEntity<EventDTO> createEvent(@Valid @RequestBody EventCreateRequest request) {
    EventDTO createdEvent = eventService.createEvent(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
  }

  /**
   * Update an existing event
   * Admin endpoint - TODO: Add admin role security
   */
  @PutMapping("/{id}")
  public ResponseEntity<EventDTO> updateEvent(
      @PathVariable Long id,
      @Valid @RequestBody EventUpdateRequest request) {
    EventDTO updatedEvent = eventService.updateEvent(id, request);
    return ResponseEntity.ok(updatedEvent);
  }

  /**
   * Delete an event
   * Admin endpoint - TODO: Add admin role security
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
    eventService.deleteEvent(id);
    return ResponseEntity.noContent().build();
  }

  // Future feature: Full-text search using Elasticsearch
  // @GetMapping("/search")
  // public ResponseEntity<List<EventDTO>> searchEvents(@RequestParam String
  // query) {
  // List<EventDTO> events = eventService.searchEventsByName(query);
  // return ResponseEntity.ok(events);
  // }
}
