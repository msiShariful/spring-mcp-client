package com.islamshariful.spring_mcp_client.mcp;

import com.islamshariful.spring_mcp_client.mcp.McpExplorerService.PromptInfo;
import com.islamshariful.spring_mcp_client.mcp.McpExplorerService.ResourceContent;
import com.islamshariful.spring_mcp_client.mcp.McpExplorerService.ResourcesInfo;
import com.islamshariful.spring_mcp_client.mcp.McpExplorerService.ServerInfo;
import com.islamshariful.spring_mcp_client.mcp.McpExplorerService.ToolInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * See what the MCP server offers, without involving the AI.
 *
 * <pre>
 * GET /api/mcp/server                              server name, version, instructions
 * GET /api/mcp/tools                               tools with safety hints
 * GET /api/mcp/resources                           resources and resource templates
 * GET /api/mcp/resources/read?uri=weather://Dhaka  read one resource
 * GET /api/mcp/prompts                             prompts and their arguments
 * </pre>
 */
@RestController
@RequestMapping("/api/mcp")
public class McpExplorerController {

    private final McpExplorerService explorer;

    public McpExplorerController(McpExplorerService explorer) {
        this.explorer = explorer;
    }

    @GetMapping("/server")
    public ServerInfo server() {
        return explorer.serverInfo();
    }

    @GetMapping("/tools")
    public List<ToolInfo> tools() {
        return explorer.tools();
    }

    @GetMapping("/resources")
    public ResourcesInfo resources() {
        return explorer.resources();
    }

    @GetMapping("/resources/read")
    public ResourceContent readResource(@RequestParam String uri) {
        return explorer.readResource(uri);
    }

    @GetMapping("/prompts")
    public List<PromptInfo> prompts() {
        return explorer.prompts();
    }
}
