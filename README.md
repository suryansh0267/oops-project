# I-ITSM: Intelligent IT Service Management & Incident Resolution System

CS F213 / MAC F212 Object-Oriented Programming project, Semester I 2026-27.

A full-stack service desk that takes an IT incident from **report → classify → prioritise → assign → investigate → resolve → verify → close**, with SLA monitoring, escalation, notifications, a complete audit trail and analytics.

**Stack:** Java 21 · Spring Boot 4 · Spring Security + JWT · Spring Data JPA · PostgreSQL · React 19 · Vite · Tailwind CSS 4 · Recharts

---

## Run it

### Docker (recommended)
```bash
docker compose up --build
```
Web app: http://localhost:3000 · API: http://localhost:8080/api

### Locally
```bash
# 1. PostgreSQL with a database called itsm_db (user/password: postgres/postgres, or set SPRING_DATASOURCE_*)
# 2. Backend
cd backend && ./mvnw spring-boot:run
# 3. Frontend
cd frontend && npm install && npm run dev      # http://localhost:5173
```

### Tests
```bash
cd backend && ./mvnw test                      # unit tests, no database needed
ITSM_DB_TESTS=true ./mvnw test                 # also boots the full app against PostgreSQL
```

### Demo accounts (seeded on first start)

| Role | Username | Password |
|---|---|---|
| Administrator | `admin` | `admin123` |
| Technician (applications) | `tech_app` | `tech123` |
| Technician (network) | `tech_network` | `tech123` |
| Technician (database) | `tech_db` | `tech123` |
| Employee | `emp_john`, `emp_sarah` | `emp123` |
| Frustration demo employee | `emp_demo` | `demo123` |

The seed data is built for the three demo workflows: a cluster of payment-gateway reports (duplicates, correlation, outage alert), an unacknowledged P1 that the SLA monitor escalates within a minute of start-up, and closed incidents whose fixes are suggested for new ones.

---

## Architecture

```
React SPA ──HTTP/JSON + JWT──► Spring Security (JWT filter, URL rules, @PreAuthorize)
                                   │
                              Controllers (thin, DTO in/out, @Valid)
                                   │
          ┌────────────────────────┼─────────────────────────────┐
     IncidentService        Strategy registries            IncidentAccessPolicy
     (orchestration)        (priority, assignment)         (object-level rules)
          │                        │
   State objects            Intelligence                 Events ──► NotificationEventListener
   (lifecycle rules)        (classifier, similarity,                     │
                             technician scorer)              NotificationChannel: In-app · Email · SMS
          │
   Repositories + Specifications ──► PostgreSQL
          ▲
     SlaMonitor (@Scheduled every minute: warn → record breach → escalate)
```

## Object-oriented design

| Pattern | Where | Why |
|---|---|---|
| **State** | `state/IncidentState`, `AbstractIncidentState`, `NewState` … `ClosedState` | Each lifecycle stage knows its legal next stages and whether the SLA clock runs. `NEW → CLOSED` is rejected with `InvalidStateTransitionException`. Adding a stage means adding one class. |
| **Factory / Flyweight** | `state/IncidentStateFactory` | One immutable state object per status. |
| **Strategy** (×2) | `PriorityCalculationStrategy` (multiplicative, weighted), `AssignmentStrategy` (skill-based, load-based, round-robin, smart score) | Algorithms can be swapped at runtime from the Automation screen. |
| **Generic registry** | `StrategyRegistry<T extends NamedStrategy>` | Shared switching logic for both strategy families (generics + inheritance). |
| **Template method** | `PriorityCalculationStrategy.calculatePriority(…, affectedUsers)` default method | Every strategy computes a base priority; the interface raises it for large user counts. |
| **Observer** | `IncidentEvent` published via Spring events → `NotificationEventListener` | `IncidentService` doesn't know who listens; new listeners need no change to it. |
| **Adapter / Strategy** | `notification/NotificationChannel` → `InApp`, `Email`, `Sms` | Add WhatsApp or Slack by adding one `@Component`. |
| **Repository + Specification** | `repository/*`, `IncidentSpecifications` | Composable search over nine criteria without hand-written SQL. |
| **State (chat dialogue)** | `chatbot/ChatState`, `ChatSession`, `ChatService` | Persisted service → description → impact → confirmation flow, with single-use create. |
| **Strategy / Decorator** | `ChatEngine`, `ChatEngineRegistry`, `RuleBasedChatEngine`, `LlmChatEngine` | Rule mode needs no API keys; provider mode enriches draft extraction with fallback. |
| **Adapter** | `LlmClient`, `GeminiLlmClient`, `AnthropicLlmClient`, `LlmTransport` | Provider-specific payloads and safe header-based authentication. |
| **Strategy (signals)** | `FrustrationSignal`, `TextFrustrationSignal`, `BehaviourFrustrationSignal`, optional `LlmSentimentSignal` | Extensible signals without changing aggregation. |
| **Observer** | `FrustrationDetectedEvent` → `FrustrationEventListener` → `IncidentEvent` | Persisted alerts and existing channels with side-effect isolation. |

