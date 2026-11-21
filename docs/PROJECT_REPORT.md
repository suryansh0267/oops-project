# Intelligent IT Service Management & Incident Resolution System (I-ITSM)
## Comprehensive Academic Project Report & Defense Document

**Course:** CS F213 / MAC F212 — Object-Oriented Programming (Semester-I, 2026-2027)  
**System Name:** Intelligent IT Service Management & Incident Resolution System (I-ITSM)  
**Technology Stack:** Java 21, Spring Boot 4, PostgreSQL 18, React 19, Tailwind CSS, Vite  

---

## 1. Problem Analysis

### 1.1 Problem Statement
Modern enterprises run on complex distributed IT services—including relational databases, authentication microservices, cloud object stores, and payment gateways. When any component falters or halts, employees experience productivity blockages, while external customers experience transactional failures.

Manual triage and informal incident reporting (via emails, chat messages, or ad-hoc phone calls) cause several critical vulnerabilities:
1. **Delayed First-Response and Extended Mean Time to Resolution (MTTR)**: Without structured queues, critical incidents sit unnoticed in general inboxes.
2. **Incorrect and Inconsistent Prioritization**: Support staff evaluate severity subjectively rather than measuring objective business impact and urgency.
3. **Inefficient Support Engineer Allocation**: Tasks are assigned blindly without considering specialized technical skills or active technician workload.
4. **Service Level Agreement (SLA) Violations**: Failure to compute target response and resolution deadlines systematically leads to missed contract milestones.
5. **Duplicate Incident Clutter**: Multiple employees encountering the same underlying service failure file separate incidents, overwhelming the support team.
6. **Incomplete Audit Trails**: Lack of tamper-resistant change histories hinders post-mortem reviews and root-cause analysis.

### 1.2 Stakeholders
- **Employees / End-Users**: Need a transparent, frictionless portal to report interruptions, track ticket status in real time, add supplemental diagnostic notes, and provide quality feedback once resolved.
- **Support Engineers / Technicians**: Require a prioritized, skill-matched work queue, instant SLA breach warnings, structured investigation logging, and automated workload accounting.
- **IT Administrators / Service Owners**: Require high-level executive dashboards, service health matrices, technician capacity planning, configurable SLA policies, and comprehensive security audit trails.

### 1.3 Proposed Solution
The Intelligent IT Service Management System (I-ITSM) is an automated, object-oriented incident management platform designed around the complete incident lifecycle:
$$\text{Report} \longrightarrow \text{Classify} \longrightarrow \text{Prioritize} \longrightarrow \text{Assign} \longrightarrow \text{Investigate} \longrightarrow \text{Resolve} \longrightarrow \text{Verify} \longrightarrow \text{Close}$$

---

## 2. Requirements Specification

### 2.1 Functional Requirements
- **FR1 (Authentication & Access Control)**: JWT-based stateless authentication supporting role-based access control for `EMPLOYEE`, `TECHNICIAN`, and `ADMIN`.
- **FR2 (Incident Reporting & Auto-Classification)**: Users can create incidents with categorization (`HARDWARE`, `SOFTWARE`, `NETWORK`, `DATABASE`, `CLOUD`, `SECURITY`, `AUTHENTICATION`, `APPLICATION`, `STORAGE`).
- **FR3 (Dynamic Priority Determination)**: Priority ($P_1, P_2, P_3, P_4$) calculated from Impact $\times$ Urgency $\times$ Service Criticality using the **Strategy Pattern**.
- **FR4 (Intelligent Technician Assignment)**: Technicians assigned via either Skill-Based matching or Load-Based load balancing.
- **FR5 (SLA Policy Enforcement)**: Automated computation of response and resolution deadlines, tracking countdowns, and flagging breaches.
- **FR6 (State-Pattern Lifecycle)**: Enforces rigid transition rules to prevent invalid skips (e.g., cannot transition directly from `CLASSIFIED` to `RESOLVED` or `CLOSED`).
- **FR7 (Multi-Channel Notifications)**: Simulated notification engine dispatching in-app, email, and SMS alerts upon state milestones.
- **FR8 (Audit Trail Logging)**: Immutable chronological log capturing every action, modifying user, previous state, and new state.
- **FR9 (Intelligent Duplicate Detection)**: Lexical similarity engine comparing title and description tokens against open incidents using Jaccard word intersection.
- **FR10 (Executive Analytics)**: Real-time charts detailing priority distribution, service incident frequency, and technician workload.

