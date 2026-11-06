package main

import (
	"bytes"
	"log"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

func TestAlertsOnFailureRecoveryAndSilence(t *testing.T) {
	var sent []string
	t0 := time.Date(2026, 10, 20, 2, 0, 0, 0, time.UTC)
	m := newMonitor([]string{"registry"}, 2, 5*time.Minute, func(_, s string) bool {
		sent = append(sent, s)
		return true
	}, t0)
	hb := func(ok bool, at time.Duration) {
		m.heartbeat(heartbeat{Source: "registry", Checks: map[string]checkResult{"backend": {OK: ok, Detail: "http 503"}}}, t0.Add(at))
	}

	hb(true, time.Minute)
	hb(false, 2*time.Minute) // one miss: no alert yet
	if len(sent) != 0 {
		t.Fatalf("alerted on a single miss: %v", sent)
	}
	hb(false, 3*time.Minute)
	hb(false, 4*time.Minute) // still failing: alert only once
	hb(true, 5*time.Minute)
	m.sweep(t0.Add(8 * time.Minute)) // 3 min of silence: fine
	m.sweep(t0.Add(11 * time.Minute))
	m.sweep(t0.Add(12 * time.Minute)) // already reported
	hb(true, 20*time.Minute)

	want := []string{"🔴 registry · backend:", "🟢 registry · backend:", "🔴 registry:", "🟢 registry:"}
	if len(sent) != len(want) {
		t.Fatalf("got %d messages: %q", len(sent), sent)
	}
	for i, prefix := range want {
		if !strings.HasPrefix(sent[i], prefix) {
			t.Errorf("message %d = %q, want prefix %q", i, sent[i], prefix)
		}
	}
}

func TestDeadServerIsReportedAfterNotifierRestart(t *testing.T) {
	var sent []string
	t0 := time.Now()
	m := newMonitor([]string{"registry"}, 2, 5*time.Minute, func(_, s string) bool {
		sent = append(sent, s)
		return true
	}, t0)
	m.sweep(t0.Add(6 * time.Minute)) // never heard from it since start
	if len(sent) != 1 || !strings.HasPrefix(sent[0], "🔴 registry:") {
		t.Fatalf("got %q", sent)
	}
}

const syntheticToken = "SYNTHETIC_TOKEN_123"

func localMessenger(t *testing.T, handler http.HandlerFunc) func(string) error {
	t.Helper()
	server := httptest.NewServer(handler)
	t.Cleanup(server.Close)
	t.Setenv("MESSENGER", "telegram")
	t.Setenv("TELEGRAM_BOT_TOKEN", syntheticToken)
	t.Setenv("TELEGRAM_CHAT_ID", "synthetic-chat")
	t.Setenv("TELEGRAM_API_URL", server.URL)
	return messenger()
}

type receivedMessages struct {
	mu    sync.Mutex
	texts []string
}

func (r *receivedMessages) accept(w http.ResponseWriter, request *http.Request) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.texts = append(r.texts, request.FormValue("text"))
	w.WriteHeader(http.StatusOK)
}

func (r *receivedMessages) snapshot() []string {
	r.mu.Lock()
	defer r.mu.Unlock()
	return append([]string(nil), r.texts...)
}

func failingHeartbeat() heartbeat {
	return heartbeat{Source: "registry", Checks: map[string]checkResult{
		"backup": {OK: false, Detail: "fixture outage"},
	}}
}

func TestActiveAlertSurvivesFiveDeliveryFailures(t *testing.T) {
	var attempts atomic.Int32
	var received receivedMessages
	q := newDeliveryQueue(localMessenger(t, func(w http.ResponseWriter, r *http.Request) {
		if attempts.Add(1) <= 5 {
			w.WriteHeader(http.StatusServiceUnavailable)
			return
		}
		received.accept(w, r)
	}))
	now := time.Now()
	m := newMonitor([]string{"registry"}, 1, time.Hour, q.add, now)
	m.heartbeat(failingHeartbeat(), now)
	for range 5 {
		q.dispatch(now)
		if delay := q.dispatch(now); delay <= 0 {
			t.Fatal("failed message retried without backoff")
		}
		now = now.Add(2 * time.Minute)
		m.heartbeat(failingHeartbeat(), now)
		m.sweep(now)
	}
	q.dispatch(now)
	m.heartbeat(failingHeartbeat(), now)
	m.sweep(now)
	q.dispatch(now.Add(time.Minute))
	got := received.snapshot()
	if attempts.Load() != 6 || len(got) != 1 || !strings.HasPrefix(got[0], "🔴 registry · backup") {
		t.Fatalf("attempts=%d delivered=%q", attempts.Load(), got)
	}
}