Also demonstrated: encapsulation (entities behind services and DTOs), abstraction and interfaces (`IncidentState`, `NotificationChannel`, `NamedStrategy`), inheritance (`AbstractIncidentState`, `StrategyRegistry`), polymorphism (every strategy and state), composition (services composed of collaborators), custom exceptions (`InvalidStateTransitionException`, `TechnicianUnavailableException`, `WorkloadExceededException`, `ResourceNotFoundException`, `DuplicateResourceException`, `BadRequestException`) mapped to HTTP codes in `GlobalExceptionHandler`, and records for immutable value objects.

## Intelligent features (brief section "Intelligent Features")

| | Feature | Implementation |
|---|---|---|
| A | Duplicate detection | `TextSimilarity` (Jaccard with stop-words and stemming) + service/category bonus. Checked live while typing and again on submit. |
| B | Incident correlation | Same service within 6 h, or similar text, shown as "Related open incidents"; 3+ reports on one service within an hour raise an outage alert on the admin dashboard. |
| C | Automatic escalation | `SlaMonitor`: unacknowledged P1/P2 past the response deadline is reassigned (level 1), then IT managers are alerted by SMS and email (level 2). |
| D | Smart technician recommendation | `TechnicianScorer`: skill 40 + capacity 30 + experience 15 + history with the service 15, shown with the breakdown in the Assign dialog. Also available as the "Smart score" strategy. |
| E | SLA risk detection | `SlaCalculator`: ON_TRACK / AT_RISK (75% of window used) / BREACHED / MET / MISSED, with a warning sent once. |
| F | Automatic categorisation | `CategoryClassifier` with admin-editable keywords per category, suggested while typing and applied when the reporter leaves it blank. |
| G | Suggested resolution | Fixes from similar resolved incidents shown to technicians and one click from the Resolve dialog. |
| H | In-app IT assistant | Role-aware chat intents, authenticated history, report confirmation and safe self-checks; optional Gemini/Anthropic extraction. |
| I | Frustration detection | Repeated frustration/behaviour signals → smoothed heuristic levels → staff badge, dashboard trend, dismiss and manager alert. |

## Requirements coverage

- **Roles:** employee, technician, administrator, each with its own home screen, navigation and colour.
- **Incidents:** create, view, update (triage), assign, auto-assign, acknowledge, investigate, request/provide info, resolve, verify, reopen, close, feedback.
- **Priority:** impact × urgency × business criticality, raised for 50+ / 500+ affected users. Strategy selectable.
- **SLA:** per-priority policies, response and resolution deadlines, live countdowns, warnings, recorded breaches, escalation.
- **Notifications:** in-app bell plus simulated email and SMS (admin Outbox), routed per event.
- **Audit:** every action with user, previous state, new state and description; per-incident timeline and global log.
- **Search:** keyword or incident number, status, priority, category, service, technician, SLA status, date from/to, "mine only".
- **Security:** BCrypt passwords, JWT, role rules on URLs and methods, per-incident ownership checks, validation on every input, self-registration limited to employees, 401 for expired sessions, no internal errors leaked.

## Configuration

