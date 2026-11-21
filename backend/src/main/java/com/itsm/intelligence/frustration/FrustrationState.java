package com.itsm.intelligence.frustration;

import com.itsm.entity.Incident;
import com.itsm.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Getter @Setter
@Table(name = "frustration_states", indexes = @Index(name = "idx_frustration_user", columnList = "user_id"))
public class FrustrationState {
    @Id @Column(length = 80) private String subjectKey;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne private Incident incident;
    private int score;
    @Enumerated(EnumType.STRING) private FrustrationLevel level = FrustrationLevel.CALM;
    private Instant assessedAt;
    private Instant notifiedAt;
    @Column(length = 1000) private String lastMessage;
    private int fallbackStreak;
    private boolean dismissed;
}
