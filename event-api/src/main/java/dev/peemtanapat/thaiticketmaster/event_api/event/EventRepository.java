package dev.peemtanapat.thaiticketmaster.event_api.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

  /**
   * Find all events that are open to buy (ON_SALE status and on-sale datetime has
   * passed)
   * Ordered by the earliest show datetime
   */
  @Query("SELECT DISTINCT e FROM Event e " +
      "LEFT JOIN FETCH e.category " +
      "LEFT JOIN e.showDateTimes st " +
      "WHERE e.eventStatus = 'ON_SALE' " +
      "AND e.onSaleDateTime <= :currentDateTime " +
      "ORDER BY st ASC")
  List<Event> findOnSaleEventsOrderByShowDate(@Param("currentDateTime") LocalDateTime currentDateTime);

  /**
   * Find events by category
   */
  List<Event> findByCategory(Category category);

  /**
   * Find events by status
   */
  List<Event> findByEventStatus(EventStatus eventStatus);

  /**
   * Find events by category and status
   */
  List<Event> findByCategoryAndEventStatus(Category category, EventStatus eventStatus);

  // Future feature: Full-text search using Elasticsearch
  // @Query(value = "SELECT * FROM events WHERE MATCH(name) AGAINST(:searchTerm IN
  // NATURAL LANGUAGE MODE)", nativeQuery = true)
  // List<Event> searchEventsByName(@Param("searchTerm") String searchTerm);
}