### 2.2 Non-Functional Requirements
- **Extensibility**: Design patterns decouple business algorithms so that new assignment or prioritization rules can be added without modifying existing service classes (Open/Closed Principle).
- **Data Integrity**: Enforced through foreign keys, Jakarta Bean Validation constraints, and PostgreSQL ACID transactions (`@Transactional`).
- **Security**: Passwords encrypted via BCrypt with salt; stateless JWT verification on every authenticated endpoint.

---

## 3. Object-Oriented Architecture & Design Patterns

### 3.1 Design Patterns Implemented

#### 1. Strategy Pattern (`com.itsm.strategy`)
- **`PriorityCalculationStrategy`**: Decouples calculation from incident services.
  - `DefaultPriorityStrategy`: $\text{Score} = \text{Impact} \times \text{Urgency} \times \text{BusinessCriticality}$.
  - `WeightedPriorityStrategy`: $\text{Score} = 0.40 \times \text{Impact} + 0.35 \times \text{Urgency} + 0.25 \times \text{BusinessCriticality}$.
- **`AssignmentStrategy`**: Decouples technician assignment heuristics.
  - `SkillBasedAssignmentStrategy`: Matches technician skills to incident domain category, selecting the engineer with the lowest active workload among matches.
  - `LoadBasedAssignmentStrategy`: Minimizes maximum queue length across all available technicians.

#### 2. State Pattern (Incident Lifecycle Management)
- Defines valid transitions between states:
  - $\text{NEW} \rightarrow \text{CLASSIFIED}$
  - $\text{CLASSIFIED} \rightarrow \text{ASSIGNED}$
  - $\text{ASSIGNED} \rightarrow \text{ACKNOWLEDGED}$
  - $\text{ACKNOWLEDGED} \rightarrow \text{IN\_PROGRESS}$
  - $\text{IN\_PROGRESS} \rightarrow \text{RESOLVED}$
  - $\text{RESOLVED} \rightarrow \text{VERIFIED} \mid \text{CLOSED}$
  - $\text{VERIFIED} \rightarrow \text{CLOSED}$
- Disallowed transitions (e.g. `NEW` $\rightarrow$ `CLOSED`) trigger `InvalidStateTransitionException`.

#### 3. Observer Pattern (`com.itsm.service.NotificationService`)
- Decouples domain incident modifications from notification dissemination. Broadcasts events to reporters, technicians, and administrators across multiple delivery channels.

#### 4. Repository Pattern (`com.itsm.repository`)
- Spring Data JPA repositories abstract database querying, filtering, and aggregation away from application services.

---

## 4. Database Schema & Entity-Relationship Design

```
+--------------------+       +----------------------+       +-------------------------+
|      users         |       |    it_services       |       |      sla_policies       |
+--------------------+       +----------------------+       +-------------------------+
| id (PK)            |       | id (PK)              |       | id (PK)                 |
| username (UNIQUE)  |       | name (UNIQUE)        |       | priority (ENUM)         |
| password (HASH)    |       | description          |       | response_time_minutes   |
| email (UNIQUE)     |       | category             |       | resolution_time_minutes |
| full_name          |       | business_criticality |       | description             |
| role (ENUM)        |       | active               |       | active                  |
| department         |       | support_contact      |       +-------------------------+
| phone_number       |       | created_at           |
| active             |       +----------------------+
+--------------------+
         | 1
         |
         | *
+-------------------------------------------------------+
|                       incidents                       |
+-------------------------------------------------------+
| id (PK)                                               |
| incident_number (UNIQUE, e.g. INC1001)                |
| reporter_id (FK -> users.id)                          |
| assigned_technician_id (FK -> users.id, NULLABLE)     |
| affected_service_id (FK -> it_services.id, NULLABLE)  |
| sla_policy_id (FK -> sla_policies.id, NULLABLE)       |
| title                                                 |
| description                                           |
| category (ENUM)                                       |
| impact (ENUM)                                         |
| urgency (ENUM)                                        |
| priority (ENUM)                                       |
| status (ENUM)                                         |
| response_deadline (TIMESTAMP)                         |
| resolution_deadline (TIMESTAMP)                       |
| responded_at (TIMESTAMP)                              |
| resolved_at (TIMESTAMP)                               |
| closed_at (TIMESTAMP)                                 |
| resolution_notes (TEXT)                               |
| feedback (TEXT)                                       |
| feedback_rating (INT)                                 |
| created_at (TIMESTAMP)                                |
| updated_at (TIMESTAMP)                                |
+-------------------------------------------------------+
         | 1                                    | 1
         | *                                    | *
+--------------------------+          +--------------------------+
|      incident_notes      |          |        audit_logs        |
+--------------------------+          +--------------------------+
| id (PK)                  |          | id (PK)                  |
| incident_id (FK)         |          | incident_id (FK)         |
| author_id (FK -> users)  |          | performed_by_id (FK)     |
| content (TEXT)           |          | action                   |
| is_internal_note (BOOL)  |          | previous_value           |
| created_at               |          | new_value                |
+--------------------------+          | description              |
                                      | timestamp                |
                                      +--------------------------+
```

