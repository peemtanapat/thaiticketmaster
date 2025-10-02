package dev.peemtanapat.thaiticketmaster.event_api.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

  private final EventRepository eventRepository;
  private final CategoryRepository categoryRepository;

  public EventService(EventRepository eventRepository, CategoryRepository categoryRepository) {
    this.eventRepository = eventRepository;
    this.categoryRepository = categoryRepository;
  }

  /**
   * Get all events
   */
  @Transactional(readOnly = true)
  public List<EventDTO> getAllEvents() {
    return eventRepository.findAll().stream()
        .map(EventDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * Get event by ID
   */
  @Transactional(readOnly = true)
  public EventDTO getEventById(Long id) {
    Event event = eventRepository.findById(id)
        .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
    return new EventDTO(event);
  }

  /**
   * Get events that are open to buy (ON_SALE and on-sale datetime has passed)
   * Ordered by show date (earliest first)
   */
  @Transactional(readOnly = true)
  public List<EventDTO> getOnSaleEvents() {
    LocalDateTime now = LocalDateTime.now();
    return eventRepository.findOnSaleEventsOrderByShowDate(now).stream()
        .map(EventDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * Get events by category
   */
  @Transactional(readOnly = true)
  public List<EventDTO> getEventsByCategory(Long categoryId) {
    Category category = categoryRepository.findById(categoryId)
        .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + categoryId));
    return eventRepository.findByCategory(category).stream()
        .map(EventDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * Get events by status
   */
  @Transactional(readOnly = true)
  public List<EventDTO> getEventsByStatus(EventStatus status) {
    return eventRepository.findByEventStatus(status).stream()
        .map(EventDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * Create a new event (Admin only)
   */
  @Transactional
  public EventDTO createEvent(EventCreateRequest request) {
    // Validate category exists
    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + request.getCategoryId()));

    // Create event entity
    Event event = new Event();
    event.setName(request.getName());
    event.setCategory(category);
    event.setShowDateTimes(request.getShowDateTimes());
    event.setLocation(request.getLocation());
    event.setOnSaleDateTime(request.getOnSaleDateTime());
    event.setTicketPrice(request.getTicketPrice());
    event.setDetail(request.getDetail());
    event.setCondition(request.getCondition());
    event.setEventStatus(request.getEventStatus());
    event.setGateOpen(request.getGateOpen());

    Event savedEvent = eventRepository.save(event);
    return new EventDTO(savedEvent);
  }

  /**
   * Update an existing event (Admin only)
   */
  @Transactional
  public EventDTO updateEvent(Long id, EventUpdateRequest request) {
    Event event = eventRepository.findById(id)
        .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));

    // Update only non-null fields
    if (request.getName() != null) {
      event.setName(request.getName());
    }
    if (request.getCategoryId() != null) {
      Category category = categoryRepository.findById(request.getCategoryId())
          .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + request.getCategoryId()));
      event.setCategory(category);
    }
    if (request.getShowDateTimes() != null) {
      event.setShowDateTimes(request.getShowDateTimes());
    }
    if (request.getLocation() != null) {
      event.setLocation(request.getLocation());
    }
    if (request.getOnSaleDateTime() != null) {
      event.setOnSaleDateTime(request.getOnSaleDateTime());
    }
    if (request.getTicketPrice() != null) {
      event.setTicketPrice(request.getTicketPrice());
    }
    if (request.getDetail() != null) {
      event.setDetail(request.getDetail());
    }
    if (request.getCondition() != null) {
      event.setCondition(request.getCondition());
    }
    if (request.getEventStatus() != null) {
      event.setEventStatus(request.getEventStatus());
    }
    if (request.getGateOpen() != null) {
      event.setGateOpen(request.getGateOpen());
    }

    Event updatedEvent = eventRepository.save(event);
    return new EventDTO(updatedEvent);
  }

  /**
   * Delete an event (Admin only)
   */
  @Transactional
  public void deleteEvent(Long id) {
    if (!eventRepository.existsById(id)) {
      throw new EventNotFoundException("Event not found with id: " + id);
    }
    eventRepository.deleteById(id);
  }

  // Future feature: Full-text search using Elasticsearch
  // public List<EventDTO> searchEventsByName(String searchTerm) {
  // return eventRepository.searchEventsByName(searchTerm).stream()
  // .map(EventDTO::new)
  // .collect(Collectors.toList());
  // }
}
