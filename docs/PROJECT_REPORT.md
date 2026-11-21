# Intelligent IT Service Management & Incident Resolution System (I-ITSM)
## Comprehensive Academic Project Report & Defense Document

**Course:** CS F213 / MAC F212 — Object-Oriented Programming (Semester-I, 2026-2027)  
**Project Title:** Intelligent IT Service Management & Incident Resolution System (I-ITSM)  
**Submission Date:** 22.11.2026  
**Source Code Repository:** [https://github.com/suryansh0267/oops-project](https://github.com/suryansh0267/oops-project)  
**Technology Stack:** Java 21, Spring Boot 4, PostgreSQL 18, React 19, Tailwind CSS v4, Vite, Docker  

---

## Team Members & Identifiable Responsibilities

> As required by the course guidelines, this project was developed by a team of 4 members with distinct, modular software engineering ownership across the full application lifecycle.

| Member Name | Student ID | Core Functional Ownership | Specific Modules & Key Contributions |
| :--- | :--- | :--- | :--- |
| **[Student Name 1]** | `[202XAXPSXXXXP]` | **UI / UX & Frontend Architecture** | Designed and developed the React 19 Single Page Application with Tailwind CSS v4 and Vite. Built interactive dashboards, ticket lifecycle views, role-based interfaces (Employee, Technician, Admin), and the real-time `ChatWidget` with accessible keyboard navigation and Web Speech voice recognition. |
| **[Student Name 2]** | `[202XAXPSXXXXP]` | **Backend Architecture & REST APIs** | Developed the Spring Boot 4 application layer, RESTful controller endpoints (`IncidentController`, `AuthController`, `AdminController`, `TechnicianController`), JWT stateless security filter chain, BCrypt password hashing, and RFC-7807 compliant `GlobalExceptionHandler`. |
| **[Student Name 3]** | `[202XAXPSXXXXP]` | **Domain Model & OOP Design Patterns** | Engineered core object-oriented architectures: State Pattern for incident lifecycle transitions (`IncidentState` hierarchy), Strategy Pattern for assignment (`SkillBased`, `LoadBased`, `RoundRobin`, `SmartScore`) and priority calculation, and Observer Pattern for event-driven decoupled notifications. |
| **[Student Name 4]** | `[202XAXPSXXXXP]` | **Database, Persistence & Intelligent Features** | Designed the normalized PostgreSQL 18 relational schema, Spring Data JPA repositories, continuous SLA background monitor (`SlaMonitor`), automated hierarchy escalation, Jaccard lexical duplicate detection, and heuristic customer frustration detection engine. |

---

## Executive Summary & Compliance Verification

| Rubric Requirement | Prescribed Minimum | Project Implementation | Status |
| :--- | :--- | :--- | :--- |
| **User Roles** | 3 roles (Employee, Tech, Admin) | `EMPLOYEE`, `TECHNICIAN`, `ADMIN` with custom role dashboards & ACLs | **FULLY MET (100%)** |
| **Incident Lifecycle** | Report → Verify → Close | 8-stage State Pattern: `NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED` | **FULLY MET (100%)** |
| **Priority Determination** | Impact $\times$ Urgency $\times$ Criticality | Multi-factor mathematical scoring with pluggable `PriorityCalculationStrategy` ($P_1$ to $P_4$) | **FULLY MET (100%)** |
| **Technician Assignment** | At least 2 strategies | 4 Strategies: `SkillBased`, `LoadBased`, `RoundRobin`, and `SmartScore` | **EXCEEDED (4 implemented)** |
| **SLA Management** | Calculation, Deadlines, Violations | Dynamic deadline computation, active countdown timers, scheduled `SlaMonitor`, and automated manager escalation | **FULLY MET (100%)** |
| **Notification System** | At least 2 channels | 3 Channels: In-App notification center, real SMTP Email (`JavaMailSender`), and SMS gateway outbox | **EXCEEDED (3 implemented)** |
| **Audit Trail** | Complete incident history | Immutable `audit_logs` tracking timestamp, actor, action, previous state, new state, and metadata | **FULLY MET (100%)** |
| **Search & Filtering** | At least 5 criteria | 9 criteria: ID, Status, Priority, Category, Service, Technician, Date range, SLA status, Frustration | **EXCEEDED (9 criteria)** |
| **Intelligent Features** | At least 2 automated features | 8 Features: Duplicate detection, Smart technician scoring, Category suggestion, SLA breach auto-escalation, Resolution retrieval, Frustration analysis, IT Assistant bot, Voice reporting | **EXCEEDED (8 implemented)** |
| **Design Patterns** | At least 2 patterns | 6 Patterns: Strategy, State, Observer, Repository, Factory, Adapter/Decorator | **EXCEEDED (6 implemented)** |
| **Testing** | At least 15 test cases | 27 automated test cases documented and verified across unit and integration suites | **EXCEEDED (27 test cases)** |
| **Deliverables & Demo** | Code repo, report, 3 workflows | GitHub repository pushed, full report generated, 3 step-by-step video scripts prepared | **FULLY MET (100%)** |

---

## 1. Problem Analysis

### 1.1 Problem Statement
Modern enterprises rely on interconnected digital services—including payment gateways, relational database clusters, Single Sign-On (SSO) identity providers, and cloud document repositories. When unexpected interruptions occur, uncoordinated manual handling leads to:
1. **Severe Communication Gaps**: Incident reports get lost in general inboxes or chat channels without structured tickets.
2. **Subjective Prioritization**: Critical outages are misclassified as low priority, delaying vital operational triage.
3. **Technician Fatigue & Imbalance**: Incidents are dumped onto single individuals regardless of active queue length or technical specialization.
4. **Contractual SLA Breaches**: Lack of real-time deadline monitoring results in contractual fines and client distrust.
5. **Duplicate Clutter**: Multiple employees submit separate reports for the same underlying failure, fragmenting response efforts.
6. **Zero Accountability**: Absence of immutable audit trails hinders post-incident reviews and compliance audits.

### 1.2 Stakeholders
- **Employees / End-Users**: Require a frictionless interface to report failures, monitor status live, provide clarification notes, and verify resolutions.
- **Support Engineers / Technicians**: Require a skill-matched ticket backlog, clear SLA countdowns, structured diagnostic notes, and automated capacity limits.
- **IT Administrators / Service Owners**: Require high-level visibility into service health, technician workload balancing, configurable SLA rules, audit compliance, and outbox logs.

### 1.3 Proposed Solution: I-ITSM
The Intelligent IT Service Management System (I-ITSM) provides an automated, enterprise-grade, object-oriented incident management solution enforcing the complete industrial lifecycle:
$$\text{Report} \longrightarrow \text{Classify} \longrightarrow \text{Prioritize} \longrightarrow \text{Assign} \longrightarrow \text{Investigate} \longrightarrow \text{Resolve} \longrightarrow \text{Verify} \longrightarrow \text{Close}$$

---

## 2. Requirements Specification

### 2.1 Functional Requirements
- **FR1 (Stateless Authentication & Role-Based Authorization)**: Secure login and registration using BCrypt password encryption and stateless JSON Web Tokens (JWT) enforcing boundaries across `EMPLOYEE`, `TECHNICIAN`, and `ADMIN`.
- **FR2 (Automated Classification & Service Association)**: Classifies tickets into 10 enterprise categories (`HARDWARE`, `SOFTWARE`, `NETWORK`, `DATABASE`, `CLOUD`, `SECURITY`, `AUTHENTICATION`, `APPLICATION`, `STORAGE`, `OTHER`) linked to affected IT services.
- **FR3 (Dynamic Mathematical Prioritization)**: Automatically computes incident priority using impact, urgency, and service business criticality via interchangeable strategies.
- **FR4 (Intelligent Assignment Engine)**: Assigns tickets based on skill specialization and live technician queue load balancing.
- **FR5 (SLA Deadline & Escalation Engine)**: Computes response and resolution deadlines from SLA matrices, triggers warnings before deadlines, records SLA breach violations, and automatically escalates neglected P1 incidents.
- **FR6 (State-Pattern Lifecycle Protection)**: Enforces finite state transitions, completely rejecting invalid operations (e.g., transitioning directly from `NEW` to `CLOSED`).
- **FR7 (Multi-Channel Event Notifications)**: Broadcasts real-time events through an In-App notification drawer, real SMTP emails via Gmail integration, and SMS delivery outboxes.
- **FR8 (Immutable Audit Logging)**: Records every state transition, field change, user timestamp, and diagnostic update.
- **FR9 (Lexical Duplicate Incident Detection)**: Computes text similarity on title/description tokens to alert users before redundant tickets are filed.
- **FR10 (AI-Powered IT Assistant Chatbot)**: Conversational assistant providing natural language ticket reporting, troubleshooting check suggestions, queue checks, and human handoff requests.

### 2.2 Non-Functional Requirements
- **Maintainability & Extensibility**: High cohesion and loose coupling through interfaces and design patterns (SOLID principles).
- **Data Integrity & Consistency**: Enforced using relational foreign keys, Jakarta Bean Validation constraints, and ACID `@Transactional` demarcations.
- **Security & Privacy**: Protection against unauthorized operations using Spring Security Method Security (`@PreAuthorize`) and `IncidentAccessPolicy` ensuring employees cannot view or tamper with other reporters' tickets.
- **Performance & Responsiveness**: Sub-50ms API response times backed by indexed PostgreSQL database queries.

---

## 3. Object-Oriented Architecture & Design Patterns

The I-ITSM backend architecture demonstrates comprehensive application of modern software engineering principles and object-oriented paradigms.

```
+-----------------------------------------------------------------------------------+
|                                PRESENTATION TIER                                  |
|   React 19 SPA + Vite + Tailwind CSS v4 + Lucide Icons + Native Speech Voice API   |
+-----------------------------------------------------------------------------------+
                                         │  HTTP / RESTful JSON (Stateless JWT)
                                         ▼
+-----------------------------------------------------------------------------------+
|                                CONTROLLER LAYER                                   |
|   AuthController  |  IncidentController  |  TechnicianController  |  ChatController|
|   AdminController |  DashboardController |  NotificationController                 |
+-----------------------------------------------------------------------------------+
                                         │  DTOs & Jakarta Validated Requests
                                         ▼
+-----------------------------------------------------------------------------------+
|                                 SERVICE LAYER                                     |
|   IncidentService ──────► SlaService ──────► NotificationService ──────► AuditSvc |
|         │                        │                     │                          |
|         ▼                        ▼                     ▼                          |
|  State Pattern             Strategy Pattern     Observer Pattern                  |
|  (8 Concrete States)      (Priority & Assign)  (IncidentEvents)                   |
+-----------------------------------------------------------------------------------+
                                         │  Domain Entities (JPA / Hibernate)
                                         ▼
+-----------------------------------------------------------------------------------+
|                               DATA ACCESS LAYER                                   |
|   IncidentRepository | UserRepository | SlaPolicyRepository | AuditLogRepository  |
|   ITServiceRepository| ChatSessionRepo| FrustrationStateRepo| SlaViolationRepo    |
+-----------------------------------------------------------------------------------+
                                         │  SQL / Connection Pool
                                         ▼
+-----------------------------------------------------------------------------------+
|                               DATABASE STORAGE                                    |
|                      PostgreSQL 18 Relational Database                            |
+-----------------------------------------------------------------------------------+
```

### 3.1 Design Patterns Implemented

#### 1. Strategy Pattern (`com.itsm.strategy`)
- **Priority Calculation (`PriorityCalculationStrategy`)**:
  - `DefaultPriorityStrategy`: $\text{Score} = \text{Impact} \times \text{Urgency} \times \text{BusinessCriticality}$
  - `WeightedPriorityStrategy`: $\text{Score} = 0.40 \times \text{Impact} + 0.35 \times \text{Urgency} + 0.25 \times \text{BusinessCriticality}$
- **Technician Assignment (`AssignmentStrategy`)**:
  - `SkillBasedAssignmentStrategy`: Matches technician skill set to incident category; breaks ties by choosing the technician with the least active backlog.
  - `LoadBasedAssignmentStrategy`: Distributes work evenly by minimizing maximum active load.
  - `RoundRobinAssignmentStrategy`: Cyclic deterministic distribution.
  - `SmartScoreAssignmentStrategy`: Multi-variable optimization combining workload, skills, and historical resolution speed.
- **Chatbot Strategies (`ChatEngine`)**:
  - `RuleBasedChatEngine`: 100% offline, deterministic rule-based processing.
  - `LlmChatEngine`: Pluggable LLM strategy delegating to Google Gemini or Anthropic Claude.

#### 2. State Pattern (`com.itsm.state`)
The incident lifecycle is encapsulated in dedicated state objects inheriting from `AbstractIncidentState`:
- `NewState`: Incident created, awaiting classification.
- `ClassifiedState`: Categorized and prioritized, ready for technician assignment.
- `AssignedState`: Technician designated, waiting for acknowledgment.
- `AcknowledgedState`: Technician has accepted ticket; initial response SLA satisfied.
- `InProgressState`: Active investigation and diagnostic note logging.
- `ResolvedState`: Solution documented; awaiting employee validation.
- `VerifiedState`: Employee confirms resolution.
- `ClosedState`: Incident finalized; feedback collected.
- **Guaranteed Invariant**: Inappropriate transitions (e.g. `NEW → CLOSED` or `CLASSIFIED → RESOLVED`) are intercepted and throw `InvalidStateTransitionException`.

#### 3. Observer Pattern (`com.itsm.event`, `com.itsm.service.NotificationService`)
- Decouples domain incident modifications from notification and audit logging.
- When state changes occur, `ApplicationEventPublisher` publishes `IncidentEvent`.
- `NotificationEventListener` captures the event asynchronously and delegates to `NotificationChannel` implementations (`InAppNotificationChannel`, `EmailNotificationChannel`, `SmsNotificationChannel`).
- `FrustrationEventListener` listens for `FrustrationDetectedEvent` and triggers staff alerts.

#### 4. Repository Pattern (`com.itsm.repository`)
- All persistence logic is completely decoupled from business operations using Spring Data JPA repositories with dynamic type-safe `IncidentSpecifications`.

#### 5. Factory Pattern (`com.itsm.state.IncidentStateFactory`)
- Centralized creation and state mapping from database `IncidentStatus` enums to concrete state behaviors.

#### 6. Adapter & Decorator Patterns (`com.itsm.chatbot`)
- `GeminiLlmClient` and `AnthropicLlmClient` adapt external REST LLM APIs into a unified `LlmClient` interface.
- `LlmChatEngine` decorates `RuleBasedChatEngine` to enrich drafts while retaining local rule safety.

---

## 4. Technology Selection Report

As mandated by the project specification, this section formally defends each architectural and technological choice against alternatives.

### 1. Why Java 21?
Java 21 delivers enterprise-grade type safety, mature object-oriented primitives (records, pattern matching, sealed classes), and high performance through the modern HotSpot JVM. It offers strict compile-time verification, making it the industry standard for mission-critical banking and enterprise systems where domain models must be robust and error-free.

### 2. Why React 19 + Tailwind CSS + Vite?
- **React 19**: Component-based UI architecture aligns with OOP thinking. Its reactive virtual DOM guarantees fluid user interactions without page reloads.
- **Tailwind CSS v4**: Utility-first CSS eliminates stylesheet sprawl and ensures consistent high-contrast, accessible visual design across light and dark modes.
- **Vite**: Modern native-ESM build tool delivering sub-second hot module replacement (HMR) and optimized production bundles.

### 3. Why Spring Boot 4?
Spring Boot provides the definitive enterprise backbone for Java applications. It standardizes dependency injection (Inversion of Control), declarative transaction management (`@Transactional`), comprehensive security filter chains, and seamless database integration through Spring Data JPA and Hibernate.

### 4. Why PostgreSQL 18?
Incident management requires strict ACID guarantees. An incident cannot be assigned to a technician while simultaneously being closed or deleted. PostgreSQL provides robust relational integrity, foreign key constraints, JSONB support for flexible metadata, and high-performance B-tree indexing.

### 5. Why a Layered 3-Tier Architecture?
The layered architecture (Presentation → Application/Business Logic → Persistence) strictly separates concerns:
- Changes to the frontend do not affect business algorithms.
- Changes to database schemas do not break client contracts (mediated by DTOs).
- Business logic is completely isolated and unit-testable without spinning up web servers.

### 6. What alternatives were considered?
- **Backend**: Node.js/Express and Python/Django were considered. While fast for prototyping, dynamic typing lacks the compile-time safety and strict interface enforcement required for complex OOP pattern evaluation.
- **Frontend**: Plain HTML/JavaScript and Angular were evaluated. Angular proved overly heavy, while plain HTML lacked modular component reusability.
- **Database**: MongoDB (NoSQL) was evaluated but rejected because incident management is inherently relational: users report incidents, technicians resolve incidents, SLAs govern incidents, and audit trails log incidents.

### 7. Advantages and Limitations of the Technology Stack
- **Advantages**: Compile-time safety, zero runtime reflection overhead on records, rock-solid ACID transactions, decoupled design patterns, and clean separation of concerns.
- **Limitations**: Higher initial memory footprint compared to Go or Rust; JVM startup time is longer than lightweight script engines.

### 8. How does the technology support Object-Oriented Design?
Java 21 provides native first-class support for encapsulation (`private` fields, access modifiers), abstraction (`interfaces`, `abstract classes`), polymorphism (dynamic method dispatch), and inheritance. Spring's dependency injection naturally encourages programming to interfaces rather than concrete implementations.

### 9. How would the system evolve as user count scales to millions?
1. **Stateless Clustering**: The backend uses stateless JWT authentication; instances can scale horizontally behind an Nginx or AWS Application Load Balancer.
2. **Database Sharding & Read Replicas**: PostgreSQL read replicas can handle high-volume reporting and analytics queries, while master nodes handle writes.
3. **Message Queuing**: Replace Spring's in-memory event publisher with Apache Kafka or RabbitMQ for distributed event streaming across decoupled worker nodes.
4. **Distributed Caching**: Introduce Redis for caching technician availability, SLA policy definitions, and active user sessions.

---

## 5. Database Design & Entity-Relationship Schema

The database schema is fully normalized into Third Normal Form (3NF) to eliminate redundancy and maintain relational integrity.

```
+--------------------+       +----------------------+       +-------------------------+
|      users         |       |    it_services       |       |      sla_policies       |
+--------------------+       +----------------------+       +-------------------------+
| id (PK)            |       | id (PK)              |       | id (PK)                 |
| username (UNIQUE)  |       | name (UNIQUE)        |       | priority (ENUM)         |
| password (HASH)    |       | description          |       | response_time_minutes   |
| email (UNIQUE)     |       | category             |       | resolution_time_minutes |
| full_name          |       | business_criticality |       | description             |
| role (ENUM)        |       | active (BOOL)        |       | active (BOOL)           |
| department         |       | support_contact      |       +-------------------------+
| phone_number       |       | created_at           |
| active (BOOL)      |       +----------------------+
+--------------------+
         │ 1                            │ 1                             │ 1
         │                              │                               │
         │ *                            │ *                             │ *
+─────────────────────────────────────────────────────────────────────────────────────+
|                                     incidents                                       |
+─────────────────────────────────────────────────────────────────────────────────────+
| id (PK)                                                                             |
| incident_number (UNIQUE, e.g. INC1001)                                              |
| reporter_id (FK -> users.id)                                                        |
| assigned_technician_id (FK -> users.id, NULLABLE)                                   |
| affected_service_id (FK -> it_services.id, NULLABLE)                                |
| sla_policy_id (FK -> sla_policies.id, NULLABLE)                                     |
| title (VARCHAR 200)                                                                 |
| description (TEXT)                                                                  |
| category (ENUM)                                                                     |
| impact (ENUM: HIGH, MEDIUM, LOW)                                                    |
| urgency (ENUM: HIGH, MEDIUM, LOW)                                                   |
| priority (ENUM: P1_CRITICAL, P2_HIGH, P3_MEDIUM, P4_LOW)                            |
| status (ENUM: NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, ...)  |
| response_deadline (TIMESTAMP)                                                       |
| resolution_deadline (TIMESTAMP)                                                     |
| responded_at (TIMESTAMP)                                                            |
| resolved_at (TIMESTAMP)                                                             |
| closed_at (TIMESTAMP)                                                               |
| resolution_notes (TEXT)                                                             |
| feedback (TEXT)                                                                     |
| feedback_rating (INT)                                                               |
| created_at (TIMESTAMP)                                                              |
| updated_at (TIMESTAMP)                                                              |
+─────────────────────────────────────────────────────────────────────────────────────+
         │ 1                     │ 1                           │ 1
         │ *                     │ *                           │ *
+────────────────────+  +────────────────────+       +────────────────────────────────+
|   incident_notes   |  |     audit_logs     |       |         sla_violations         |
+────────────────────+  +────────────────────+       +────────────────────────────────+
| id (PK)            |  | id (PK)            |       | id (PK)                        |
| incident_id (FK)   |  | incident_id (FK)   |       | incident_id (FK)               |
| author_id (FK)     |  | performed_by_id(FK)|       | violation_type (ENUM)          |
| content (TEXT)     |  | action (VARCHAR)   |       | target_deadline (TIMESTAMP)    |
| is_internal (BOOL) |  | previous_state     |       | actual_time (TIMESTAMP)        |
| created_at         |  | new_state          |       | breach_duration_minutes (INT)  |
+────────────────────+  | description        |       | recorded_at (TIMESTAMP)        |
                        | timestamp          |       +────────────────────────────────+
                        +────────────────────+

+────────────────────────────────────────────+       +────────────────────────────────+
|             chat_sessions                  |       |          chat_messages         |
+────────────────────────────────────────────+       +────────────────────────────────+
| id (PK)                                    | 1   * | id (PK)                        |
| user_id (FK -> users.id, UNIQUE)           ├───────┤ session_id (FK -> chat_sessions)
| state (ENUM: IDLE, AWAIT_SERVICE, ...)     |       | from_user (BOOL)               |
| draft_title, draft_description             |       | text (VARCHAR 3000)            |
| draft_category, draft_service_id           |       | intent (VARCHAR)               |
| draft_impact, draft_urgency                |       | message_frustration_score      |
| active_incident_id                         |       | incident_id (NULLABLE)         |
| updated_at                                 |       | created_at                     |
+────────────────────────────────────────────+       +────────────────────────────────+
```

---

## 6. Comprehensive Automated Testing Documentation (27 Test Cases)

The project includes an extensive test suite verifying business logic, edge conditions, security rules, and error handling.

| Test # | Test Case Method | Target Component | Category | Expected Condition & Assertion | Result |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC01** | `testDefaultPriority_Critical` | `DefaultPriorityStrategy` | Normal Operation | High Impact $\times$ High Urgency $\times$ 5 Criticality evaluates to $P_1$ Critical | **PASSED** |
| **TC02** | `testDefaultPriority_High` | `DefaultPriorityStrategy` | Normal Operation | High Impact $\times$ Low Urgency $\times$ 4 Criticality evaluates to $P_2$ High | **PASSED** |
| **TC03** | `testDefaultPriority_Low` | `DefaultPriorityStrategy` | Boundary Condition | Low Impact $\times$ Low Urgency $\times$ 1 Criticality evaluates to $P_4$ Low | **PASSED** |
| **TC04** | `testWeightedPriority` | `WeightedPriorityStrategy` | Normal Operation | Validates continuous floating point linear scoring matrix | **PASSED** |
| **TC05** | `testSkillBasedAssignment_MatchFound` | `SkillBasedAssignmentStrategy` | Business Rule | Matches engineer specializing in the exact category of the ticket | **PASSED** |
| **TC06** | `testSkillBasedAssignment_NoMatch` | `SkillBasedAssignmentStrategy` | Boundary Condition | Returns `Optional.empty()` safely when no engineer holds required skills | **PASSED** |
| **TC07** | `testSkillBasedAssignment_WorkloadTieBreak` | `SkillBasedAssignmentStrategy` | Business Rule | Breaks ties between qualified engineers by picking the lower queue load | **PASSED** |
| **TC08** | `testLoadBasedAssignment` | `LoadBasedAssignmentStrategy` | Business Rule | Allocates incident to technician with lowest active workload | **PASSED** |
| **TC09** | `testCreateIncident` | `IncidentService` | Normal Operation | Full pipeline: categorization, SLA computation, and notification publishing | **PASSED** |
| **TC10** | `testInvalidStateTransition` | `IncidentService` | Invalid State Skipping | Throws `InvalidStateTransitionException` when attempting `CLASSIFIED → RESOLVED` | **PASSED** |
| **TC11** | `testAcknowledgeIncident` | `IncidentService` | Normal Operation | Records response timestamp, satisfies initial response SLA, transitions state | **PASSED** |
| **TC12** | `testResolveIncident` | `IncidentService` | Normal Operation | Sets resolution timestamp, appends notes, decrements active technician count | **PASSED** |
| **TC13** | `testCloseIncident` | `IncidentService` | Normal Operation | Sets closed timestamp, marks ticket finalized, allows feedback rating | **PASSED** |
| **TC14** | `testDuplicateDetection_PositiveMatch` | `DuplicateDetectionService` | Intelligent Feature | Identifies high lexical Jaccard similarity across identical issue descriptions | **PASSED** |
| **TC15** | `testDuplicateDetection_NoMatch` | `DuplicateDetectionService` | Intelligent Feature | Correctly discriminates completely distinct infrastructure reports | **PASSED** |
| **TC16** | `testAddFeedback` | `IncidentService` | Normal Operation | Stores 1-to-5 star rating and post-resolution employee comments | **PASSED** |
| **TC17** | `testResourceNotFound` | `IncidentService` | Exception Handling | Throws `ResourceNotFoundException` when accessing non-existent ticket ID | **PASSED** |
| **TC18** | `technicalFailuresDoNotBecomePersonalFrustration` | `FrustrationDetector` | False Positive Check | Generic technical failure ("DB server down") correctly stays at `CALM` | **PASSED** |
| **TC19** | `escalatingDemoLadderReachesCriticalInThreeTurns` | `FrustrationDetector` | Escalation Rule | Successive hostile inputs transition levels `ANNOYED → FRUSTRATED → CRITICAL` | **PASSED** |
| **TC20** | `singleAllCapsOutburstCannotBeCritical` | `FrustrationDetector` | Boundary Condition | A single angry all-caps outburst cannot prematurely jump to `CRITICAL` | **PASSED** |
| **TC21** | `previousFrustrationDecaysAfterCalmTime` | `FrustrationDetector` | Temporal Decay | Mathematical half-life decay reduces frustration score after calm period | **PASSED** |
| **TC22** | `profanityIsMaskedBeforeStorage` | `ProfanitySanitizer` | Security / Sanitization | Masked profanity replaced with `[redacted]` before persistence or LLM dispatch | **PASSED** |
| **TC23** | `intentsMatchPatterns` | `IntentClassifier` | Natural Language | Rule-based classifier accurately resolves all role-specific intents | **PASSED** |
| **TC24** | `reportFlowCreatesOnlyAfterConfirmation` | `ChatService` | State Machine / Safety | Creation turns are executed only upon explicit user confirmation | **PASSED** |
| **TC25** | `employeeCannotLearnAnythingAboutAnotherReporterIncident`| `ChatService` / `IncidentAccessPolicy` | Authorization / Security | Rejects unauthorized ticket lookups with zero leakage of private details | **PASSED** |
| **TC26** | `malformedJsonFallsBackToRules` | `LlmFallbackTests` | Fault Tolerance | Unparseable LLM output safely falls back to local deterministic rules | **PASSED** |
| **TC27** | `timeoutFallsBackWithoutPropagatingException` | `LlmFallbackTests` | Fault Tolerance | External LLM HTTP timeouts fail silently without crashing chat widget | **PASSED** |

---

## 7. Project Demonstration Scripts (3 Workflows for Video Presentation)

For the mandatory 15–20 minute video presentation, team members can execute these three end-to-end operational scenarios:

### Workflow 1: Employee Incident Reporting & Automated Intelligent Assignment
1. **Login**: Sign in as Employee (`emp_john` / `emp123`).
2. **Report**: Open **Report Incident**. Select service **"Payment Gateway"**, category **APPLICATION**, Impact **HIGH**, Urgency **HIGH**.
3. **Automated Prioritization**: Submit ticket $\rightarrow$ System computes priority as **P1 CRITICAL**, binds the 5m response / 30m resolution SLA, and logs state `CLASSIFIED`.
4. **Intelligent Assignment**: System automatically matches `tech_app` (specialist in Applications) with the lowest active load and moves state to `ASSIGNED`.
5. **Notification**: The assigned technician receives instant in-app alerts and an email notification.

### Workflow 2: Technician Investigation & SLA Compliance Lifecycle
1. **Login**: Switch account to Technician (`tech_app` / `tech123`).
2. **Queue**: Open **"My Assigned Queue"** $\rightarrow$ Locate the newly created P1 ticket.
3. **Acknowledgment**: Click **"Acknowledge Incident"** $\rightarrow$ Records response timestamp meeting initial response SLA; state advances to `ACKNOWLEDGED`.
4. **Investigation**: Click **"Start Investigation"** $\rightarrow$ State transitions to `IN_PROGRESS`. Add diagnostic internal notes.
5. **Resolution**: Click **"Resolve Incident"**, document root cause and solution $\rightarrow$ State transitions to `RESOLVED`, resolution timestamp is captured, and technician active workload automatically decrements.

### Workflow 3: User Verification, Feedback, Duplicate Detection & AI Assistant
1. **Verification & Feedback**: Return to Employee (`emp_john`). View the resolved ticket, click **"Verify"** and **"Close Incident"**, and submit a 5-star satisfaction rating.
2. **Duplicate Detection**: Go to **Report Incident** and begin typing: *"Payment application unavailable during transactions"*. Notice the system instantly displays the **Duplicate Incident Alert**, linking to the previous ticket.
3. **AI IT Assistant & Voice Input**: Open the floating **IT Assistant** in the bottom-right corner. Use speech-to-text or type: *"i have an issue with network"*. Watch the assistant guide you through interactive service selection, self-check troubleshooting, and ticket drafting.

---

## 8. Innovation Challenges Implemented

The team implemented multiple advanced capabilities exceeding basic course requirements:
1. **Rule-Based & LLM Chatbot Engine**: Dual-strategy conversational assistant with zero-key offline local operation and optional Google Gemini / Anthropic Claude support.
2. **Speech Recognition Voice Reporting**: Native browser Web Speech API integration in `ChatWidget` allowing hands-free voice-to-text incident creation.
3. **Customer Frustration Detection Engine**: Heuristic multi-signal sentiment analysis tracking text aggression, repetition, and punctuation with mathematical half-life score decay.
4. **Automatic Escalation Hierarchy**: Continuous scheduled background monitor escalates unacknowledged P1 incidents up the chain (Technician $\rightarrow$ Lead $\rightarrow$ Admin).
5. **Real-Time SMTP Email Dispatch**: Gmail SMTP integration delivering real-world notifications to technicians and administrators.
6. **Lexical Duplicate Detection**: Token-based Jaccard similarity engine catching duplicate incidents before database submission.
7. **Resolution History Recommendation**: Analyzes past resolved incidents to suggest proven solutions to technicians during investigation.

---

## 9. Conclusion

The **Intelligent IT Service Management & Incident Resolution System (I-ITSM)** successfully satisfies and exceeds every standard and advanced requirement established in the CS F213 / MAC F212 course syllabus. Through strict adherence to Object-Oriented Design principles (SOLID, Design Patterns, layered abstractions, defensive validation) and modern full-stack technologies (Java 21, Spring Boot 4, PostgreSQL 18, React 19), the system represents a complete, production-ready enterprise software solution.
