package dev.peemtanapat.thaiticketmaster.event_api.event;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class EventService {

  private final EventRepository eventRepository;
  private final CategoryRepository categoryRepository;
  private final RedisTemplate<String, Object> redisTemplate;

  private static final String EVENT_CACHE_PREFIX = "event:";
  private static final String EVENTS_ALL_CACHE_KEY = "events:all";
  private static final String EVENTS_ON_SALE_CACHE_KEY = "events:onsale";
  private static final long CACHE_TTL_HOURS = 1;

  public EventService(EventRepository eventRepository, CategoryRepository categoryRepository,
      RedisTemplate<String, Object> redisTemplate) {
    this.eventRepository = eventRepository;
    this.categoryRepository = categoryRepository;
    this.redisTemplate = redisTemplate;
  }

  /**
   * Get all events
   * Uses write-through cache: Check cache first, if miss, load from DB and cache
   */
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<EventDTO> getAllEvents() {
    // Try to get from cache first
    List<EventDTO> cachedEvents = (List<EventDTO>) redisTemplate.opsForValue().get(EVENTS_ALL_CACHE_KEY);
    if (cachedEvents != null) {
      return cachedEvents;
    }

    // If not in cache, get from database
    List<EventDTO> events = eventRepository.findAll().stream()
        .map(EventDTO::new)
        .collect(Collectors.toList());

    // Cache the result
    redisTemplate.opsForValue().set(EVENTS_ALL_CACHE_KEY, events, CACHE_TTL_HOURS, TimeUnit.HOURS);

    return events;
  }

  /**
   * Get event by ID
   * Uses write-through cache: Check cache first, if miss, load from DB and cache
   */
  @Transactional(readOnly = true)
  public EventDTO getEventById(Long id) {
    String cacheKey = EVENT_CACHE_PREFIX + id;

    // Try to get from cache first
    EventDTO cachedEvent = (EventDTO) redisTemplate.opsForValue().get(cacheKey);
    if (cachedEvent != null) {
      return cachedEvent;
    }

    // If not in cache, get from database
    Event event = eventRepository.findById(id)
        .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
    EventDTO eventDTO = new EventDTO(event);

    // Cache the result
    redisTemplate.opsForValue().set(cacheKey, eventDTO, CACHE_TTL_HOURS, TimeUnit.HOURS);

    return eventDTO;
  }

  /**
   * Get events that are open to buy (ON_SALE and on-sale datetime has passed)
   * Ordered by show date (earliest first)
   * Uses write-through cache: Check cache first, if miss, load from DB and cache
   */
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<EventDTO> getOnSaleEvents() {
    // Try to get from cache first
    List<EventDTO> cachedEvents = (List<EventDTO>) redisTemplate.opsForValue().get(EVENTS_ON_SALE_CACHE_KEY);
    if (cachedEvents != null) {
      return cachedEvents;
    }

    // If not in cache, get from database
    LocalDateTime now = LocalDateTime.now();
    List<EventDTO> events = eventRepository.findOnSaleEventsOrderByShowDate(now).stream()
        .map(EventDTO::new)
        .collect(Collectors.toList());

    // Cache the result with shorter TTL since it's time-sensitive
    redisTemplate.opsForValue().set(EVENTS_ON_SALE_CACHE_KEY, events, 15, TimeUnit.MINUTES);

    return events;
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
   * Write-through strategy: Write to DB first, then cache the result and
   * invalidate list caches
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

    // Write to database first
    Event savedEvent = eventRepository.save(event);
    EventDTO eventDTO = new EventDTO(savedEvent);

    // Cache the individual event
    String cacheKey = EVENT_CACHE_PREFIX + savedEvent.getId();
    redisTemplate.opsForValue().set(cacheKey, eventDTO, CACHE_TTL_HOURS, TimeUnit.HOURS);

    // Invalidate list caches since a new event was added
    redisTemplate.delete(EVENTS_ALL_CACHE_KEY);
    redisTemplate.delete(EVENTS_ON_SALE_CACHE_KEY);

    return eventDTO;
  }

  /**
   * Update an existing event (Admin only)
   * Write-through strategy: Update DB first, then update cache and invalidate
   * list caches
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

    // Update database first
    Event updatedEvent = eventRepository.save(event);
    EventDTO eventDTO = new EventDTO(updatedEvent);

    // Update cache with new data
    String cacheKey = EVENT_CACHE_PREFIX + id;
    redisTemplate.opsForValue().set(cacheKey, eventDTO, CACHE_TTL_HOURS, TimeUnit.HOURS);

    // Invalidate list caches since event data changed
    redisTemplate.delete(EVENTS_ALL_CACHE_KEY);
    redisTemplate.delete(EVENTS_ON_SALE_CACHE_KEY);

    return eventDTO;
  }

  /**
   * Delete an event (Admin only)
   * Write-through strategy: Delete from DB first, then evict from cache
   */
  @Transactional
  public void deleteEvent(Long id) {
    if (!eventRepository.existsById(id)) {
      throw new EventNotFoundException("Event not found with id: " + id);
    }

    // Delete from database first
    eventRepository.deleteById(id);

    // Evict from cache
    String cacheKey = EVENT_CACHE_PREFIX + id;
    redisTemplate.delete(cacheKey);

    // Invalidate list caches since an event was removed
    redisTemplate.delete(EVENTS_ALL_CACHE_KEY);
    redisTemplate.delete(EVENTS_ON_SALE_CACHE_KEY);
  }

  // Future feature: Full-text search using Elasticsearch
  // public List<EventDTO> searchEventsByName(String searchTerm) {
  // return eventRepository.searchEventsByName(searchTerm).stream()
  // .map(EventDTO::new)
  // .collect(Collectors.toList());
  // }
}
