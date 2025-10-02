package dev.peemtanapat.thaiticketmaster.event_api.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class EventDTO {
  private Long id;
  private String name;
  private CategoryDTO category;
  private List<LocalDateTime> showDateTimes;
  private String location;
  private LocalDateTime onSaleDateTime;
  private BigDecimal ticketPrice;
  private String detail;
  private String condition;
  private EventStatus eventStatus;
  private String gateOpen;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Constructors
  public EventDTO() {
  }

  public EventDTO(Event event) {
    this.id = event.getId();
    this.name = event.getName();
    this.category = new CategoryDTO(event.getCategory());
    this.showDateTimes = event.getShowDateTimes();
    this.location = event.getLocation();
    this.onSaleDateTime = event.getOnSaleDateTime();
    this.ticketPrice = event.getTicketPrice();
    this.detail = event.getDetail();
    this.condition = event.getCondition();
    this.eventStatus = event.getEventStatus();
    this.gateOpen = event.getGateOpen();
    this.createdAt = event.getCreatedAt();
    this.updatedAt = event.getUpdatedAt();
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

  public CategoryDTO getCategory() {
    return category;
  }

  public void setCategory(CategoryDTO category) {
    this.category = category;
  }

  public List<LocalDateTime> getShowDateTimes() {
    return showDateTimes;
  }

  public void setShowDateTimes(List<LocalDateTime> showDateTimes) {
    this.showDateTimes = showDateTimes;
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

  // Inner DTO for Category
  public static class CategoryDTO {
    private Long id;
    private String name;
    private String description;

    public CategoryDTO() {
    }

    public CategoryDTO(Category category) {
      this.id = category.getId();
      this.name = category.getName();
      this.description = category.getDescription();
    }

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

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }
}
