package com.itsm.repository;

import com.itsm.entity.Notification;
import com.itsm.entity.User;
import com.itsm.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByRecipientAndTypeOrderByCreatedAtDesc(User recipient, NotificationType type);

    List<Notification> findByRecipientAndTypeAndReadFalse(User recipient, NotificationType type);

    long countByRecipientAndTypeAndReadFalse(User recipient, NotificationType type);

    /** Simulated e-mail / SMS outbox shown to administrators. */
    List<Notification> findTop100ByTypeInOrderByCreatedAtDesc(Collection<NotificationType> types);
}
