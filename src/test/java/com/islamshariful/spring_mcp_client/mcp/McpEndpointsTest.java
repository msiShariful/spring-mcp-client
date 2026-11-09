package com.islamshariful.spring_mcp_client.mcp;

import com.islamshariful.spring_mcp_client.chat.ChatService;
import com.islamshariful.spring_mcp_client.mcp.McpExplorerService.ResourceContent;
import com.islamshariful.spring_mcp_client.mcp.McpExplorerService.ToolInfo;
import com.islamshariful.spring_mcp_client.prompts.PromptController;
import io.modelcontextprotocol.spec.McpError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests the MCP explorer and prompt endpoints with the MCP server mocked out. */
@WebMvcTest({McpExplorerController.class, PromptController.class})
class McpEndpointsTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private McpExplorerService explorer;

    @MockitoBean
    private ChatService chatService;

    @Test
    void listsTools() throws Exception {
        when(explorer.tools()).thenReturn(List.of(
                new ToolInfo("get_forecast", "Daily forecast", true, false),
                new ToolInfo("delete_task", "Delete a task", false, true)));

        mvc.perform(get("/api/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("get_forecast"))
                .andExpect(jsonPath("$[1].destructive").value(true));
    }

    @Test
    void readsAResource() throws Exception {
        when(explorer.readResource("weather://Dhaka"))
                .thenReturn(new ResourceContent("weather://Dhaka", "text/markdown", "# Weather in Dhaka"));

        mvc.perform(get("/api/mcp/resources/read").param("uri", "weather://Dhaka"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("# Weather in Dhaka"));
    }

    @Test
    void mcpServerErrorsBecome502() throws Exception {
        when(explorer.readResource("tasks://open"))
                .thenThrow(McpError.builder(-32603).message("Authentication required").build());

        mvc.perform(get("/api/mcp/resources/read").param("uri", "tasks://open"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("MCP server error: Authentication required"));
    }

    @Test
    void runsAServerPromptThroughClaude() throws Exception {
        when(explorer.renderPrompt("plan_my_day", Map.of("city", "Dhaka"))).thenReturn("Help me plan my day in Dhaka.");
        when(chatService.chat(any(), any())).thenReturn(new ChatService.Reply("abc-123", "Here's your plan..."));

        mvc.perform(post("/api/prompts/plan_my_day").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"arguments\": {\"city\": \"Dhaka\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt").value("Help me plan my day in Dhaka."))
                .andExpect(jsonPath("$.reply").value("Here's your plan..."))
                .andExpect(jsonPath("$.conversationId").value("abc-123"));
    }
}
