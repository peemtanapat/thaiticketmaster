package dev.peemtanapat.thaiticketmaster.event_api.event;

public class EventNotFoundException extends RuntimeException {
  public EventNotFoundException(String message) {
    super(message);
  }
}
