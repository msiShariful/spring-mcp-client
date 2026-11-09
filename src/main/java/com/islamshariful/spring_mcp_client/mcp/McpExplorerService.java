package com.islamshariful.spring_mcp_client.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ResourceContents;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Talks to the MCP server directly (no AI involved) to show what it offers:
 * server info, tools, resources and prompts. Handy for learning and debugging.
 */
@Service
public class McpExplorerService {

    private final ObjectProvider<List<McpSyncClient>> clients;

    public McpExplorerService(ObjectProvider<List<McpSyncClient>> clients) {
        this.clients = clients;
    }

    public ServerInfo serverInfo() {
        McpSyncClient server = server();
        return new ServerInfo(server.getServerInfo().name(), server.getServerInfo().version(),
                server.getServerInstructions());
    }

    public List<ToolInfo> tools() {
        return server().listTools().tools().stream().map(ToolInfo::from).toList();
    }

    public ResourcesInfo resources() {
        McpSyncClient server = server();
        List<ResourceInfo> resources = server.listResources().resources().stream()
                .map(r -> new ResourceInfo(r.uri(), r.name(), r.description()))
                .toList();
        List<ResourceInfo> templates = server.listResourceTemplates().resourceTemplates().stream()
                .map(t -> new ResourceInfo(t.uriTemplate(), t.name(), t.description()))
                .toList();
        return new ResourcesInfo(resources, templates);
    }

    /** Reads a resource, e.g. {@code weather://Dhaka} or {@code tasks://open}. */
    public ResourceContent readResource(String uri) {
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("uri can't be empty, e.g. weather://Dhaka");
        }
        ResourceContents contents = server().readResource(new ReadResourceRequest(uri)).contents().getFirst();
        String text = (contents instanceof TextResourceContents textContents) ? textContents.text() : "(binary content)";
        return new ResourceContent(contents.uri(), contents.mimeType(), text);
    }

    public List<PromptInfo> prompts() {
        return server().listPrompts().prompts().stream()
                .map(p -> new PromptInfo(p.name(), p.description(),
                        p.arguments() == null ? List.of() : p.arguments().stream()
                                .map(a -> new PromptArgumentInfo(a.name(), a.description(), Boolean.TRUE.equals(a.required())))
                                .toList()))
                .toList();
    }

    /** This client connects to one MCP server (spring-mcp-server). */
    McpSyncClient server() {
        List<McpSyncClient> connected = clients.getIfAvailable(List::of);
        if (connected.isEmpty()) {
            throw new IllegalStateException("No MCP server connected. Check spring.ai.mcp.client settings.");
        }
        return connected.getFirst();
    }

    // ---- What the endpoints return ----

    public record ServerInfo(String name, String version, String instructions) {}

    public record ToolInfo(String name, String description, boolean readOnly, boolean destructive) {
        static ToolInfo from(Tool tool) {
            ToolAnnotations hints = tool.annotations();
            return new ToolInfo(tool.name(), tool.description(),
                    hints != null && Boolean.TRUE.equals(hints.readOnlyHint()),
                    hints != null && Boolean.TRUE.equals(hints.destructiveHint()));
        }
    }

    public record ResourceInfo(String uri, String name, String description) {}

    public record ResourcesInfo(List<ResourceInfo> resources, List<ResourceInfo> templates) {}

    public record ResourceContent(String uri, String mimeType, String text) {}

    public record PromptArgumentInfo(String name, String description, boolean required) {}

    public record PromptInfo(String name, String description, List<PromptArgumentInfo> arguments) {}
}
