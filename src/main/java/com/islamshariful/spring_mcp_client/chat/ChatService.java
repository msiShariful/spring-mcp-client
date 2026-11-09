package com.islamshariful.spring_mcp_client.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Sends the user's message to Claude together with the MCP server's tools.
 *
 * <p>How a question like "Do I need an umbrella in Dhaka tomorrow?" is answered:
 * <ol>
 *   <li>Claude gets the message plus the list of tools from the MCP server.</li>
 *   <li>Claude decides to call {@code get_forecast} with city "Dhaka".</li>
 *   <li>Spring AI runs that tool on the MCP server and gives Claude the result.</li>
 *   <li>Claude writes the final answer.</li>
 * </ol>
 *
 * <p>Each conversation has an id. Messages with the same id share history, so
 * follow-up questions like "and the day after?" work. History is kept in memory
 * (last 20 messages per conversation) and is lost on restart.
 */
@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant for weather and personal tasks.
            Today is %s. Use the available tools to answer; don't guess weather or task data.
            Keep answers short and friendly. Always confirm with the user before deleting a task.
            """;

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder, ChatMemory chatMemory,
                       ObjectProvider<SyncMcpToolCallbackProvider> mcpTools) {
        // Tools are looked up from the MCP server when a request is made, not at startup.
        mcpTools.ifAvailable(builder::defaultToolCallbacks);
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * Sends a message in a conversation.
     *
     * @param conversationId the conversation to continue, or null to start a new one
     */
    public Reply chat(String conversationId, String message) {
        String id = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT.formatted(LocalDate.now()))
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, id))
                .call()
                .content();

        return new Reply(id, answer);
    }

    public record Reply(String conversationId, String text) {}
}
