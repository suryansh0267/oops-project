package com.itsm.controller;

import com.itsm.dto.NotificationResponse;
import com.itsm.security.CustomUserDetails;
import com.itsm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal CustomUserDetails me) {
        return notificationService.getNotifications(me.getUsername());
    }

    @GetMapping("/unread/count")
    public long unreadCount(@AuthenticationPrincipal CustomUserDetails me) {
        return notificationService.getUnreadCount(me.getUsername());
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return notificationService.markAsRead(id, me.getUsername());
    }

    @PutMapping("/read-all")
    public Map<String, Object> markAllRead(@AuthenticationPrincipal CustomUserDetails me) {
        return notificationService.markAllAsRead(me.getUsername());
    }
}
