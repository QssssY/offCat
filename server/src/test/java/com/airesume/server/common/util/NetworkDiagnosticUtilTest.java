package com.airesume.server.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NetworkDiagnosticUtilTest {

    @Test
    void executeSystemCommandShouldReturnOutput() {
        List<String> output = NetworkDiagnosticUtil.executeSystemCommand(
                new String[]{"cmd", "/c", "echo", "hello-test"}
        );
        assertFalse(output.isEmpty());
        assertTrue(output.stream().anyMatch(line -> line.contains("hello-test")),
                "输出应包含 hello-test, 实际: " + output);
    }

    @Test
    void executeSystemCommandShouldNotThrowOnNonExistentCommand() {
        List<String> output = NetworkDiagnosticUtil.executeSystemCommand(
                new String[]{"nonexistent-command-xyz"}
        );
        assertTrue(output.stream().anyMatch(line -> line.contains("失败")));
    }

    @Test
    void checkHttpConnectivityShouldNotThrow() {
        String result = NetworkDiagnosticUtil.checkHttpConnectivity("https://www.baidu.com");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }
}
