package com.islamshariful.spring_mcp_client.chat;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat with Claude, which can use the MCP server's tools.
 *
 * <pre>
 * POST /api/chat
 * {"message": "Do I need an umbrella in Dhaka tomorrow?"}
 * </pre>
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("message can't be empty");
        }
        return new ChatResponse(chatService.chat(request.message()));
    }

    public record ChatRequest(String message) {}

    public record ChatResponse(String reply) {}
}
