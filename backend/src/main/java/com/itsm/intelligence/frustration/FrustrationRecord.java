package com.itsm.intelligence.frustration;

import com.itsm.entity.Incident;
import com.itsm.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Getter @Setter
@Table(name = "frustration_records", indexes = @Index(name = "idx_frustration_record_time", columnList = "recorded_at"))
public class FrustrationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private User user;
    @ManyToOne private Incident incident;
    @Enumerated(EnumType.STRING) private FrustrationLevel level;
    private int score;
    @Column(length = 500) private String reasons;
    @Column(name = "recorded_at") private Instant recordedAt;
}