---

## 5. Automated Verification & Test Documentation (17 Test Cases)

| Test # | Test Method | Component | Condition / Target | Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC01** | `testDefaultPriority_Critical` | DefaultPriorityStrategy | Impact=High, Urgency=High, Criticality=5 yields P1_CRITICAL | **PASSED** |
| **TC02** | `testDefaultPriority_High` | DefaultPriorityStrategy | Impact=High, Urgency=Low, Criticality=4 yields P2_HIGH | **PASSED** |
| **TC03** | `testDefaultPriority_Low` | DefaultPriorityStrategy | Impact=Low, Urgency=Low, Criticality=1 yields P4_LOW | **PASSED** |
| **TC04** | `testWeightedPriority` | WeightedPriorityStrategy | Continuous weighted score validation | **PASSED** |
| **TC05** | `testSkillBasedAssignment_MatchFound` | SkillBasedAssignmentStrategy | Match technician skill to incident category | **PASSED** |
| **TC06** | `testSkillBasedAssignment_NoMatch` | SkillBasedAssignmentStrategy | No matching skills returns Optional.empty() | **PASSED** |
| **TC07** | `testSkillBasedAssignment_WorkloadTieBreak` | SkillBasedAssignmentStrategy | Multiple matches resolved by selecting lower workload | **PASSED** |
| **TC08** | `testLoadBasedAssignment` | LoadBasedAssignmentStrategy | Selects technician with least active backlog | **PASSED** |
| **TC09** | `testCreateIncident` | IncidentService | Auto-classification, SLA deadline computation, notification dispatch | **PASSED** |
| **TC10** | `testInvalidStateTransition` | IncidentService | Reject invalid transition (`CLASSIFIED` $\rightarrow$ `RESOLVED`) | **PASSED** |
| **TC11** | `testAcknowledgeIncident` | IncidentService | Sets `respondedAt` timestamp and state `ACKNOWLEDGED` | **PASSED** |
| **TC12** | `testResolveIncident` | IncidentService | Sets `resolvedAt`, logs notes, and decrements active technician load | **PASSED** |
| **TC13** | `testCloseIncident` | IncidentService | Sets `closedAt` timestamp and state `CLOSED` | **PASSED** |
| **TC14** | `testDuplicateDetection_PositiveMatch` | DuplicateDetectionService | High Jaccard similarity between similar descriptions | **PASSED** |
| **TC15** | `testDuplicateDetection_NoMatch` | DuplicateDetectionService | Distinguishes between completely unrelated problems | **PASSED** |
| **TC16** | `testAddFeedback` | IncidentService | Captures 5-star ratings and textual feedback post-resolution | **PASSED** |
| **TC17** | `testResourceNotFound` | IncidentService | Throws `ResourceNotFoundException` on missing incident ID | **PASSED** |

---

## 6. Project Demonstration Script (Workflows for Video Presentation)

For your recorded demonstration (15–20 mins), follow these 3 complete end-to-end workflows:

### Workflow 1: Employee Reporting & Automated Intelligent Assignment
1. Login with **Employee** credentials (`emp_john` / `emp123`).
2. Navigate to **Report Incident**.
3. Select service **"Payment Gateway"**, category **APPLICATION**, Impact **HIGH**, Urgency **HIGH**.
4. Submit the ticket $\rightarrow$ System calculates priority as **P1 CRITICAL**, determines SLA targets, sets status to **CLASSIFIED**, and logs the audit event.
5. In the ticket details, click **"Auto-Assign (Skill Match)"** $\rightarrow$ Strategy engine matches `tech_app` (specialist in Applications/Software) and updates state to **ASSIGNED**.

