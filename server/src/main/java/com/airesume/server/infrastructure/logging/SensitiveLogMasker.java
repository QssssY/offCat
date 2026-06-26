package com.airesume.server.infrastructure.logging;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 日志敏感信息脱敏工具。
 * 用于在日志最终输出前统一清理签名 URL、鉴权凭据和 AI 请求正文，避免敏感内容落盘。
 */
public final class SensitiveLogMasker {

    private static final String MASK = "***";
    private static final Set<String> SENSITIVE_JSON_KEYS = Set.of(
            "authorization",
            "apikey",
            "api_key",
            "api-key",
            "x-api-key",
            "x-goog-api-key",
            "accesskey",
            "accesskeyid",
            "accesskeysecret",
            "access-key-id",
            "access-key-secret",
            "ossaccesskeyid",
            "signature",
            "expires",
            "security-token",
            "messages",
            "prompt",
            "system",
            "systemprompt",
            "userprompt",
            "resumetext",
            "diagnosisresult",
            "conversation",
            "conversationtext",
            "chathistory"
    );

    private static final Pattern URL_SECRET_PARAMETER_PATTERN = Pattern.compile(
            "(?i)(\\b(?:OSSAccessKeyId|Signature|Expires|security-token)=)([^&\\s,]*)");
    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile(
            "(?i)(\\bAuthorization\\s*[:=]\\s*)(?:Bearer\\s+)?([^\\s,;}\"]+)");
    private static final Pattern PLAIN_SECRET_FIELD_PATTERN = Pattern.compile(
            "(?i)(\\b(?:apiKey|api_key|api-key|x-api-key|x-goog-api-key|AccessKey|AccessKeyId|AccessKeySecret|access-key-id|access-key-secret)\\s*[:=]\\s*)([^\\s,;}&\"]+)");
    private static final Pattern PLAIN_MESSAGES_PATTERN = Pattern.compile(
            "(?is)(\\bmessages\\s*[:=]\\s*)\\[[^\\]]*]");

    private SensitiveLogMasker() {
    }

    /**
     * 对单条日志消息执行脱敏。
     *
     * @param message 原始日志消息
     * @return 脱敏后的日志消息，null 会原样返回
     */
    public static String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // 先处理 URL/Header/普通 key-value，再扫描 JSON 字段，覆盖大部分日志落盘形态。
        String masked = URL_SECRET_PARAMETER_PATTERN.matcher(message).replaceAll("$1" + MASK);
        masked = AUTHORIZATION_PATTERN.matcher(masked).replaceAll("$1" + MASK);
        masked = PLAIN_SECRET_FIELD_PATTERN.matcher(masked).replaceAll("$1" + MASK);
        masked = PLAIN_MESSAGES_PATTERN.matcher(masked).replaceAll("$1\"" + MASK + "\"");
        return maskSensitiveJsonFields(masked);
    }

    private static String maskSensitiveJsonFields(String message) {
        StringBuilder builder = new StringBuilder(message.length());
        int index = 0;

        while (index < message.length()) {
            int quoteStart = message.indexOf('"', index);
            if (quoteStart < 0) {
                builder.append(message, index, message.length());
                break;
            }

            int quoteEnd = findStringEnd(message, quoteStart);
            if (quoteEnd < 0) {
                builder.append(message, index, message.length());
                break;
            }

            int afterKey = skipWhitespace(message, quoteEnd + 1);
            if (afterKey < message.length()
                    && message.charAt(afterKey) == ':'
                    && isSensitiveJsonKey(message.substring(quoteStart + 1, quoteEnd))) {
                builder.append(message, index, quoteEnd + 1);
                builder.append(message, quoteEnd + 1, afterKey + 1);

                int valueStart = skipWhitespace(message, afterKey + 1);
                builder.append(message, afterKey + 1, valueStart);
                builder.append('"').append(MASK).append('"');
                index = findJsonValueEnd(message, valueStart);
            } else {
                builder.append(message, index, quoteEnd + 1);
                index = quoteEnd + 1;
            }
        }

        return builder.toString();
    }

    private static boolean isSensitiveJsonKey(String key) {
        return SENSITIVE_JSON_KEYS.contains(key.toLowerCase(Locale.ROOT));
    }

    private static int skipWhitespace(String message, int index) {
        int cursor = index;
        while (cursor < message.length() && Character.isWhitespace(message.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static int findStringEnd(String message, int quoteStart) {
        boolean escaped = false;
        for (int i = quoteStart + 1; i < message.length(); i++) {
            char current = message.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                return i;
            }
        }
        return -1;
    }

    private static int findJsonValueEnd(String message, int valueStart) {
        if (valueStart >= message.length()) {
            return valueStart;
        }

        char first = message.charAt(valueStart);
        if (first == '"') {
            int stringEnd = findStringEnd(message, valueStart);
            return stringEnd < 0 ? message.length() : stringEnd + 1;
        }
        if (first == '{' || first == '[') {
            return findContainerEnd(message, valueStart);
        }

        int cursor = valueStart;
        while (cursor < message.length()) {
            char current = message.charAt(cursor);
            if (current == ',' || current == '}' || current == ']' || Character.isWhitespace(current)) {
                return cursor;
            }
            cursor++;
        }
        return message.length();
    }

    private static int findContainerEnd(String message, int valueStart) {
        int objectDepth = 0;
        int arrayDepth = 0;
        for (int i = valueStart; i < message.length(); i++) {
            char current = message.charAt(i);
            if (current == '"') {
                int stringEnd = findStringEnd(message, i);
                if (stringEnd < 0) {
                    return message.length();
                }
                i = stringEnd;
                continue;
            }
            if (current == '{') {
                objectDepth++;
            } else if (current == '}') {
                objectDepth--;
            } else if (current == '[') {
                arrayDepth++;
            } else if (current == ']') {
                arrayDepth--;
            }
            if (objectDepth == 0 && arrayDepth == 0) {
                return i + 1;
            }
        }
        return message.length();
    }
}
