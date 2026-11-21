package com.itsm.chatbot;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Getter @Setter
@Table(name = "chat_messages", indexes = @Index(name = "idx_chat_session_message", columnList = "session_id,created_at"))
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "session_id") private ChatSession session;
    private boolean fromUser;
    @Column(length = 3000) private String text;
    private String intent;
    private Integer messageFrustrationScore;
    private Long incidentId;
    @Column(name = "created_at") private Instant createdAt;
}
