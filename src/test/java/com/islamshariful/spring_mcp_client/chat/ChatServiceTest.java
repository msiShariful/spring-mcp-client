package com.islamshariful.spring_mcp_client.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests conversation memory with a fake chat model instead of calling Claude. */
class ChatServiceTest {

    /** Remembers every prompt it receives and answers "Answer 1", "Answer 2", ... */
    static class FakeChatModel implements ChatModel {
        final List<Prompt> prompts = new ArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Answer " + prompts.size()))));
        }

        String lastPromptText() {
            return prompts.getLast().getInstructions().stream().map(Message::getText).toList().toString();
        }
    }

    private final FakeChatModel model = new FakeChatModel();

    private final ChatService chatService = new ChatService(
            ChatClient.builder(model),
            MessageWindowChatMemory.builder().build(),
            new StaticListableBeanFactory().getBeanProvider(SyncMcpToolCallbackProvider.class)); // no MCP tools

    @Test
    void startsANewConversationWhenNoIdIsGiven() {
        ChatService.Reply reply = chatService.chat(null, "Hello");

        assertThat(reply.conversationId()).isNotBlank();
        assertThat(reply.text()).isEqualTo("Answer 1");
    }

    @Test
    void sameConversationIdKeepsHistory() {
        String id = chatService.chat(null, "Weather in Dhaka?").conversationId();

        chatService.chat(id, "And tomorrow?");

        assertThat(model.lastPromptText()).contains("Weather in Dhaka?", "Answer 1", "And tomorrow?");
    }

    @Test
    void differentConversationsDontShareHistory() {
        chatService.chat("conversation-a", "Secret plan A");

        chatService.chat("conversation-b", "Hello");

        assertThat(model.lastPromptText()).doesNotContain("Secret plan A");
    }

    @Test
    void systemPromptIncludesTodaysDate() {
        chatService.chat(null, "Hi");

        assertThat(model.lastPromptText()).contains(java.time.LocalDate.now().toString());
    }
}
