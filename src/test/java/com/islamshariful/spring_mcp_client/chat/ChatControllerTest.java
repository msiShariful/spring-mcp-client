package com.islamshariful.spring_mcp_client.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void returnsReplyAndConversationId() throws Exception {
        when(chatService.chat(any(), eq("Weather in Dhaka?")))
                .thenReturn(new ChatService.Reply("abc-123", "It's sunny in Dhaka."));

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Weather in Dhaka?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("abc-123"))
                .andExpect(jsonPath("$.reply").value("It's sunny in Dhaka."));
    }

    @Test
    void continuesAConversation() throws Exception {
        when(chatService.chat("abc-123", "And tomorrow?"))
                .thenReturn(new ChatService.Reply("abc-123", "Rain tomorrow."));

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conversationId\": \"abc-123\", \"message\": \"And tomorrow?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Rain tomorrow."));
    }

    @Test
    void rejectsEmptyMessage() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("message can't be empty"));

        verifyNoInteractions(chatService);
    }
}
