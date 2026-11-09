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
 *
 * → {"conversationId": "3f2b...", "reply": "..."}
 * </pre>
 * Send the returned {@code conversationId} with your next message to continue the conversation.
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
        ChatService.Reply reply = chatService.chat(request.conversationId(), request.message());
        return new ChatResponse(reply.conversationId(), reply.text());
    }

    /** {@code conversationId} is optional: leave it out to start a new conversation. */
    public record ChatRequest(String conversationId, String message) {}

    public record ChatResponse(String conversationId, String reply) {}
}
