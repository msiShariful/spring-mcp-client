package com.islamshariful.spring_mcp_client.config;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sends the MCP server's API key with every request.
 *
 * <p>The server's weather tools are public, but its task tools and resources need
 * an API key. Spring AI calls this customizer before each HTTP request to the server,
 * so the key is added as {@code Authorization: Bearer <key>}.
 */
@Configuration
public class McpServerAuthConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerAuthConfig.class);

    @Bean
    McpSyncHttpClientRequestCustomizer mcpServerApiKey(@Value("${mcp.server.api-key:}") String apiKey) {
        if (apiKey.isBlank()) {
            log.warn("No MCP_SERVER_API_KEY set: only the server's public tools (weather) will work.");
            return (request, method, uri, body, context) -> { };
        }
        return (request, method, uri, body, context) -> request.header("Authorization", "Bearer " + apiKey);
    }
}