func TestQueueOverflowRetriesActiveStateFromSweep(t *testing.T) {
	var received receivedMessages
	q := newDeliveryQueue(localMessenger(t, received.accept))
	for range 100 {
		if !q.add("", "event") {
			t.Fatal("queue filled before capacity")
		}
	}
	if q.add("", "overflow") {
		t.Fatal("queue exceeded capacity")
	}
	now := time.Now()
	m := newMonitor([]string{"registry"}, 1, time.Hour, q.add, now)
	m.heartbeat(failingHeartbeat(), now) // red cannot enter yet
	q.dispatch(now)                     // one slot becomes free
	m.sweep(now)                        // no new heartbeat needed to retry admission
	for range 100 {
		q.dispatch(now)
	}
	m.sweep(now)
	q.dispatch(now)
	got := received.snapshot()
	reds := 0
	for _, text := range got {
		if strings.HasPrefix(text, "🔴 registry · backup") {
			reds++
		}
	}
	if len(got) != 101 || reds != 1 {
		t.Fatalf("delivered=%d red alerts=%d", len(got), reds)
	}
}

func TestRecoveryReplacesQueuedRedEvenAtCapacity(t *testing.T) {
	for _, silence := range []bool{false, true} {
		name := "check"
		if silence {
			name = "silence"
		}
		t.Run(name, func(t *testing.T) {
			var attempts atomic.Int32
			var received receivedMessages
			q := newDeliveryQueue(localMessenger(t, func(w http.ResponseWriter, r *http.Request) {
				if attempts.Add(1) == 1 {
					w.WriteHeader(http.StatusServiceUnavailable)
					return
				}
				received.accept(w, r)
			}))
			now := time.Now()
			m := newMonitor([]string{"registry"}, 1, time.Minute, q.add, now)
			if silence {
				m.sweep(now.Add(2 * time.Minute))
			} else {
				m.heartbeat(failingHeartbeat(), now)
			}
			q.dispatch(now)
			for range 99 {
				q.add("", "event")
			}
			m.heartbeat(heartbeat{Source: "registry", Checks: map[string]checkResult{"backup": {OK: true}}}, now.Add(3*time.Minute))
			for range 100 {
				q.dispatch(now.Add(4 * time.Minute))
			}
			got := received.snapshot()
			greens := 0
			for _, text := range got {
				if strings.HasPrefix(text, "🔴") {
					t.Fatalf("stale red delivered after recovery: %q", got)
				}
				if strings.HasPrefix(text, "🟢") {
					greens++
				}
			}
			if len(got) != 100 || greens != 1 {
				t.Fatalf("delivered=%d greens=%d", len(got), greens)
			}
		})
	}
}

func TestRecoveryWhileRedIsInFlightIsNotLost(t *testing.T) {
	entered, release := make(chan struct{}), make(chan struct{})
	var received receivedMessages
	q := newDeliveryQueue(localMessenger(t, func(w http.ResponseWriter, r *http.Request) {
		if strings.HasPrefix(r.FormValue("text"), "🔴") {
			close(entered)
			<-release
		}
		received.accept(w, r)
	}))
	now := time.Now()
	m := newMonitor([]string{"registry"}, 1, time.Hour, q.add, now)
	m.heartbeat(failingHeartbeat(), now)
	done := make(chan struct{})
	go func() {
		q.dispatch(now)
		close(done)
	}()
	select {
	case <-entered:
	case <-time.After(5 * time.Second):
		close(release)
		t.Fatal("red never reached the local messenger")
	}
	m.heartbeat(heartbeat{Source: "registry", Checks: map[string]checkResult{"backup": {OK: true}}}, now)
	close(release)
	select {
	case <-done:
	case <-time.After(5 * time.Second):
		t.Fatal("red delivery did not complete")
	}
	q.dispatch(now)
	got := received.snapshot()
	if len(got) != 2 || !strings.HasPrefix(got[0], "🔴") || !strings.HasPrefix(got[1], "🟢") {
		t.Fatalf("in-flight red/recovery order: %q", got)
	}
}

