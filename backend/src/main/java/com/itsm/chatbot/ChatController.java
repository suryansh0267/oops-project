package com.itsm.chatbot;

import com.itsm.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chat;
    @PostMapping("/message")
    public ChatReply message(@Valid @RequestBody ChatMessageRequest body,
                             @AuthenticationPrincipal CustomUserDetails me) {
        return chat.message(me.getUsername(), body.message());
    }
    @GetMapping("/history")
    public List<Map<String, Object>> history(@AuthenticationPrincipal CustomUserDetails me) {
        return chat.history(me.getUsername());
    }
    @PostMapping("/reset")
    public ChatReply reset(@AuthenticationPrincipal CustomUserDetails me) { return chat.reset(me.getUsername()); }
}
