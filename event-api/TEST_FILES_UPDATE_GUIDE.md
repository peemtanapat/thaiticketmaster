# ⚠️ Test Files Update Required

The Java test files need to be updated to use `OffsetDateTime` instead of `LocalDateTime` for showDateTimes.

---

## Files That Need Updates

### Test Files:
1. `EventTest.java`
2. `EventDTOTest.java`
3. `EventControllerTest.java`
4. `EventControllerIntegrationTest.java`
5. `EventServiceIntegrationTest.java`
6. `EventServiceTest.java`

---

## Required Changes

### Pattern to Find:
```java
// OLD - LocalDateTime without timezone
List<LocalDateTime> showTimes = Arrays.asList(LocalDateTime.now().plusDays(30));
```

### Replace With:
```java
// NEW - OffsetDateTime with timezone
List<OffsetDateTime> showTimes = Arrays.asList(OffsetDateTime.now().plusDays(30));
```

---

## Import Statement Changes

### Add Import:
```java
import java.time.OffsetDateTime;
```

### Keep These:
```java
import java.time.LocalDateTime;  // Still used for onSaleDateTime, createdAt, updatedAt
```

---

## Example: EventTest.java

### Before:
```java
@Test
@DisplayName("Constructor_WithValidParameters_ShouldCreateEvent")
void constructor_WithValidParameters_ShouldCreateEvent() {
    // Arrange
    List<LocalDateTime> showTimes = Arrays.asList(LocalDateTime.now().plusDays(30));
    LocalDateTime onSaleDate = LocalDateTime.now().plusDays(1);
    BigDecimal price = new BigDecimal("500.00");

    Category category = new Category();
    category.setId(1L);
    category.setName("Concert");

    // Act
    Event event = new Event(
        "Test Event",
        category,
        showTimes,
        "Bangkok Arena",
        onSaleDate,
        price,
        "Test Details",
        "Test Conditions",
        EventStatus.ON_SALE,
        "1 hour before");

    // Assert
    assertNotNull(event);
    assertEquals("Test Event", event.getName());
    assertEquals(showTimes, event.getShowDateTimes());
}
```

### After:
```java
@Test
@DisplayName("Constructor_WithValidParameters_ShouldCreateEvent")
void constructor_WithValidParameters_ShouldCreateEvent() {
    // Arrange
    List<OffsetDateTime> showTimes = Arrays.asList(OffsetDateTime.now().plusDays(30));
    LocalDateTime onSaleDate = LocalDateTime.now().plusDays(1);
    BigDecimal price = new BigDecimal("500.00");

    Category category = new Category();
    category.setId(1L);
    category.setName("Concert");

    // Act
    Event event = new Event(
        "Test Event",
        category,
        showTimes,
        "Bangkok Arena",
        onSaleDate,
        price,
        "Test Details",
        "Test Conditions",
        EventStatus.ON_SALE,
        "1 hour before");

    // Assert
    assertNotNull(event);
    assertEquals("Test Event", event.getName());
    assertEquals(showTimes, event.getShowDateTimes());
}
```

**Key Changes:**
- Line 4: `LocalDateTime` → `OffsetDateTime`
- Line 4: `LocalDateTime.now()` → `OffsetDateTime.now()`

---

## Automated Find & Replace

### Using VS Code:

1. **Open Find & Replace** (Cmd+Shift+H / Ctrl+Shift+H)

2. **Find:** `List<LocalDateTime> showTimes = Arrays.asList\(LocalDateTime`
   **Replace:** `List<OffsetDateTime> showTimes = Arrays.asList(OffsetDateTime`
   
3. **Find:** `showTimes = Arrays.asList\(LocalDateTime`
   **Replace:** `showTimes = Arrays.asList(OffsetDateTime`

4. **Files to include:** `event-api/src/test/**/*.java`

5. Click **Replace All**

---

## Manual Updates Needed

### EventControllerIntegrationTest.java
```java
// Line ~42
// OLD:
futureDate = LocalDateTime.now().plusDays(30);

// NEW:
futureDate = OffsetDateTime.now().plusDays(30);
```

### EventServiceIntegrationTest.java
```java
// Line ~34
// OLD:
futureDate = LocalDateTime.now().plusDays(30);

// NEW:
futureDate = OffsetDateTime.now().plusDays(30);
```

### EventTest.java - multipleShowTimes_CanBeAdded
```java
// Lines ~145-147
// OLD:
List<LocalDateTime> showTimes = Arrays.asList(
    LocalDateTime.now().plusDays(1),
    LocalDateTime.now().plusDays(2),
    LocalDateTime.now().plusDays(3));

// NEW:
List<OffsetDateTime> showTimes = Arrays.asList(
    OffsetDateTime.now().plusDays(1),
    OffsetDateTime.now().plusDays(2),
    OffsetDateTime.now().plusDays(3));
```

---

## Variable Declaration Updates

### Pattern:
```java
// OLD
List<LocalDateTime> showTimes

// NEW
List<OffsetDateTime> showTimes
```

### Affected Variables:
- `showTimes`
- `futureDate` (in integration tests)
- Any list of show date/times in test methods

---

## Import Updates

### Add to all test files:
```java
import java.time.OffsetDateTime;
```

### Example Import Section:
```java
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;  // ← ADD THIS
import java.util.Arrays;
import java.util.List;
```

---

## Verification

After making changes, run tests:

```bash
cd event-api
./mvnw test
```

### Expected Results:
✅ All tests pass  
✅ No compilation errors  
✅ No type mismatch errors

### If Errors Occur:
Check for:
- Missing `import java.time.OffsetDateTime;`
- Missed `LocalDateTime` → `OffsetDateTime` conversions
- Variable declarations still using `List<LocalDateTime>`

---

## Quick Script

Save as `update_tests.sh`:

```bash
#!/bin/bash
cd event-api/src/test/java

# Update List declarations
find . -name "*.java" -exec sed -i '' 's/List<LocalDateTime> showTimes = Arrays.asList(LocalDateTime/List<OffsetDateTime> showTimes = Arrays.asList(OffsetDateTime/g' {} +

# Update standalone assignments
find . -name "*.java" -exec sed -i '' 's/showTimes = Arrays.asList(LocalDateTime/showTimes = Arrays.asList(OffsetDateTime/g' {} +

# Update futureDate declarations
find . -name "*.java" -exec sed -i '' 's/futureDate = LocalDateTime.now/futureDate = OffsetDateTime.now/g' {} +

echo "✅ Test files updated. Run ./mvnw test to verify."
```

Make executable: `chmod +x update_tests.sh`  
Run: `./update_tests.sh`

---

## Status

**Required:** Yes - Tests will fail without these updates  
**Priority:** High  
**Effort:** ~15 minutes manual updates or 2 minutes with script

---

## Next Steps

1. Update all test files (manually or with script)
2. Add `import java.time.OffsetDateTime;` to each test file
3. Run `./mvnw test` to verify
4. Fix any remaining compilation errors
5. Commit changes

---

**Note:** The entity and DTO changes are already complete. Only test files remain.
