package com.itsm.config;

import com.itsm.entity.*;
import com.itsm.enums.*;
import com.itsm.repository.*;
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
    private final PasswordEncoder passwordEncoder;

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

        // 5. Seed Initial Sample Incidents for immediate demo
        LocalDateTime now = LocalDateTime.now();

        Incident inc1 = Incident.builder()
                .title("Payment application unavailable during checkout")
                .description("Users encountering 503 Bad Gateway when submitting credit card payments.")
                .reporter(empJohn)
                .category(IncidentCategory.APPLICATION)
                .impact(Impact.HIGH)
                .urgency(Urgency.HIGH)
                .priority(Priority.P1_CRITICAL)
                .status(IncidentStatus.IN_PROGRESS)
                .affectedService(paymentService)
                .assignedTechnician(techApp)
                .responseDeadline(now.plusMinutes(5))
                .resolutionDeadline(now.plusMinutes(30))
                .respondedAt(now.minusMinutes(10))
                .notes(new ArrayList<>())
                .auditLogs(new ArrayList<>())
                .build();
        incidentRepository.save(inc1);

        Incident inc2 = Incident.builder()
                .title("Cannot authenticate to VPN from remote branch")
                .description("LDAP authentication timeout when logging into Cisco AnyConnect gateway.")
                .reporter(empSarah)
                .category(IncidentCategory.AUTHENTICATION)
                .impact(Impact.HIGH)
                .urgency(Urgency.MEDIUM)
                .priority(Priority.P2_HIGH)
                .status(IncidentStatus.ASSIGNED)
                .affectedService(authService)
                .assignedTechnician(techNetwork)
                .responseDeadline(now.plusMinutes(15))
                .resolutionDeadline(now.plusHours(2))
                .notes(new ArrayList<>())
                .auditLogs(new ArrayList<>())
                .build();
        incidentRepository.save(inc2);

        Incident inc3 = Incident.builder()
                .title("Database read replicas showing 15s replication lag")
                .description("Analytics queries are returning stale data due to delayed WAL replay.")
                .reporter(empJohn)
                .category(IncidentCategory.DATABASE)
                .impact(Impact.MEDIUM)
                .urgency(Urgency.LOW)
                .priority(Priority.P3_MEDIUM)
                .status(IncidentStatus.CLASSIFIED)
                .affectedService(dbService)
                .responseDeadline(now.plusHours(1))
                .resolutionDeadline(now.plusHours(8))
                .notes(new ArrayList<>())
                .auditLogs(new ArrayList<>())
                .build();
        incidentRepository.save(inc3);

        log.info("Initial seeding complete! Demo credentials created:");
        log.info("  Admin: admin / admin123");
        log.info("  Technicians: tech_network, tech_db, tech_app (password: tech123)");
        log.info("  Employees: emp_john, emp_sarah (password: emp123)");
    }
}
