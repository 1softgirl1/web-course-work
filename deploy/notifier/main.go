// Notifier: a tiny dead man's switch that lives outside the customer's network.
//
// The backup service on the registry server sends a heartbeat every minute with the
// state of its checks, and events (backup failed, settings changed) when they happen.
// The notifier tells the messenger when a check goes red or green again, when an event
// arrives, and - the point of living elsewhere - when heartbeats stop coming at all.
// Only service metadata travels here, never registry data.
package main

import (
	"crypto/subtle"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"
	_ "time/tzdata" // the scratch image has no zoneinfo
)

type checkResult struct {
	OK     bool   `json:"ok"`
	Detail string `json:"detail"`
}

type heartbeat struct {
	Source string                 `json:"source"`
	Checks map[string]checkResult `json:"checks"`
}

type event struct {
	Source string `json:"source"`
	Level  string `json:"level"` // error | info
	Text   string `json:"text"`
}

type checkState struct {
	fails   int
	alerted bool
	pending string
}

type sourceState struct {
	lastSeen time.Time
	silent   bool
	checks   map[string]*checkState
	pending  string
}

type monitor struct {
	mu            sync.Mutex
	sources       map[string]*sourceState
	failThreshold int
	silence       time.Duration
	notify        func(key, text string) bool
}

func newMonitor(expected []string, failThreshold int, silence time.Duration, notify func(key, text string) bool, now time.Time) *monitor {
	m := &monitor{sources: map[string]*sourceState{}, failThreshold: failThreshold, silence: silence, notify: notify}
	// Expected sources start "seen now": after a notifier restart a dead server is still reported.
	for _, s := range expected {
		m.sources[s] = &sourceState{lastSeen: now, checks: map[string]*checkState{}}
	}
	return m
}

func (m *monitor) source(name string, now time.Time) *sourceState {
	s, ok := m.sources[name]
	if !ok {
		s = &sourceState{lastSeen: now, checks: map[string]*checkState{}}
		m.sources[name] = s
	}
	return s
}

func (m *monitor) heartbeat(hb heartbeat, now time.Time) {
	m.mu.Lock()
	defer m.mu.Unlock()
	s := m.source(hb.Source, now)
	if s.silent {
		s.pending = fmt.Sprintf("🟢 %s: снова на связи (молчал %s)", hb.Source, minutes(now.Sub(s.lastSeen)))
		s.silent = false
	}
	s.lastSeen = now
	for name, r := range hb.Checks {
		c, ok := s.checks[name]
		if !ok {
			c = &checkState{}
			s.checks[name] = c
		}
		switch {
		case r.OK && c.alerted:
			*c = checkState{pending: fmt.Sprintf("🟢 %s · %s: восстановилось", hb.Source, name)}
		case r.OK:
			c.fails = 0
		default:
			c.fails++
			// Two misses in a row before alerting: one slow minute is not an incident.
			if c.fails >= m.failThreshold && !c.alerted {
				c.pending = fmt.Sprintf("🔴 %s · %s: %s", hb.Source, name, clip(r.Detail, 300))
				c.alerted = true
			}
		}
	}
	m.flush()
}

func (m *monitor) event(e event) bool {
	icon := "⚠️"
	if e.Level == "info" {
		icon = "ℹ️"
	}
	if !m.notify("", fmt.Sprintf("%s %s: %s", icon, e.Source, clip(e.Text, 1000))) {
		log.Print("queue full, event rejected")
		return false
	}
	return true
}