### Workflow 2: Technician Investigation & SLA Compliance
1. Switch account to **Technician** (`tech_app` / `tech123`).
2. Open **"My Assigned Queue"** $\rightarrow$ Locate the newly assigned P1 incident.
3. Click **"Acknowledge Incident"** $\rightarrow$ Response timestamp recorded, meeting initial response SLA target.
4. Click **"Start Investigation"** $\rightarrow$ State transitions to **IN_PROGRESS**.
5. Add internal diagnostic notes in the incident investigation log.
6. Click **"Resolve Incident"**, enter resolution details $\rightarrow$ Status changes to **RESOLVED**, resolution timestamp logged, technician active load decremented.

### Workflow 3: Resolution Verification, Feedback & Duplicate Detection
1. Switch back to **Employee** (`emp_john`).
2. View the resolved incident $\rightarrow$ Click **"Verify"** and then **"Close Incident"**.
3. Submit a 5-star rating and feedback comment.
4. Now, go to **Report Incident** and start typing a duplicate title: *"Payment application unavailable during checkout"*.
5. Observe the **Intelligent Duplicate Detection Warning** instantly alert the user with the existing ticket number to avoid duplicate clutter!

---

## 7. Chatbot & Frustration Detection

### Problem, design and OO pattern mapping

The new assistant helps three JWT-authenticated roles with real incident information while letting employees report incidents without learning the incident-management API. Report drafting runs through persisted dialogue states `IDLE → AWAIT_SERVICE → AWAIT_DESCRIPTION → AWAIT_IMPACT → AWAIT_CONFIRM → IDLE`; creation occurs **only** on the single-use confirmation turn via the existing `IncidentService`. It therefore inherits classification, priority calculation, SLA rules, audit, notifications and auto-assignment. A second confirmation in `IDLE` is inert. A `REQUEST_HUMAN` intent similarly requires confirmation before a support-request note is recorded; there is no pretend live human operator.

`ChatEngine` and `ChatEngineRegistry` reuse the existing **Strategy** abstraction. `RuleBasedChatEngine` is the no-key default; `LlmChatEngine` is a **Decorator** that delegates report orchestration to the same state handler and only enriches JSON extraction / generic phrasing. `GeminiLlmClient`, `AnthropicLlmClient` and `LlmTransport` are **Adapters** around one-attempt HTTP request/response, with header authentication and timeout fallback. Fixed prompts treat masked user text as untrusted data. User-visible text is escaped naturally by React (not inserted as HTML). Per-message data and session state live in `ChatMessage` and `ChatSession` JPA entities, with a unique user-session constraint and 200-message retention.

Frustration detection uses **Strategy** (`FrustrationSignal` with `TextFrustrationSignal`, `BehaviourFrustrationSignal`, optional low-weight `LlmSentimentSignal`) and immutable `SignalResult` / `FrustrationAssessment` records. The detector computes a 0–100 score with time decay (12-hour half-life), smoothing (35% decayed history + 65% latest evidence), small sustained-frustration bonus, and thresholds **25 / 55 / 85** for ANNOYED / FRUSTRATED / CRITICAL. Example: `still not fixed!!` ≈ 38 (ANNOYED) → `THIRD TIME this week, useless` ≈ 67 (FRUSTRATED) → explicit manager demand ≥ 85 (CRITICAL). Direct manager demands alone reach FRUSTRATED but do not instantly become CRITICAL. Technical failures such as `the database server is not responding` are neutral absent service-directed anger. The assessment is a **heuristic, not a diagnosis**; false positives are possible.

On a rising level at FRUSTRATED or higher, an **Observer** event `FrustrationDetectedEvent` triggers `FrustrationEventListener`, which writes `FrustrationRecord`, raises the staff-only incident flag, logs an audit and publishes an existing `IncidentEvent` for `NotificationService`. The assignee receives in-app notification; at CRITICAL, admins receive in-app + email + SMS Outbox records. Optional SMTP uses `JavaMailSender`, but Outbox persistence is independent of actual delivery. `FrustrationState` tracks per-user/incident smoothing, cooldown and dismissals. `IncidentAccessPolicy` applies to the chatbot and staff-only data routes; regular employee incident DTOs and chat history contain no frustration scores or levels. The admin dashboard uses Asia/Kolkata dates for 14-day trends; staff can dismiss flags with an audited endpoint. Scheduled rechecks examine a bounded batch.

### Key classes and endpoints

