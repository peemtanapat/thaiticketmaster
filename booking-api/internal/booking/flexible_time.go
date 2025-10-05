package booking

import (
	"fmt"
	"strings"
	"time"
)

// FlexibleTime is a custom time type that can parse multiple timestamp formats
// This is needed for compatibility with Java LocalDateTime format (no timezone)
// and standard RFC3339 format (with timezone)
type FlexibleTime struct {
	time.Time
}

// UnmarshalJSON implements custom JSON unmarshaling for FlexibleTime
func (ft *FlexibleTime) UnmarshalJSON(b []byte) error {
	s := strings.Trim(string(b), "\"")
	if s == "null" || s == "" {
		ft.Time = time.Time{}
		return nil
	}

	// Try multiple timestamp formats
	formats := []string{
		time.RFC3339,                 // "2006-01-02T15:04:05Z07:00" (with timezone)
		"2006-01-02T15:04:05",        // Java LocalDateTime format (no timezone)
		time.RFC3339Nano,             // "2006-01-02T15:04:05.999999999Z07:00"
		"2006-01-02T15:04:05.999999", // with nanoseconds, no timezone
	}

	var parseErr error
	for _, format := range formats {
		t, err := time.Parse(format, s)
		if err == nil {
			ft.Time = t
			return nil
		}
		parseErr = err
	}

	return fmt.Errorf("unable to parse time %q: %v", s, parseErr)
}

// MarshalJSON implements custom JSON marshaling for FlexibleTime
func (ft FlexibleTime) MarshalJSON() ([]byte, error) {
	if ft.Time.IsZero() {
		return []byte("null"), nil
	}
	return []byte(fmt.Sprintf("\"%s\"", ft.Time.Format(time.RFC3339))), nil
}