func (m *monitor) acceptEvent(w http.ResponseWriter, e event) {
	if !m.event(e) {
		http.Error(w, "notification queue full", http.StatusServiceUnavailable)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// sweep is the "message that was not cancelled": no heartbeat for too long means the server is down.
func (m *monitor) sweep(now time.Time) {
	m.mu.Lock()
	defer m.mu.Unlock()
	for name, s := range m.sources {
		if !s.silent && now.Sub(s.lastSeen) > m.silence {
			s.pending = fmt.Sprintf("🔴 %s: нет связи больше %s (последний сигнал %s)",
				name, minutes(m.silence), s.lastSeen.In(zone).Format("02.01 15:04"))
			s.silent = true
		}
	}
	m.flush()
}

// Keep unaccepted state transitions on their source/check, not in another unbounded queue.
// Every sweep retries admission; a recovery supersedes a still-pending red notification.
func (m *monitor) flush() {
	for source, s := range m.sources {
		if s.pending != "" && m.notify(fmt.Sprintf("silence:%q", source), s.pending) {
			s.pending = ""
		}
		for name, c := range s.checks {
			if c.pending != "" && m.notify(fmt.Sprintf("check:%q:%q", source, name), c.pending) {
				c.pending = ""
			}
		}
	}
}

// zone is only for message timestamps: the people reading them are in Kemerovo.
var zone = func() *time.Location {
	if loc, err := time.LoadLocation(envOr("TZ_NAME", "Asia/Novokuznetsk")); err == nil {
		return loc
	}
	return time.UTC
}()

func minutes(d time.Duration) string {
	if d < time.Hour {
		return fmt.Sprintf("%d мин", int(d.Round(time.Minute).Minutes()))
	}
	return fmt.Sprintf("%d ч %d мин", int(d.Hours()), int(d.Minutes())%60)
}

func probe(client *http.Client, u string) checkResult {
	resp, err := client.Get(u)
	if err != nil {
		if e, ok := err.(interface{ Timeout() bool }); ok && e.Timeout() {
			return checkResult{OK: false, Detail: "не открывается (таймаут)"}
		}
		return checkResult{OK: false, Detail: "не открывается (нет соединения)"}
	}
	resp.Body.Close()
	if resp.StatusCode >= 400 {
		return checkResult{OK: false, Detail: fmt.Sprintf("HTTP %d", resp.StatusCode)}
	}
	return checkResult{OK: true}
}

func clip(s string, n int) string {
	if r := []rune(strings.TrimSpace(s)); len(r) > n {
		return string(r[:n]) + "…"
	}
	return strings.TrimSpace(s)
}

// Messenger: the only place that knows about Telegram. Another messenger = another function here.
func messenger() func(string) error {
	switch os.Getenv("MESSENGER") {
	case "log":
		return func(text string) error { log.Printf("MESSAGE: %s", text); return nil }
	default:
		token, chat := mustEnv("TELEGRAM_BOT_TOKEN"), mustEnv("TELEGRAM_CHAT_ID")
		api := envOr("TELEGRAM_API_URL", "https://api.telegram.org")
		client := &http.Client{Timeout: 15 * time.Second}
		return func(text string) error {
			resp, err := client.PostForm(api+"/bot"+token+"/sendMessage", url.Values{"chat_id": {chat}, "text": {text}})
			if err != nil {
				// net/http's url.Error includes the bot token in URL. Preserve the
				// transport cause, but never return that URL or an embedded token.
				var transport *url.Error
				if errors.As(err, &transport) {
					err = transport.Err
				}
				return fmt.Errorf("telegram transport: %s", strings.ReplaceAll(err.Error(), token, "[redacted]"))
			}
			resp.Body.Close()
			if resp.StatusCode != http.StatusOK {
				return fmt.Errorf("telegram status %d", resp.StatusCode)
			}
			return nil
		}
	}
}

type queuedMessage struct {
	key      string
	text     string
	attempts int
	next     time.Time
}

type deliveryQueue struct {
	mu    sync.Mutex
	items []*queuedMessage
	wake  chan struct{}
	send  func(string) error
}

func newDeliveryQueue(send func(string) error) *deliveryQueue {
	return &deliveryQueue{send: send, wake: make(chan struct{}, 1)}
}

// State notifications coalesce by source/check; events (empty key) remain distinct.
// Replacements are admitted even at capacity, so a recovery cannot leave a queued red behind.
func (q *deliveryQueue) add(key, text string) bool {
	q.mu.Lock()
	defer q.mu.Unlock()
	for i, item := range q.items {
		if key != "" && item.key == key {
			q.items[i] = &queuedMessage{key: key, text: text}
			q.signal()
			return true
		}
	}
	if len(q.items) >= 100 {
		return false
	}
	q.items = append(q.items, &queuedMessage{key: key, text: text})
	q.signal()
	return true
}

func (q *deliveryQueue) signal() {
	select {
	case q.wake <- struct{}{}:
	default:
	}
}

// dispatch makes at most one attempt. Failed messages stay pending indefinitely,
// with 1s..1min backoff. Failed items rotate to the tail so several slow retries
// cannot starve a later message once their backoff intervals overlap.
func (q *deliveryQueue) dispatch(now time.Time) time.Duration {
	q.mu.Lock()
	var ready *queuedMessage
	delay := time.Hour
	for _, item := range q.items {
		if !item.next.After(now) {
			ready = item
			break
		}
		if wait := item.next.Sub(now); wait < delay {
			delay = wait
		}
	}
	q.mu.Unlock()
	if ready == nil {
		return delay
	}

	started := time.Now()
	err := q.send(ready.text)
	q.mu.Lock()
	defer q.mu.Unlock()
	for i, item := range q.items {
		// A newer state may have replaced this message while send was in flight.
		if item != ready {
			continue
		}
		if err == nil {
			q.items = append(q.items[:i], q.items[i+1:]...)
		} else {
			if item.attempts < 7 {
				item.attempts++
			}
			backoff := min(time.Second<<uint(item.attempts-1), time.Minute)
			item.next = now.Add(time.Since(started) + backoff)
			copy(q.items[i:], q.items[i+1:])
			q.items[len(q.items)-1] = item
		}
		break
	}
	if err != nil {
		log.Printf("send failed: %v", err)
	}
	return 0
}

// HTTP handlers only enqueue. One worker and one timer bound delivery resource use.
// Pending delivery is memory-only. After restart, heartbeats and the expected-source
// silence timeout rebuild active alarms; queued events and old transitions are lost.
func queue(send func(string) error) func(key, text string) bool {
	q := newDeliveryQueue(send)
	go func() {
		timer := time.NewTimer(time.Hour)
		defer timer.Stop()
		for {
			delay := q.dispatch(time.Now())
			timer.Reset(delay)
			select {
			case <-timer.C:
			case <-q.wake:
			}
		}
	}()
	return q.add
}

func main() {
	token := mustEnv("NOTIFIER_TOKEN")
	silence := time.Duration(envInt("SILENCE_MINUTES", 5)) * time.Minute
	m := newMonitor(strings.Fields(strings.ReplaceAll(envOr("EXPECTED_SOURCES", "registry"), ",", " ")),
		envInt("FAIL_THRESHOLD", 2), silence, queue(messenger()), time.Now())

	go func() {
		for now := range time.Tick(15 * time.Second) {
			m.sweep(now)
		}
	}()

	// External probes: the site as users see it, checked from outside the customer's network.
	if urls := strings.Fields(strings.ReplaceAll(os.Getenv("PROBE_URLS"), ",", " ")); len(urls) > 0 {
		source := envOr("PROBE_SOURCE", "сайт")
		go func() {
			client := &http.Client{Timeout: 15 * time.Second}
			for range time.Tick(time.Minute) {
				checks := map[string]checkResult{}
				for _, u := range urls {
					checks[u] = probe(client, u)
				}
				m.heartbeat(heartbeat{Source: source, Checks: checks}, time.Now())
			}
		}()
		log.Printf("probing %v every minute", urls)
	}

	authorized := func(r *http.Request) bool {
		got := strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer ")
		return subtle.ConstantTimeCompare([]byte(got), []byte(token)) == 1
	}
	decode := func(w http.ResponseWriter, r *http.Request, v any) bool {
		if r.Method != http.MethodPost || !authorized(r) {
			http.Error(w, "forbidden", http.StatusForbidden)
			return false
		}
		if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 16<<10)).Decode(v); err != nil {
			http.Error(w, "bad json", http.StatusBadRequest)
			return false
		}
		return true
	}

	http.HandleFunc("/heartbeat", func(w http.ResponseWriter, r *http.Request) {
		var hb heartbeat
		if decode(w, r, &hb) && hb.Source != "" {
			m.heartbeat(hb, time.Now())
			w.WriteHeader(http.StatusNoContent)
		}
	})
	http.HandleFunc("/event", func(w http.ResponseWriter, r *http.Request) {
		var e event
		if decode(w, r, &e) && e.Source != "" && e.Text != "" {
			m.acceptEvent(w, e)
		}
	})
	http.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) { w.Write([]byte("ok")) })

	addr := envOr("LISTEN_ADDR", ":8080")
	log.Printf("notifier listening on %s, silence timeout %s", addr, silence)
	log.Fatal(http.ListenAndServe(addr, nil))
}

func mustEnv(k string) string {
	v := os.Getenv(k)
	if v == "" {
		log.Fatalf("%s is required", k)
	}
	return v
}

func envOr(k, def string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return def
}

func envInt(k string, def int) int {
	if v, err := strconv.Atoi(os.Getenv(k)); err == nil && v > 0 {
		return v
	}
	return def
}
