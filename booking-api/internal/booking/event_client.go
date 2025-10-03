package booking

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

// HTTPEventAPIClient implements EventAPIClient using HTTP
type HTTPEventAPIClient struct {
	baseURL    string
	httpClient *http.Client
}

// NewHTTPEventAPIClient creates a new HTTP-based event API client
func NewHTTPEventAPIClient(baseURL string) *HTTPEventAPIClient {
	return &HTTPEventAPIClient{
		baseURL:    baseURL,
		httpClient: &http.Client{Timeout: 10 * time.Second},
	}
}

// GetEvent fetches event data from the event-api service
func (c *HTTPEventAPIClient) GetEvent(ctx context.Context, eventID string) (*Event, error) {
	url := fmt.Sprintf("%s/api/v1/events/%s", c.baseURL, eventID)

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to call event-api: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusNotFound {
		return nil, fmt.Errorf("event not found")
	}

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("event-api returned status %d", resp.StatusCode)
	}

	var event Event
	if err := json.NewDecoder(resp.Body).Decode(&event); err != nil {
		return nil, fmt.Errorf("failed to decode event: %w", err)
	}

	return &event, nil
}