| Layer | Classes / routes |
|---|---|
| Chat intents and states | `IntentClassifier`, `ChatState`, `ChatService`, `ChatSession`, `ChatMessage` |
| Engines / LLM | `ChatEngine`, `RuleBasedChatEngine`, `LlmChatEngine`, `GeminiLlmClient`, `AnthropicLlmClient`, `LlmTransport` |
| Chat HTTP | `POST /api/chat/message`, `GET /api/chat/history`, `POST /api/chat/reset` |
| Frustration strategies | `FrustrationDetector`, `TextFrustrationSignal`, `BehaviourFrustrationSignal`, `LlmSentimentSignal`, `ProfanitySanitizer` |
| Frustration persistence and observer | `FrustrationState`, `FrustrationRecord`, `FrustrationDetectedEvent`, `FrustrationEventListener` |
| Staff HTTP | `POST /api/incidents/{id}/frustration/dismiss`, `GET /api/dashboard/frustration`, `GET /api/admin/frustration-config` |
| Search | Existing `GET /api/incidents` gains staff-only `frustration` and `frustratedFirst` parameters |
| React | `ChatWidget`, `FrustrationBadge`, `IncidentRows`, `IncidentDetail`, `IncidentList`, `MyQueue`, `Home`, `AdminAutomation` |

### Configuration and privacy

`CHATBOT_ENGINE` defaults to `rule`; `CHATBOT_LLM_PROVIDER` selects `gemini` or `anthropic` when set to `llm`. Optional keys and model IDs are passed from `.env` through Docker environment into `application.properties`. `FRUSTRATION_ENABLED`, `FRUSTRATION_ANNOYED`, `FRUSTRATION_FRUSTRATED`, `FRUSTRATION_CRITICAL`, `FRUSTRATION_DECAY_HOURS`, `FRUSTRATION_COOLDOWN_MINUTES`, `FRUSTRATION_RECHECK_INTERVAL_MS`, `FRUSTRATION_REPEAT_WINDOW_DAYS`, `FRUSTRATION_LLM_SENTIMENT` and `FRUSTRATION_AUTO_ESCALATE` control heuristic analysis. Auto-escalation and LLM sentiment are off by default. `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH` and `MAIL_STARTTLS` control optional SMTP. The Automation screen displays frustration settings read-only; env changes require restart. Raw profanity is masked before persistence or outbound LLM calls. API keys are never put in URLs or logged.

### Added JUnit test cases (definitions; execution must be verified separately)

| # | Test case | Kind | Expected result | Test class |
|---|---|---|---|---|
| TC18 | `technicalFailuresDoNotBecomePersonalFrustration` | Normal / false positive | Database failure stays CALM | `FrustrationDetectorTests` |
| TC19 | `escalatingDemoLadderReachesCriticalInThreeTurns` | Business rule | Levels ANNOYED → FRUSTRATED → CRITICAL | `FrustrationDetectorTests` |
| TC20 | `singleAllCapsOutburstCannotBeCritical` | Boundary | Single angry message stays below CRITICAL | `FrustrationDetectorTests` |
| TC21 | `previousFrustrationDecaysAfterCalmTime` | Time / decay | Calm activity lowers score after simulated time | `FrustrationDetectorTests` |
| TC22 | `profanityIsMaskedBeforeStorage` | Invalid input | Profanity replaced by `[redacted]` | `FrustrationDetectorTests` |
| TC23 | `intentsMatchPatterns` | Normal | Rule patterns classify role-related intents | `IntentClassifierTests` |
| TC24 | `reportFlowCreatesOnlyAfterConfirmationAndDoubleConfirmIsSafe` | Business rule | Exactly one incident create after confirmed transition | `ChatServiceTests` |
| TC25 | `employeeCannotLearnAnythingAboutAnotherReporterIncident` | Authorisation | Generic refusal, no private details | `ChatServiceTests` |
| TC26 | `malformedJsonFallsBackToRules` | Invalid provider data | Local rule output remains available | `LlmFallbackTests` |
| TC27 | `timeoutFallsBackWithoutPropagatingException` | Exception | Provider timeout does not break chat extraction | `LlmFallbackTests` |

**Verification caveat:** The source archive's Maven parent dependency could not be fetched in this environment. New and existing JUnit tests are included, but execution must be confirmed on a machine with Maven Central access. The frontend build was run locally. Schema updates use Hibernate's `ddl-auto=update`; reset disposable old volumes with care because new enum literals may conflict with historical CHECK constraints.
