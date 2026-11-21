# I-ITSM v2 — implementation and evidence checklist

Legend: `[x]` source implementation present; `[ ]` incomplete or execution not yet verified. A code checkbox **does not imply a passing integration test**.

## Baseline, architecture, integration

- [x] Baseline `npm ci && npm run build` completed successfully in this session.
- [ ] Baseline `./mvnw clean test`: blocked by unreachable Maven Central parent POM (`spring-boot-starter-parent:4.0.0`); not a code-pass claim.
- [x] Existing IncidentService, security/access policy, state, strategy, observer, mapper, notifications and frontend architecture inspected.
- [x] Chat engine implements `NamedStrategy`, selected by generic `StrategyRegistry`.
- [x] Chat session and message entities (one session per user via unique DB constraint; capped history).
- [x] Draft state persisted; confirmation state single-use; no side effects before confirmation.
- [ ] Dialogue states use one enum-based handler, not the brief's requested one-class-per-state OO implementation.
- [x] Incident creation delegates to original IncidentService, preserving lifecycle, assignment, SLA and audit.
- [x] DTO-based chat replies with text, cards, quick replies and internal-route actions.
- [x] Default rule-based operation requires no API keys.
- [x] Gemini/Anthropic adapters, injected HTTP seam, header-only keys, 3s connect/8s request, no retry.
- [x] Provider missing/invalid/bad JSON/timeout fallback and enum/length validation.
- [x] LLM input is masked current user text and constrained system prompt; model never directly executes actions.
- [x] Chat per-user in-memory sliding rate limit and friendly HTTP 429 handler.
- [x] IncidentAccessPolicy checked for explicit/chat-linked incidents; no internal notes in employee chat.
- [x] Chat history excludes per-message frustration metadata.
- [x] Human handoff requires confirmation; note + technician event; no false live-agent claims.
- [ ] Human request without a linked incident does not automatically open a ticket; chatbot directs user to the report flow.

## Intents and rule-based flow

- [x] Employee: REPORT_INCIDENT, CHECK_STATUS, TROUBLESHOOT, SLA_QUESTION, ADD_UPDATE, REQUEST_HUMAN, HELP, GREETING, FALLBACK.
- [x] Staff: MY_QUEUE, SUMMARIZE_INCIDENT; ADMIN: SYSTEM_SUMMARY; roles checked before execution.
- [x] Incident selection order: explicit number, linked active incident, recent open incident.
- [x] Service → description → impact → duplicate/self-fix preview → confirm/create; cancel/restart works.
- [x] Similar report count shown without leaking other reporters' IDs, titles or notes.
- [ ] Self-fix hints are category-based; chatbot does not expose case-specific notes from unrelated reporters.
- [x] Transcript profanity masked before persistence and provider calls.
- [x] Optional LLM draft extraction can infer title/category/impact/urgency/affectedUsers.

## Frustration detection

- [x] Injectable `List<FrustrationSignal>` and immutable `SignalResult`/`FrustrationAssessment`.
- [x] Service-directed anger, repetition, caps/punctuation, escalation demands, masked profanity.
- [x] Behaviour: reopen history, chasers, SLA risk, unacknowledged P1/P2, repeated same-service incidents, low feedback, reassignment and info ping-pong.
- [x] Optional LLM sentiment signal off by default, low weight.
- [x] Calm technical failure false-positive guard.
- [x] Weighted 0–100 aggregation, 12h half-life, 25/55/85 thresholds, rising/steady/falling trend.
- [x] Repeated frustration demo ladder and decaying calm behaviour encoded as JUnit fixtures.
- [x] Per-user/incident persistence and level-rise cooldown with critical override.
- [x] Observer writes record, audited flag, technician in-app alert, CRITICAL manager email/SMS Outbox.
- [x] Optional CRITICAL auto-priority reconsideration off by default.
- [x] Triggers: chat messages, reporter note, reply, feedback, reopen, bounded scheduled re-check.
- [x] Staff-only dismiss endpoint with audit; employee IncidentResponse has no frustration fields.
- [x] Existing incident Specification filters staff frustration levels and sorts flagged-first.
- [x] Staff React badges/reasons/dismiss/filter/sort; admin 14-day chart and services breakdown.
- [x] AdminAutomation shows runtime frustration config read-only with environment/restart instructions.
- [x] Heuristic false-positive caution documented and shown in the UI.

## Operations, seed, tests, docs

- [x] Optional SMTP sender behind flag; Outbox persists without real email transport.
- [x] Runtime config and Docker environment variables for providers, sentiment, escalation and SMTP.
- [x] `.env.example`, `.env` ignore, CI backend test/frontend build workflow.
- [x] Original seeded incidents retained in order; separate `emp_demo` account/incident and frustration timeline appended.
- [x] README tables and separate project-report section with 10 new test descriptions.
- [x] Plain JUnit/Mockito test classes for signals, intents, report confirmation/security and provider fallbacks.
- [ ] Full backend tests and clean-slate Docker demo: not yet runnable in this restricted environment; must run before GitHub push.
- [ ] Live Gemini/Anthropic/SMTP calls: no keys provided; cannot verify external provider integrations.
- [ ] End-to-end replay of acceptance 1–7 against running Postgres: not yet verified.

## Calibration and decisions

With the default text signal, `still not fixed!!` produces ~38 (ANNOYED), followed by `THIRD TIME this week, useless` ~67 (FRUSTRATED). An explicit manager demand after that sets a CRITICAL score floor. For a lone manager demand, the floor is only FRUSTRATED. Inactivity reduces prior evidence exponentially, then the new signal is smoothed 35/65. Duplicate and case-specific data from other reporters are intentionally hidden in the chatbot even when a similarity match exists. `IncidentEventType.FRUSTRATION_ALERT` is used instead of `NotificationType.FRUSTRATION_ALERT`, because in this repository `NotificationType` identifies delivery transport (IN_APP, EMAIL, SMS), not business event kind.
