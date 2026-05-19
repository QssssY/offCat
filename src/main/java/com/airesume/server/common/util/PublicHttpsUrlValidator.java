package com.airesume.server.common.util;

import java.net.URI;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 公网 HTTPS 地址校验工具。
 *
 * <p>用于管理端 AI 引擎配置和运行时 AI 调用入口，避免业务层各自维护不同的 SSRF 防护规则。</p>
 */
public final class PublicHttpsUrlValidator {

    private static final Pattern IPV4_PATTERN = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}");

    private PublicHttpsUrlValidator() {
    }

    /**
     * 校验并返回 trim 后的公网 HTTPS URL。
     *
     * @param url 待校验的 URL
     * @param emptyMessage 空值错误信息
     * @return trim 后的 URL
     */
    public static String validate(String url, String emptyMessage) {
        return validate(url, emptyMessage, InetAddress::getAllByName);
    }

    static String validate(String url, String emptyMessage, HostResolver hostResolver) {
        String normalized = url == null ? null : url.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }

        URI uri = URI.create(normalized);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(scheme) || host == null || host.isBlank()) {
            throw new IllegalArgumentException("基础地址只允许 https:// 公网地址");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("基础地址不允许携带用户信息");
        }

        String normalizedHost = normalizeHost(host);
        if (isBlockedHost(normalizedHost)) {
            throw new IllegalArgumentException("基础地址不允许指向本机、内网或云元数据地址");
        }
        if (hasBlockedResolvedAddress(normalizedHost, hostResolver)) {
            throw new IllegalArgumentException("基础地址域名解析结果不允许指向本机、内网或云元数据地址");
        }

        return normalized;
    }

    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private static String normalizeHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean isBlockedHost(String host) {
        if ("localhost".equals(host) || host.endsWith(".localhost") || "metadata.google.internal".equals(host)) {
            return true;
        }
        if (IPV4_PATTERN.matcher(host).matches()) {
            return isBlockedIpv4(host);
        }
        // IPv6 字面量不需要 DNS 解析即可识别，重点拦截本机、链路本地、唯一本地地址和 IPv4 映射内网地址。
        if (host.contains(":")) {
            return "::1".equals(host)
                    || host.startsWith("fc")
                    || host.startsWith("fd")
                    || host.startsWith("fe80:")
                    || host.endsWith(":127.0.0.1")
                    || host.endsWith(":169.254.169.254")
                    || isBlockedMappedIpv4(host);
        }
        return false;
    }

    private static boolean hasBlockedResolvedAddress(String host, HostResolver hostResolver) {
        InetAddress[] addresses;
        try {
            addresses = hostResolver.resolve(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("基础地址域名无法解析");
        }
        if (addresses == null || addresses.length == 0) {
            throw new IllegalArgumentException("基础地址域名无法解析");
        }
        for (InetAddress address : addresses) {
            // 域名可能解析到内网、回环或云元数据地址，必须在服务端出网前拦截，避免 SSRF。
            if (isBlockedAddress(address)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        String hostAddress = normalizeHost(address.getHostAddress());
        return isBlockedHost(hostAddress);
    }

    private static boolean isBlockedIpv4(String host) {
        String[] parts = host.split("\\.");
        int first = parseIpv4Part(parts[0]);
        int second = parseIpv4Part(parts[1]);
        if (first < 0 || second < 0 || parseIpv4Part(parts[2]) < 0 || parseIpv4Part(parts[3]) < 0) {
            return true;
        }
        return first == 0
                || first == 10
                || first == 127
                || first == 169 && second == 254
                || first == 100 && (second >= 64 && second <= 127 || second == 100)
                || first == 172 && second >= 16 && second <= 31
                || first == 192 && second == 168;
    }

    private static boolean isBlockedMappedIpv4(String host) {
        int lastColonIndex = host.lastIndexOf(':');
        if (lastColonIndex < 0 || lastColonIndex == host.length() - 1) {
            return false;
        }
        String mappedIpv4 = host.substring(lastColonIndex + 1);
        return IPV4_PATTERN.matcher(mappedIpv4).matches() && isBlockedIpv4(mappedIpv4);
    }

    private static int parseIpv4Part(String part) {
        try {
            int value = Integer.parseInt(part);
            return value >= 0 && value <= 255 ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