| Property / env | Default | Meaning |
|---|---|---|
| `APP_SLA_MONITOR_INTERVAL_MS` | 60000 | How often the SLA monitor runs |
| `APP_SLA_ESCALATION_ENABLED` | true | Turn automatic escalation on/off |
| `APP_AUTO_ASSIGN_ON_CREATE` | true | Route new incidents to a technician immediately |
| `APP_TIMEZONE` | Asia/Kolkata | Server time zone (timestamps are shown in local time) |
| `APP_JWT_SECRET` | dev value | **Change for any real deployment** |
| `APP_CORS_ALLOWED_ORIGINS` | localhost:3000, 5173 | Allowed browser origins |
| `CHATBOT_ENGINE`, `CHATBOT_LLM_PROVIDER` | `rule`, `gemini` | Select offline mode or optional LLM engine and provider. |
| `GEMINI_API_KEY`, `ANTHROPIC_API_KEY` | empty | Optional provider keys, injected through headers only. |
| `GEMINI_MODEL`, `ANTHROPIC_MODEL` | `gemini-3.8-flash`, `claude-haiku-5-5` | Overridable provider model IDs. |
| `FRUSTRATION_ENABLED` | true | Enable heuristic assessment. |
| `FRUSTRATION_ANNOYED`, `FRUSTRATION_FRUSTRATED`, `FRUSTRATION_CRITICAL` | 25, 55, 85 | Thresholds out of 100. |
| `FRUSTRATION_DECAY_HOURS`, `FRUSTRATION_COOLDOWN_MINUTES` | 12, 30 | Decay half-life and notification cooldown. |
| `FRUSTRATION_RECHECK_INTERVAL_MS`, `FRUSTRATION_REPEAT_WINDOW_DAYS` | 60000, 7 | Behavioural re-check cadence and repeat horizon. |
| `FRUSTRATION_LLM_SENTIMENT`, `FRUSTRATION_AUTO_ESCALATE` | false, false | Optional, off-by-default sentiment signal and automatic priority reconsideration. |
| `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | off / empty / 587 | Optional SMTP; Outbox records still persist without SMTP. |
| `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | false | Optional SMTP authentication and STARTTLS switches. |

## Chatbot and frustration demo (v2)

1. On a clean Docker stack (`docker compose down -v && docker compose up --build`), log in as `emp_john` and open the floating assistant. Say **my vpn is down**, choose an affected service, describe symptoms, choose impact and confirm. The final confirmation invokes the existing IncidentService (priority, SLA, assignment, audit).
2. Try `my open incidents` or `status of INC00001`. As an employee, another reporter's private incident cannot be opened. `tech_app` can try `my queue` or `summarize INC00001`; `admin` can try `system summary`.
3. Log in as `emp_demo` / `demo123`. The additional seeded incident, normally `INC00013` on a fresh database, is already flagged FRUSTRATED. Ask for a manager and confirm the human follow-up. Staff will see a badge; the admin dashboard plots the last 14 days of alert history. A CRITICAL rise sends manager notifications through in-app, email Outbox, and SMS Outbox. No live chat agent is connected.
4. For a calm control, send `the database server is not responding` from a fresh conversation. Technical failures alone should not trigger a frustration alert. These are **heuristic** assessments; review before taking action because false positives are possible.

The admin Automation page shows the currently effective frustration configuration in **read-only** form. Edit `.env` and restart to change it. Copy `.env.example` to `.env` for optional LLM/SMTP, and never commit real keys. The default `CHATBOT_ENGINE=rule` works without any key. A bad/missing key falls back to the rule engine. In an existing PostgreSQL volume, legacy Hibernate CHECK constraints may reject new enum literals; start from scratch with `docker compose down -v` (this **deletes all database contents**) or apply a controlled schema migration.

### Push and CI

The included `.github/workflows/ci.yml` runs `cd backend && chmod +x mvnw && ./mvnw test` and `cd frontend && npm ci && npm run build`. Before pushing, run both locally and `docker compose down -v && docker compose up --build` on a disposable development database. If your ZIP extraction removed the executable flag, run `chmod +x backend/mvnw` and `git update-index --chmod=+x backend/mvnw` in Git. Secret scan example: `rg -n 'AIza|sk-ant' --glob '!*.zip' .`.

### Known limitations

No live support agent is connected; the chatbot creates a recorded human-follow-up request. Chat history restores text, while interactive cards are provided on new messages. The in-memory rate limiter resets on restart and is per server instance. LLM enrichment requires external provider access and valid keys. The scheduled behaviour monitor covers a bounded first page of 100 incidents per interval. The chat flow is implemented by one persisted enum-state handler (not a separate class per dialogue state). A full end-to-end backend validation is still necessary wherever Maven Central, PostgreSQL and Docker can run.
