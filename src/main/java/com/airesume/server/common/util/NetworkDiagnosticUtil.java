package com.airesume.server.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 网络诊断工具类
 *
 * 所属模块：公共模块 - 工具类
 * 用途：提供网络连通性诊断功能，帮助排查 DNS 解析、端口绑定、代理等问题
 *
 * 【主要功能】
 * 1. DNS 解析检测 - 验证域名是否能正常解析
 * 2. 端口占用检测 - 检查本地端口是否被占用
 * 3. HTTP 连通性检测 - 验证能否访问指定 URL
 * 4. 代理配置检测 - 检查系统代理设置
 * 5. 完整诊断报告 - 一键生成所有检测项的报告
 *
 * @author AI Resume Team
 */
@Slf4j
public class NetworkDiagnosticUtil {

    /**
     * 执行完整网络诊断并返回报告
     *
     * @param targetUrl 目标 URL（用于连通性检测）
     * @return 诊断报告字符串
     */
    public static String runFullDiagnosis(String targetUrl) {
        StringBuilder report = new StringBuilder();
        report.append("========================================\n");
        report.append("      网络诊断报告\n");
        report.append("========================================\n\n");

        // 1. 基础环境信息
        report.append("【1】基础环境信息\n");
        report.append("----------------------------------------\n");
        report.append("操作系统: ").append(System.getProperty("os.name")).append("\n");
        report.append("操作系统版本: ").append(System.getProperty("os.version")).append("\n");
        report.append("Java 版本: ").append(System.getProperty("java.version")).append("\n");
        report.append("\n");

        // 2. 代理配置检测
        report.append("【2】代理配置检测\n");
        report.append("----------------------------------------\n");
        report.append(checkProxySettings());
        report.append("\n");

        // 3. DNS 解析检测
        report.append("【3】DNS 解析检测\n");
        report.append("----------------------------------------\n");
        if (targetUrl != null && !targetUrl.isBlank()) {
            try {
                String host = new URL(targetUrl).getHost();
                report.append(checkDnsResolution(host));
            } catch (Exception e) {
                report.append("URL 解析失败: ").append(e.getMessage()).append("\n");
            }
        }
        report.append("\n");

        // 4. HTTP 连通性检测
        report.append("【4】HTTP 连通性检测\n");
        report.append("----------------------------------------\n");
        if (targetUrl != null && !targetUrl.isBlank()) {
            report.append(checkHttpConnectivity(targetUrl));
        }
        report.append("\n");

        // 5. 常用端口检测
        report.append("【5】常用端口占用检测\n");
        report.append("----------------------------------------\n");
        report.append(checkCommonPorts());
        report.append("\n");

        report.append("========================================\n");
        report.append("      诊断完成\n");
        report.append("========================================\n");

        return report.toString();
    }

    /**
     * 检查代理配置
     *
     * @return 代理配置信息
     */
    public static String checkProxySettings() {
        StringBuilder sb = new StringBuilder();

        // 检查系统属性
        String httpProxy = System.getProperty("http.proxyHost");
        String httpsProxy = System.getProperty("https.proxyHost");
        String noProxy = System.getProperty("http.nonProxyHosts");

        sb.append("HTTP 代理主机: ").append(httpProxy != null ? httpProxy : "未设置").append("\n");
        if (httpProxy != null) {
            sb.append("HTTP 代理端口: ").append(System.getProperty("http.proxyPort", "未设置")).append("\n");
        }
        sb.append("HTTPS 代理主机: ").append(httpsProxy != null ? httpsProxy : "未设置").append("\n");
        if (httpsProxy != null) {
            sb.append("HTTPS 代理端口: ").append(System.getProperty("https.proxyPort", "未设置")).append("\n");
        }
        sb.append("不使用代理的主机: ").append(noProxy != null ? noProxy : "未设置").append("\n");

        // 检查环境变量
        String envHttpProxy = System.getenv("http_proxy");
        String envHttpsProxy = System.getenv("https_proxy");
        String envNoProxy = System.getenv("no_proxy");

        if (envHttpProxy != null || envHttpsProxy != null) {
            sb.append("\n【环境变量代理设置】\n");
            sb.append("http_proxy: ").append(envHttpProxy != null ? envHttpProxy : "未设置").append("\n");
            sb.append("https_proxy: ").append(envHttpsProxy != null ? envHttpsProxy : "未设置").append("\n");
            sb.append("no_proxy: ").append(envNoProxy != null ? envNoProxy : "未设置").append("\n");
        }

        // 检查是否有代理可能影响连接
        if (httpProxy != null || httpsProxy != null || envHttpProxy != null || envHttpsProxy != null) {
            sb.append("\n⚠️  【警告】检测到代理配置，可能影响 AI API 访问\n");
            sb.append("建议：尝试禁用代理或在 noProxy 中添加目标域名\n");
        }

        return sb.toString();
    }

