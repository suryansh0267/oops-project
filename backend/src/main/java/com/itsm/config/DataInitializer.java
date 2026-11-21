package com.itsm.config;

import com.itsm.entity.*;
import com.itsm.enums.*;
import com.itsm.repository.*;
import com.itsm.chatbot.*;
import com.itsm.intelligence.frustration.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ITServiceRepository itServiceRepository;
    private final SLAPolicyRepository slaPolicyRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentCategoryConfigRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final IncidentNoteRepository incidentNoteRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChatSessionRepository chatSessions;
    private final ChatMessageRepository chatMessages;
    private final FrustrationStateRepository frustrationStates;
    private final FrustrationRecordRepository frustrationRecords;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            List<String> defaultCats = List.of(
                    "HARDWARE", "SOFTWARE", "NETWORK", "DATABASE", "CLOUD",
                    "SECURITY", "AUTHENTICATION", "APPLICATION", "STORAGE", "OTHER"
            );
            for (String cat : defaultCats) {
                categoryRepository.save(IncidentCategoryConfig.builder()
                        .name(cat)
                        .description("Standard category for " + cat.toLowerCase() + " incidents")
                        .keywords(new ArrayList<>(com.itsm.intelligence.CategoryClassifier.DEFAULT_KEYWORDS
                                .getOrDefault(cat, List.of())))
                        .active(true)
                        .build());
            }
            log.info("Initialized {} default incident categories", defaultCats.size());
        }

        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data setup.");
            return;
        }

        log.info("Seeding initial data for ITSM application...");

        // 1. Users
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@itsm.local")
                .fullName("System Administrator")
                .role(UserRole.ADMIN)
                .department("IT Infrastructure")
                .phoneNumber("555-0100")
                .active(true)
                .build();
        userRepository.save(admin);

        User techNetwork = User.builder()
                .username("tech_network")
                .password(passwordEncoder.encode("tech123"))
                .email("alex.network@itsm.local")
                .fullName("Alex Chen")
                .role(UserRole.TECHNICIAN)
                .department("Network Operations")
                .phoneNumber("555-0101")
                .active(true)
                .build();
        userRepository.save(techNetwork);

        User techDb = User.builder()
                .username("tech_db")
                .password(passwordEncoder.encode("tech123"))
                .email("priya.data@itsm.local")
                .fullName("Priya Sharma")
                .role(UserRole.TECHNICIAN)
                .department("Data & Cloud Services")
                .phoneNumber("555-0102")
                .active(true)
                .build();
        userRepository.save(techDb);

        User techApp = User.builder()
                .username("tech_app")
                .password(passwordEncoder.encode("tech123"))
                .email("marcus.app@itsm.local")
                .fullName("Marcus Taylor")
                .role(UserRole.TECHNICIAN)
                .department("Enterprise Applications")
                .phoneNumber("555-0103")
                .active(true)
                .build();
        userRepository.save(techApp);

        User empJohn = User.builder()
                .username("emp_john")
                .password(passwordEncoder.encode("emp123"))
                .email("john.doe@itsm.local")
                .fullName("John Doe")
                .role(UserRole.EMPLOYEE)
                .department("Finance & Billing")
                .phoneNumber("555-0201")
                .active(true)
                .build();
        userRepository.save(empJohn);

        User empSarah = User.builder()
                .username("emp_sarah")
                .password(passwordEncoder.encode("emp123"))
                .email("sarah.connor@itsm.local")
                .fullName("Sarah Connor")
                .role(UserRole.EMPLOYEE)
                .department("Human Resources")
                .phoneNumber("555-0202")
                .active(true)
                .build();
        userRepository.save(empSarah);

        // 2. Technician Profiles
        technicianProfileRepository.save(TechnicianProfile.builder()
                .user(techNetwork)
                .skills(List.of("NETWORK", "SECURITY", "HARDWARE"))
                .certifications(List.of("CCNA", "CompTIA Security+"))
                .specialization("Network & Perimeter Security")
                .maxActiveIncidents(8)
                .currentActiveIncidents(0)
                .totalResolvedIncidents(14)
                .avgResolutionTimeMinutes(45.0)
                .available(true)
                .build());

        technicianProfileRepository.save(TechnicianProfile.builder()
                .user(techDb)
                .skills(List.of("DATABASE", "CLOUD", "STORAGE"))
                .certifications(List.of("AWS Solutions Architect", "PostgreSQL Certified"))
                .specialization("Database & Cloud Infrastructure")
                .maxActiveIncidents(10)
                .currentActiveIncidents(0)
                .totalResolvedIncidents(22)
                .avgResolutionTimeMinutes(38.0)
                .available(true)
                .build());

        technicianProfileRepository.save(TechnicianProfile.builder()
                .user(techApp)
                .skills(List.of("APPLICATION", "SOFTWARE", "AUTHENTICATION"))
                .certifications(List.of("Spring Certified Professional"))
                .specialization("Core Web Services & Identity")
                .maxActiveIncidents(10)
                .currentActiveIncidents(0)
                .totalResolvedIncidents(19)
                .avgResolutionTimeMinutes(30.0)
                .available(true)
                .build());

        // 3. IT Services
        ITService paymentService = itServiceRepository.save(ITService.builder()
                .name("Payment Gateway")
                .description("Processes enterprise billing, client invoicing and payments")
                .category("Finance")
                .businessCriticality(5)
                .supportContact("payments-team@itsm.local")
                .active(true)
                .build());

        ITService authService = itServiceRepository.save(ITService.builder()
                .name("Authentication & SSO Service")
                .description("Central Keycloak / OAuth2 Single Sign-On for all internal tools")
                .category("Security")
                .businessCriticality(5)
                .supportContact("iam-team@itsm.local")
                .active(true)
                .build());

        ITService emailService = itServiceRepository.save(ITService.builder()
                .name("Corporate Email (Exchange)")
                .description("Enterprise mailing and scheduling system")
                .category("Communication")
                .businessCriticality(3)
                .supportContact("exchange-ops@itsm.local")
                .active(true)
                .build());

        ITService dbService = itServiceRepository.save(ITService.builder()
                .name("Primary Database Cluster")
                .description("Main transactional PostgreSQL database cluster")
                .category("Database")
                .businessCriticality(5)
                .supportContact("dba@itsm.local")
                .active(true)
                .build());

        ITService storageService = itServiceRepository.save(ITService.builder()
                .name("Cloud Document Storage")
                .description("Secure distributed file storage for employee documents")
                .category("Cloud")
                .businessCriticality(3)
                .supportContact("cloud-ops@itsm.local")
                .active(true)
                .build());

        ITService networkService = itServiceRepository.save(ITService.builder()
                .name("Corporate Network & VPN")
                .description("Enterprise Wi-Fi, Ethernet, VPN and internal routing")
                .category("Network")
                .businessCriticality(4)
                .supportContact("network-ops@itsm.local")
                .active(true)
                .build());

        // 4. SLA Policies
        slaPolicyRepository.save(SLAPolicy.builder()
                .priority(Priority.P1_CRITICAL)
                .responseTimeMinutes(5)
                .resolutionTimeMinutes(30)
                .description("Critical business stoppage - highest urgency")
                .active(true)
                .build());

        slaPolicyRepository.save(SLAPolicy.builder()
                .priority(Priority.P2_HIGH)
                .responseTimeMinutes(15)
                .resolutionTimeMinutes(120)
                .description("Major component unavailable with high impact")
                .active(true)
                .build());

        slaPolicyRepository.save(SLAPolicy.builder()
                .priority(Priority.P3_MEDIUM)
                .responseTimeMinutes(60)
                .resolutionTimeMinutes(480)
                .description("Moderate issue affecting normal workflow")
                .active(true)
                .build());

        slaPolicyRepository.save(SLAPolicy.builder()
                .priority(Priority.P4_LOW)
                .responseTimeMinutes(240)
                .resolutionTimeMinutes(1440)
                .description("Minor issue or cosmetic glitch with workarounds")
                .active(true)
                .build());

        // 5. Demo incidents covering every workflow shown in the video
        LocalDateTime now = LocalDateTime.now();

        // Workflow 3 cluster: three open reports against the payment gateway within the hour
        Incident pay1 = seed("Payment application unavailable during checkout",
                "Users get a 503 Bad Gateway when submitting card payments. Around 200 customers affected.",
                empJohn, "APPLICATION", Impact.HIGH, Urgency.HIGH, Priority.P1_CRITICAL, IncidentStatus.IN_PROGRESS,
                paymentService, techApp, now.minusMinutes(20), now.minusMinutes(18), null);
        note(pay1, techApp, "Error rate jumped after the 14:00 deploy. Checking the gateway pods.", true, now.minusMinutes(15));
        seed("Payment transaction failing at checkout",
                "Card payments time out after 30 seconds and the order is not created.",
                empSarah, "APPLICATION", Impact.HIGH, Urgency.HIGH, Priority.P1_CRITICAL, IncidentStatus.CLASSIFIED,
                paymentService, null, now.minusMinutes(25), null, null);
        seed("Unable to make payment, gateway timeout",
                "The payment page spins and then shows a gateway timeout error.",
                empJohn, "APPLICATION", Impact.MEDIUM, Urgency.HIGH, Priority.P2_HIGH, IncidentStatus.CLASSIFIED,
                paymentService, null, now.minusMinutes(12), null, null);

        Incident vpn = seed("Cannot authenticate to VPN from remote branch",
                "LDAP authentication times out when logging in to the VPN gateway from the Pune office.",
                empSarah, "AUTHENTICATION", Impact.HIGH, Urgency.MEDIUM, Priority.P2_HIGH, IncidentStatus.ASSIGNED,
                authService, techNetwork, now.minusMinutes(10), null, null);
        note(vpn, techNetwork, "[INFO_REQUEST] Which VPN client version are you using, and does it fail on mobile data too?", false, now.minusMinutes(6));
        note(vpn, empSarah, "[INFO_RESPONSE] Client 4.10. Mobile data fails the same way.", false, now.minusMinutes(3));

        seed("Database deadlocks during month-end billing run",
                "Billing batch job aborts with deadlock detected errors on the invoices table.",
                empJohn, "DATABASE", Impact.HIGH, Urgency.MEDIUM, Priority.P2_HIGH, IncidentStatus.ACKNOWLEDGED,
                dbService, techDb, now.minusMinutes(40), now.minusMinutes(30), null);
        seed("Database read replicas showing 15s replication lag",
                "Analytics queries return stale data because WAL replay on the replicas is delayed.",
                empJohn, "DATABASE", Impact.MEDIUM, Urgency.LOW, Priority.P3_MEDIUM, IncidentStatus.CLASSIFIED,
                dbService, null, now.minusHours(2), null, null);

        // Resolved / closed history: feeds suggested resolutions, trends and SLA compliance
        Incident outlook = seed("Outlook not syncing mail on laptop",
                "New mail stopped arriving in Outlook since this morning; webmail works.",
                empSarah, "SOFTWARE", Impact.LOW, Urgency.MEDIUM, Priority.P4_LOW, IncidentStatus.RESOLVED,
                emailService, techApp, now.minusDays(2).minusHours(3), now.minusDays(2).minusHours(2), now.minusDays(2));
        outlook.setResolutionNotes("Cleared the Outlook OST cache and re-created the mail profile. Sync resumed.");
        incidentRepository.save(outlook);

        closed("Payment gateway returning 503 after deployment",
                "Checkout fails with 503 errors right after release 4.2.1.",
                empJohn, "APPLICATION", Priority.P1_CRITICAL, paymentService, techApp, now.minusDays(5), 4, 25,
                "Rolled back release 4.2.1, restarted the gateway pods and cleared the connection pool. "
                        + "Root cause: a missing environment variable in the new release.", 5, "Back within half an hour, thanks!");
        closed("VPN login fails with LDAP timeout",
                "Remote staff cannot sign in to the VPN; the client reports an LDAP timeout.",
                empSarah, "AUTHENTICATION", Priority.P2_HIGH, authService, techNetwork, now.minusDays(3), 10, 95,
                "Restarted the LDAP connector service and raised the VPN gateway LDAP timeout from 5s to 15s.", 4, null);
        closed("Shared drive quota full for HR folder",
                "Cannot save documents to the HR shared folder: quota exceeded.",
                empSarah, "STORAGE", Priority.P3_MEDIUM, storageService, techDb, now.minusDays(6), 50, 600,
                "Archived files older than two years to cold storage and raised the folder quota to 200 GB.", 3, "Took a while.");
        closed("Wi-Fi drops in third floor meeting rooms",
                "Laptops lose Wi-Fi every few minutes in rooms 301-305.",
                empJohn, "NETWORK", Priority.P3_MEDIUM, null, techNetwork, now.minusDays(9), 30, 300,
                "Replaced a failing access point in room 303 and moved two APs off the congested channel 6.", 5, null);
        closed("Laptop battery swelling",
                "My laptop battery has swollen and the trackpad is lifting.",
                empJohn, "HARDWARE", Priority.P3_MEDIUM, null, techNetwork, now.minusDays(12), 40, 200,
                "Replaced the battery under warranty and issued a loaner for the day.", 5, null);

        // Demo-only fixtures appended AFTER every original incident: INC00001/INC00002 stay unchanged.
        User frustratedDemo = userRepository.save(User.builder().username("emp_demo")
                .password(passwordEncoder.encode("demo123")).email("frustrated.demo@itsm.local")
                .fullName("Demo Reporter").department("Operations").role(UserRole.EMPLOYEE).active(true).build());
        Incident frustrationDemo = seed("Recurring VPN disconnects despite initial fix",
                "VPN disconnects every fifteen minutes even after resetting the client.", frustratedDemo,
                "NETWORK", Impact.MEDIUM, Urgency.MEDIUM, Priority.P3_MEDIUM,
                IncidentStatus.IN_PROGRESS, authService, techNetwork, now.minusDays(1), now.minusHours(20), null);
        note(frustrationDemo, frustratedDemo, "Reopened: the VPN issue returned after the first fix", false, now.minusHours(4));
        audit(frustrationDemo, frustratedDemo, "REOPENED", "RESOLVED", "IN_PROGRESS",
                "Reporter reopened the recurring issue", now.minusHours(4));
        frustrationDemo.setFrustrationLevel(FrustrationLevel.FRUSTRATED);
        frustrationDemo.setFrustrationReasons("Repeated failures or poor support; Repeated follow-up or service history");
        incidentRepository.save(frustrationDemo);
        FrustrationState demoState = new FrustrationState();
        demoState.setSubjectKey(frustratedDemo.getId() + ":" + frustrationDemo.getId());
        demoState.setUser(frustratedDemo); demoState.setIncident(frustrationDemo);
        demoState.setScore(67); demoState.setLevel(FrustrationLevel.FRUSTRATED);
        demoState.setAssessedAt(java.time.Instant.now()); demoState.setNotifiedAt(java.time.Instant.now());
        demoState.setLastMessage("THIRD TIME this week, useless"); frustrationStates.save(demoState);
        FrustrationRecord demoRecord = new FrustrationRecord(); demoRecord.setUser(frustratedDemo);
        demoRecord.setIncident(frustrationDemo); demoRecord.setLevel(FrustrationLevel.FRUSTRATED);
        demoRecord.setScore(67); demoRecord.setReasons(frustrationDemo.getFrustrationReasons());
        demoRecord.setRecordedAt(java.time.Instant.now()); frustrationRecords.save(demoRecord);
        ChatSession demoSession = new ChatSession(); demoSession.setUser(frustratedDemo);
        demoSession.setActiveIncidentId(frustrationDemo.getId()); demoSession.setUpdatedAt(java.time.Instant.now());
        chatSessions.save(demoSession);
        ChatMessage first = new ChatMessage(); first.setSession(demoSession); first.setFromUser(true);
        first.setText("still not fixed!!"); first.setIntent("FALLBACK"); first.setMessageFrustrationScore(38);
        first.setIncidentId(frustrationDemo.getId()); first.setCreatedAt(java.time.Instant.now().minusSeconds(120));
        chatMessages.save(first);
        ChatMessage second = new ChatMessage(); second.setSession(demoSession); second.setFromUser(true);
        second.setText("THIRD TIME this week, useless"); second.setIntent("FALLBACK"); second.setMessageFrustrationScore(67);
        second.setIncidentId(frustrationDemo.getId()); second.setCreatedAt(java.time.Instant.now().minusSeconds(60));
        chatMessages.save(second);

        // Keep technician workload counters consistent with the seeded data
        for (TechnicianProfile profile : technicianProfileRepository.findAll()) {
            long active = incidentRepository.countByAssignedTechnicianAndStatusIn(profile.getUser(),
                    com.itsm.state.IncidentStateFactory.openStatuses());
            profile.setCurrentActiveIncidents((int) active);
            technicianProfileRepository.save(profile);
        }

        log.info("Initial seeding complete! Demo credentials created:");
        log.info("  Admin: admin / admin123");
        log.info("  Technicians: tech_network, tech_db, tech_app (password: tech123)");
        log.info("  Employees: emp_john, emp_sarah (password: emp123)");
        log.info("  Frustration demo: emp_demo / demo123, incident {}", frustrationDemo.getIncidentNumber());
    }

    private static final java.util.Map<Priority, int[]> SLA_MINUTES = java.util.Map.of(
            Priority.P1_CRITICAL, new int[]{5, 30},
            Priority.P2_HIGH, new int[]{15, 120},
            Priority.P3_MEDIUM, new int[]{60, 480},
            Priority.P4_LOW, new int[]{240, 1440});

    /** Saves a demo incident with back-dated timestamps, deadlines and a short audit trail. */
    private Incident seed(String title, String description, User reporter, String category, Impact impact,
                          Urgency urgency, Priority priority, IncidentStatus status, ITService service, User tech,
                          LocalDateTime created, LocalDateTime respondedAt, LocalDateTime resolvedAt) {
        int[] sla = SLA_MINUTES.get(priority);
        Incident incident = incidentRepository.save(Incident.builder()
                .title(title).description(description).reporter(reporter).category(category)
                .impact(impact).urgency(urgency).priority(priority).status(status)
                .affectedService(service).assignedTechnician(tech)
                .slaPolicy(slaPolicyRepository.findByPriority(priority).orElse(null))
                .escalationLevel(0)
                .notes(new ArrayList<>()).auditLogs(new ArrayList<>())
                .build());
        incident.setIncidentNumber(String.format("INC%05d", incident.getId()));
        incident.setCreatedAt(created);
        incident.setResponseDeadline(created.plusMinutes(sla[0]));
        incident.setResolutionDeadline(created.plusMinutes(sla[1]));
        incident.setRespondedAt(respondedAt);
        incident.setResolvedAt(resolvedAt);
        incident = incidentRepository.save(incident);

        audit(incident, reporter, "CREATED", null, "NEW", "Incident reported", created);
        audit(incident, null, "CLASSIFIED", "NEW", "CLASSIFIED", "Category: " + category + ", priority " + priority, created);
        if (tech != null) {
            audit(incident, null, "AUTO_ASSIGNED", "Unassigned", tech.getFullName(), "Assigned to " + tech.getFullName(), created.plusMinutes(1));
        }
        if (respondedAt != null) {
            audit(incident, tech, "ACKNOWLEDGED", "ASSIGNED", "ACKNOWLEDGED", "Technician acknowledged the incident", respondedAt);
        }
        if (resolvedAt != null) {
            audit(incident, tech, "RESOLVED", "IN_PROGRESS", "RESOLVED", "Incident resolved", resolvedAt);
        }
        return incident;
    }

    private void closed(String title, String description, User reporter, String category, Priority priority,
                        ITService service, User tech, LocalDateTime created, int respondAfterMin, int resolveAfterMin,
                        String resolution, int rating, String feedback) {
        Incident incident = seed(title, description, reporter, category, Impact.MEDIUM, Urgency.MEDIUM, priority,
                IncidentStatus.CLOSED, service, tech, created, created.plusMinutes(respondAfterMin),
                created.plusMinutes(resolveAfterMin));
        incident.setResolutionNotes(resolution);
        incident.setClosedAt(created.plusMinutes(resolveAfterMin + 60L));
        incident.setFeedbackRating(rating);
        incident.setFeedback(feedback);
        incidentRepository.save(incident);
        audit(incident, reporter, "CLOSED", "RESOLVED", "CLOSED", "Incident closed", incident.getClosedAt());
    }

    private void audit(Incident incident, User by, String action, String from, String to, String description, LocalDateTime at) {
        AuditLog entry = auditLogRepository.save(AuditLog.builder().incident(incident).performedBy(by).action(action)
                .previousValue(from).newValue(to).description(description).build());
        entry.setTimestamp(at);
        auditLogRepository.save(entry);
    }

    private void note(Incident incident, User author, String content, boolean internal, LocalDateTime at) {
        IncidentNote n = incidentNoteRepository.save(IncidentNote.builder().incident(incident).author(author)
                .content(content).isInternalNote(internal).build());
        n.setCreatedAt(at);
        incidentNoteRepository.save(n);
    }
}
