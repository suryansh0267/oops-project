package com.itsm.chatbot;

import com.itsm.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByUser(User user);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChatSession s where s.user = :user")
    Optional<ChatSession> findLockedByUser(@Param("user") User user);
}