func TestFailedMessageDoesNotBlockFollowingMessage(t *testing.T) {
	var received receivedMessages
	q := newDeliveryQueue(localMessenger(t, func(w http.ResponseWriter, r *http.Request) {
		if r.FormValue("text") == "unavailable" {
			w.WriteHeader(http.StatusServiceUnavailable)
			return
		}
		received.accept(w, r)
	}))
	now := time.Now()
	q.add("", "unavailable")
	q.add("", "available")
	q.dispatch(now)
	q.dispatch(now)
	if got := received.snapshot(); len(got) != 1 || got[0] != "available" {
		t.Fatalf("following message blocked: %q", got)
	}
	if delay := q.dispatch(now); delay <= 0 {
		t.Fatal("failing message has no backoff")
	}
}

func TestRetryBackoffIsBoundedWithoutBusyLoop(t *testing.T) {
	q := newDeliveryQueue(localMessenger(t, func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusServiceUnavailable)
	}))
	now := time.Now()
	q.add("", "outage")
	for _, want := range []time.Duration{1, 2, 4, 8, 16, 32, 60, 60} {
		q.dispatch(now)
		delay := q.dispatch(now)
		if delay < want*time.Second || delay > (want+1)*time.Second {
			t.Fatalf("retry delay=%s want about %ds", delay, want)
		}
		now = now.Add(delay)
	}
}

func TestTransportErrorLogsCauseWithoutBotToken(t *testing.T) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	address := listener.Addr().String()
	listener.Close()
	t.Setenv("MESSENGER", "telegram")
	t.Setenv("TELEGRAM_BOT_TOKEN", syntheticToken)
	t.Setenv("TELEGRAM_CHAT_ID", "synthetic-chat")
	t.Setenv("TELEGRAM_API_URL", "http://"+address)
	var logs bytes.Buffer
	previous := log.Writer()
	log.SetOutput(&logs)
	t.Cleanup(func() { log.SetOutput(previous) })
	q := newDeliveryQueue(messenger())
	q.add("", "transport fixture")
	q.dispatch(time.Now())
	got := logs.String()
	if strings.Contains(got, syntheticToken) || strings.Contains(got, "/bot") || strings.Contains(got, "http://") {
		t.Fatalf("transport log exposed request URL: %q", got)
	}
	if !strings.Contains(got, "telegram transport:") || !strings.Contains(got, "dial tcp") {
		t.Fatalf("transport cause was suppressed: %q", got)
	}
}

func TestEventResponseRejectsFullQueueAndAcceptsRetry(t *testing.T) {
	var received receivedMessages
	q := newDeliveryQueue(localMessenger(t, received.accept))
	for range 100 {
		q.add("", "existing event")
	}
	now := time.Now()
	m := newMonitor([]string{"registry"}, 1, time.Hour, q.add, now)
	e := event{Source: "registry", Level: "error", Text: "retry this event"}
	rejected := httptest.NewRecorder()
	m.acceptEvent(rejected, e)
	if rejected.Code != http.StatusServiceUnavailable {
		t.Fatalf("full queue falsely acknowledged event: HTTP %d", rejected.Code)
	}
	q.dispatch(now)
	accepted := httptest.NewRecorder()
	m.acceptEvent(accepted, e)
	if accepted.Code != http.StatusNoContent {
		t.Fatalf("available queue rejected event retry: HTTP %d", accepted.Code)
	}
	for range 100 {
		q.dispatch(now)
	}
	retries := 0
	for _, text := range received.snapshot() {
		if strings.Contains(text, "retry this event") {
			retries++
		}
	}
	if retries != 1 {
		t.Fatalf("accepted retry delivered %d times", retries)
	}
}

func TestMultipleCappedRetriesDoNotStarveLateRecovery(t *testing.T) {
	var failed atomic.Int32
	var received receivedMessages
	q := newDeliveryQueue(localMessenger(t, func(w http.ResponseWriter, r *http.Request) {
		if r.FormValue("text") == "unavailable" {
			failed.Add(1)
			w.WriteHeader(http.StatusServiceUnavailable)
			return
		}
		received.accept(w, r)
	}))
	for range 6 {
		q.add("", "unavailable")
	}
	// Start in the steady state of capped retry backoff, without slow warmup.
	for _, item := range q.items {
		item.attempts = 7
	}
	now := time.Now()
	// Each attempt makes real loopback HTTP. Fifteen-second scheduling intervals
	// are virtual, representing a slow worker rather than sleeping in the test.
	for range 6 {
		q.dispatch(now)
		now = now.Add(15 * time.Second)
	}
	q.add("recovery", "recovery")
	for range 7 {
		q.dispatch(now)
		now = now.Add(15 * time.Second)
	}
	got := received.snapshot()
	if failed.Load() != 12 || len(got) != 1 || got[0] != "recovery" {
		t.Fatalf("late recovery starved: failed=%d delivered=%q", failed.Load(), got)
	}
}
