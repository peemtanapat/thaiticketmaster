package dev.peemtanapat.thaiticketmaster.event_api.event;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_event_status", columnList = "event_status"),
    @Index(name = "idx_on_sale_datetime", columnList = "on_sale_datetime")
})
public class Event implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  // Multiple show times in the same day can be stored as a list
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "event_show_times", joinColumns = @JoinColumn(name = "event_id"))
  @Column(name = "show_datetime", nullable = false, columnDefinition = "TIMESTAMPTZ")
  private List<OffsetDateTime> showDateTimes = new ArrayList<>();

  @Column(nullable = false, length = 500)
  private String location;

  @Column(name = "on_sale_datetime", nullable = false)
  private LocalDateTime onSaleDateTime;

  @Column(name = "ticket_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal ticketPrice;

  @Column(columnDefinition = "TEXT")
  private String detail;

  @Column(columnDefinition = "TEXT")
  private String condition;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_status", nullable = false, length = 20)
  private EventStatus eventStatus;

  @Column(name = "gate_open", length = 100)
  private String gateOpen; // e.g., "1 hour before show start"

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Constructors
  public Event() {
  }

  public Event(String name, Category category, List<OffsetDateTime> showDateTimes,
      String location, LocalDateTime onSaleDateTime, BigDecimal ticketPrice,
      String detail, String condition, EventStatus eventStatus, String gateOpen) {
    this.name = name;
    this.category = category;
    this.showDateTimes = showDateTimes;
    this.location = location;
    this.onSaleDateTime = onSaleDateTime;
    this.ticketPrice = ticketPrice;
    this.detail = detail;
    this.condition = condition;
    this.eventStatus = eventStatus;
    this.gateOpen = gateOpen;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public List<OffsetDateTime> getShowDateTimes() {
    return showDateTimes;
  }

  public void setShowDateTimes(List<OffsetDateTime> showDateTimes) {
    // Create a new ArrayList to avoid Hibernate's persistent collection issues
    this.showDateTimes = new ArrayList<>();

    if (showDateTimes != null) {
      this.showDateTimes.addAll(showDateTimes);
    }
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public LocalDateTime getOnSaleDateTime() {
    return onSaleDateTime;
  }

  public void setOnSaleDateTime(LocalDateTime onSaleDateTime) {
    this.onSaleDateTime = onSaleDateTime;
  }

  public BigDecimal getTicketPrice() {
    return ticketPrice;
  }

  public void setTicketPrice(BigDecimal ticketPrice) {
    this.ticketPrice = ticketPrice;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public EventStatus getEventStatus() {
    return eventStatus;
  }

  public void setEventStatus(EventStatus eventStatus) {
    this.eventStatus = eventStatus;
  }

  public String getGateOpen() {
    return gateOpen;
  }

  public void setGateOpen(String gateOpen) {
    this.gateOpen = gateOpen;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
