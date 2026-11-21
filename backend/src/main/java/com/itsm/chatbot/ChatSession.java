package com.itsm.chatbot;

import com.itsm.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Getter @Setter
@Table(name = "chat_sessions")
public class ChatSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @Enumerated(EnumType.STRING) private ChatState state = ChatState.IDLE;
    private String draftTitle;
    @Column(length = 5000) private String draftDescription;
    private String draftCategory;
    private Long draftServiceId;
    private String draftServiceName;
    private String draftImpact;
    private String draftUrgency;
    private Integer draftAffectedUsers;
    private Long activeIncidentId;
    @Column(length = 1000) private String pendingNote;
    private Instant updatedAt;

    public void clearDraft() {
        state = ChatState.IDLE; draftTitle = null; draftDescription = null; draftCategory = null;
        draftServiceId = null; draftServiceName = null; draftImpact = null; draftUrgency = null;
        draftAffectedUsers = null; pendingNote = null;
    }
}
