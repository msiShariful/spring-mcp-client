package com.islamshariful.spring_mcp_client.prompts;

import com.islamshariful.spring_mcp_client.chat.ChatService;
import com.islamshariful.spring_mcp_client.mcp.McpExplorerService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Runs one of the MCP server's prompts (ready-made workflows) through Claude.
 *
 * <pre>
 * POST /api/prompts/plan_my_day
 * {"arguments": {"city": "Dhaka"}}
 * </pre>
 *
 * Steps: get the filled-in prompt from the MCP server, then send it to Claude as a
 * chat message. Claude follows it and calls the server's tools as needed.
 * The reply includes a {@code conversationId} so you can continue in {@code /api/chat}.
 */
@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final McpExplorerService explorer;
    private final ChatService chatService;

    public PromptController(McpExplorerService explorer, ChatService chatService) {
        this.explorer = explorer;
        this.chatService = chatService;
    }

    @PostMapping("/{name}")
    public PromptRunResponse run(@PathVariable String name, @RequestBody(required = false) PromptRunRequest request) {
        Map<String, String> arguments = (request == null) ? Map.of() : request.arguments();
        String conversationId = (request == null) ? null : request.conversationId();

        String promptText = explorer.renderPrompt(name, arguments);
        ChatService.Reply reply = chatService.chat(conversationId, promptText);

        return new PromptRunResponse(reply.conversationId(), promptText, reply.text());
    }

    public record PromptRunRequest(String conversationId, Map<String, String> arguments) {
        public PromptRunRequest {
            arguments = (arguments == null) ? Map.of() : arguments;
        }
    }

    /** {@code prompt} is the text the server generated, so you can see what Claude was asked. */
    public record PromptRunResponse(String conversationId, String prompt, String reply) {}
}
