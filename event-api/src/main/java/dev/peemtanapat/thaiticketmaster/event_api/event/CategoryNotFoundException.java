package dev.peemtanapat.thaiticketmaster.event_api.event;

public class CategoryNotFoundException extends RuntimeException {
  public CategoryNotFoundException(String message) {
    super(message);
  }
}
