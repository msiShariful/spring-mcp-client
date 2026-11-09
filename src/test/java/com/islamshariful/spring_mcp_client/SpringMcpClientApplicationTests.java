package com.islamshariful.spring_mcp_client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Checks that the app starts. The MCP connection is turned off and a fake Claude key is used,
 * so this test runs without the MCP server or an Anthropic account (e.g. in CI).
 */
@SpringBootTest(properties = {
        "spring.ai.mcp.client.enabled=false",
        "spring.ai.anthropic.api-key=test-key"
})
class SpringMcpClientApplicationTests {

    @Test
    void contextLoads() {
    }
}
