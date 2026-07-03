package com.airesume.server;

import com.airesume.server.common.util.NetworkDiagnosticUtil;

import java.util.List;
import java.util.Map;

/**
 * 网络诊断工具独立测试类
 *
 * 用途：直接测试 NetworkDiagnosticUtil 的各项功能，无需启动 Spring Boot 应用
 * 执行方式：在 server 目录下运行：mvn exec:java -Dexec.mainClass="com.airesume.server.NetworkDiagnosticTest"
 *
 * @author AI Resume Team
 */
public class NetworkDiagnosticTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   网络诊断工具测试");
        System.out.println("========================================\n");

        String targetUrl = "https://ark.cn-beijing.volces.com/api/coding/v3";

        // 1. 测试完整诊断
        System.out.println("【测试 1】执行完整网络诊断");
        System.out.println("----------------------------------------");
        String fullReport = NetworkDiagnosticUtil.runFullDiagnosis(targetUrl);
        System.out.println(fullReport);
        System.out.println();

        // 2. 测试 DNS 解析
        System.out.println("【测试 2】DNS 解析检测");
        System.out.println("----------------------------------------");
        String dnsResult = NetworkDiagnosticUtil.checkDnsResolution("ark.cn-beijing.volces.com");
        System.out.println(dnsResult);
        System.out.println();

        // 3. 测试代理配置
        System.out.println("【测试 3】代理配置检测");
        System.out.println("----------------------------------------");
        String proxyResult = NetworkDiagnosticUtil.checkProxySettings();
        System.out.println(proxyResult);
        System.out.println();

        // 4. 测试 HTTP 连通性
        System.out.println("【测试 4】HTTP 连通性检测");
        System.out.println("----------------------------------------");
        String httpResult = NetworkDiagnosticUtil.checkHttpConnectivity(targetUrl);
        System.out.println(httpResult);
        System.out.println();

        // 5. 测试端口检测
        System.out.println("【测试 5】常用端口占用检测");
        System.out.println("----------------------------------------");
        String portsResult = NetworkDiagnosticUtil.checkCommonPorts();
        System.out.println(portsResult);
        System.out.println();

        // 6. 测试单个端口检测
        System.out.println("【测试 6】单个端口检测 (8080)");
        System.out.println("----------------------------------------");
        boolean port8080 = NetworkDiagnosticUtil.isPortInUse(8080);
        System.out.println("端口 8080: " + (port8080 ? "已占用" : "可用"));
        System.out.println();

        // 7. 测试 ping 命令
        System.out.println("【测试 7】执行 ping 命令");
        System.out.println("----------------------------------------");
        List<String> pingResult = NetworkDiagnosticUtil.ping("ark.cn-beijing.volces.com", 2);
        for (String line : pingResult) {
            System.out.println(line);
        }
        System.out.println();

        // 8. 测试 nslookup 命令
        System.out.println("【测试 8】执行 nslookup 命令");
        System.out.println("----------------------------------------");
        List<String> nslookupResult = NetworkDiagnosticUtil.nslookup("ark.cn-beijing.volces.com");
        for (String line : nslookupResult) {
            System.out.println(line);
        }
        System.out.println();

        System.out.println("========================================");
        System.out.println("   所有测试完成");
        System.out.println("========================================");
    }
}
