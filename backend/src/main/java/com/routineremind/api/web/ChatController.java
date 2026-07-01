package com.routineremind.api.web;

import com.routineremind.api.model.ChatMessage;
import com.routineremind.api.model.ChatRequest;
import com.routineremind.api.model.ChatResponse;
import com.routineremind.api.security.AuthUser;
import com.routineremind.api.security.CurrentUser;
import com.routineremind.api.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse ask(@CurrentUser AuthUser authUser, @Valid @RequestBody ChatRequest request) {
        return chatService.answer(authUser.uid(), request.ownerUid(), request.message());
    }

    @GetMapping("/history")
    public List<ChatMessage> history(@CurrentUser AuthUser authUser,
                                     @RequestParam(required = false) String ownerUid,
                                     @RequestParam(required = false) Integer limit) {
        return chatService.history(authUser.uid(), ownerUid, limit);
    }
}
