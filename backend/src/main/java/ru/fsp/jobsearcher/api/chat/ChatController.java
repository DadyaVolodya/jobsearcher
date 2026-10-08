package ru.fsp.jobsearcher.api.chat;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.jobsearcher.application.security.SecurityUtils;
import ru.fsp.jobsearcher.application.service.ChatService;
import ru.fsp.jobsearcher.domain.entity.ChatMessage;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Tag(name = "Chats")
public class ChatController {

    private final SecurityUtils securityUtils;
    private final ChatService chatService;

    @GetMapping
    public List<ChatService.ThreadView> list() {
        return chatService.list(securityUtils.requireCurrentUser());
    }

    @GetMapping("/{threadId}/messages")
    public List<ChatMessage> messages(@PathVariable UUID threadId) {
        return chatService.messages(securityUtils.requireCurrentUser(), threadId);
    }

    @PostMapping("/{threadId}/messages")
    public ChatMessage send(@PathVariable UUID threadId, @RequestBody Map<String, String> body) {
        return chatService.send(securityUtils.requireCurrentUser(), threadId, body.get("body"));
    }
}
