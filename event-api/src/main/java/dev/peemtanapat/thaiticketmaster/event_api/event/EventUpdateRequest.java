package dev.peemtanapat.thaiticketmaster.event_api.event;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public class EventUpdateRequest {

  @Size(max = 200, message = "Event name must not exceed 200 characters")
  private String name;

  private Long categoryId;

  private List<OffsetDateTime> showDateTimes;

  @Size(max = 500, message = "Location must not exceed 500 characters")
  private String location;

  private LocalDateTime onSaleDateTime;

  @DecimalMin(value = "0.0", inclusive = false, message = "Ticket price must be greater than 0")
  private BigDecimal ticketPrice;

  private String detail;

  private String condition;

  private EventStatus eventStatus;

  @Size(max = 100, message = "Gate open information must not exceed 100 characters")
  private String gateOpen;

  // Constructors
  public EventUpdateRequest() {
  }

  // Getters and Setters
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  public List<OffsetDateTime> getShowDateTimes() {
    return showDateTimes;
  }

  public void setShowDateTimes(List<OffsetDateTime> showDateTimes) {
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
}