    /**
     * 检查 DNS 解析
     *
     * @param host 主机名
     * @return DNS 解析结果
     */
    public static String checkDnsResolution(String host) {
        StringBuilder sb = new StringBuilder();
        sb.append("目标主机: ").append(host).append("\n");

        try {
            long start = System.currentTimeMillis();
            InetAddress[] addresses = InetAddress.getAllByName(host);
            long duration = System.currentTimeMillis() - start;

            sb.append("✅ DNS 解析成功 (耗时: ").append(duration).append("ms)\n");
            sb.append("解析到 ").append(addresses.length).append(" 个 IP 地址:\n");
            for (InetAddress addr : addresses) {
                sb.append("  - ").append(addr.getHostAddress());
                if (addr instanceof Inet4Address) {
                    sb.append(" (IPv4)");
                } else if (addr instanceof Inet6Address) {
                    sb.append(" (IPv6)");
                }
                sb.append("\n");
            }

            // 测试 ICMP ping（可能需要管理员权限）
            sb.append("\n连通性测试:\n");
            for (InetAddress addr : addresses) {
                try {
                    boolean reachable = addr.isReachable(3000);
                    sb.append("  ").append(addr.getHostAddress()).append(": ")
                            .append(reachable ? "✅ 可访问" : "❌ 不可访问 (ICMP)").append("\n");
                } catch (Exception e) {
                    sb.append("  ").append(addr.getHostAddress()).append(": ⚠️  ICMP 测试失败 (").append(e.getMessage()).append(")\n");
                }
            }

        } catch (UnknownHostException e) {
            sb.append("❌ DNS 解析失败: ").append(e.getMessage()).append("\n");
            sb.append("\n【建议排查步骤】\n");
            sb.append("1. 检查网络连接是否正常\n");
            sb.append("2. 检查 DNS 服务器配置\n");
            sb.append("3. 尝试使用 nslookup 或 ping 命令手动验证\n");
            sb.append("4. 检查是否有 VPN/代理/防火墙干扰\n");
            sb.append("5. 尝试在 JVM 参数中添加: -Djava.net.preferIPv4Stack=true\n");
        } catch (Exception e) {
            sb.append("❌ DNS 检测异常: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 检查 HTTP 连通性
     *
     * @param urlStr 目标 URL
     * @return HTTP 连通性检测结果
     */
    public static String checkHttpConnectivity(String urlStr) {
        StringBuilder sb = new StringBuilder();
        sb.append("目标 URL: ").append(urlStr).append("\n");

        try {
            URL url = new URL(urlStr);
            int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
            sb.append("协议: ").append(url.getProtocol()).append("\n");
            sb.append("主机: ").append(url.getHost()).append("\n");
            sb.append("端口: ").append(port).append("\n");
            sb.append("路径: ").append(url.getPath()).append("\n\n");

            // 测试 TCP 连接
            sb.append("TCP 连接测试:\n");
            try (Socket socket = new Socket()) {
                long start = System.currentTimeMillis();
                socket.connect(new InetSocketAddress(url.getHost(), port), 5000);
                long duration = System.currentTimeMillis() - start;
                sb.append("  ✅ TCP 连接成功 (耗时: ").append(duration).append("ms)\n");
            } catch (Exception e) {
                sb.append("  ❌ TCP 连接失败: ").append(e.getMessage()).append("\n");
                sb.append("\n【建议排查步骤】\n");
                sb.append("1. 检查目标服务器是否正常运行\n");
                sb.append("2. 检查防火墙是否阻止了 ").append(port).append(" 端口\n");
                sb.append("3. 检查公司网络/安全软件是否限制了出站连接\n");
            }

            // 测试 HTTP 请求（不发送实际数据，仅测试连接）
            sb.append("\nHTTP 握手测试:\n");
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                long start = System.currentTimeMillis();
                int responseCode = conn.getResponseCode();
                long duration = System.currentTimeMillis() - start;
                sb.append("  ✅ HTTP 请求成功 (响应码: ").append(responseCode).append(", 耗时: ").append(duration).append("ms)\n");
            } catch (Exception e) {
                sb.append("  ⚠️  HTTP 请求失败 (仅用于参考，AI API 需要认证): ").append(e.getMessage()).append("\n");
            }

        } catch (Exception e) {
            sb.append("❌ URL 解析或检测失败: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 检查常用端口是否被占用
     *
     * @return 端口检测结果
     */
    public static String checkCommonPorts() {
        StringBuilder sb = new StringBuilder();

        // 常用端口列表
        int[] ports = {8080, 80, 443, 3306, 6379, 5672};
        String[] portNames = {"Tomcat(8080)", "HTTP(80)", "HTTPS(443)", "MySQL(3306)", "Redis(6379)", "RabbitMQ(5672)"};

        for (int i = 0; i < ports.length; i++) {
            boolean inUse = isPortInUse(ports[i]);
            sb.append(portNames[i]).append(": ").append(inUse ? "🔴 已占用" : "🟢 可用").append("\n");
        }

        return sb.toString();
    }

    /**
     * 检查单个端口是否被占用
     *
     * @param port 端口号
     * @return true 表示端口被占用
     */
    public static boolean isPortInUse(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 执行系统命令（如 ping、nslookup）
     *
     * @param command 命令数组
     * @return 命令输出
     */
    public static List<String> executeSystemCommand(String[] command) {
        List<String> output = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }

            process.waitFor();
        } catch (Exception e) {
            output.add("命令执行失败: " + e.getMessage());
        }
        return output;
    }

    /**
     * 执行 ping 命令
     *
     * @param host 目标主机
     * @param count ping 次数
     * @return ping 输出
     */
    public static List<String> ping(String host, int count) {
        String os = System.getProperty("os.name").toLowerCase();
        String[] command;
        if (os.contains("win")) {
            command = new String[]{"ping", "-n", String.valueOf(count), host};
        } else {
            command = new String[]{"ping", "-c", String.valueOf(count), host};
        }
        return executeSystemCommand(command);
    }

    /**
     * 执行 nslookup 命令
     *
     * @param host 目标主机
     * @return nslookup 输出
     */
    public static List<String> nslookup(String host) {
        String os = System.getProperty("os.name").toLowerCase();
        String[] command;
        if (os.contains("win")) {
            command = new String[]{"nslookup", host};
        } else {
            command = new String[]{"nslookup", host};
        }
        return executeSystemCommand(command);
    }
}
