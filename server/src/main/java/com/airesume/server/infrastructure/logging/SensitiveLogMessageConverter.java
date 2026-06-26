package com.airesume.server.infrastructure.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

/**
 * Logback 消息转换器。
 * 将格式化消息和异常文本统一交给 {@link SensitiveLogMasker}，保证控制台、全量日志和错误日志一致脱敏。
 */
public class SensitiveLogMessageConverter extends ClassicConverter {

    private static final int MAX_THROWABLE_DEPTH = 16;

    @Override
    public String convert(ILoggingEvent event) {
        if (event == null) {
            return "";
        }

        String maskedMessage = SensitiveLogMasker.mask(event.getFormattedMessage());
        StringBuilder builder = new StringBuilder(maskedMessage == null ? "" : maskedMessage);
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            builder.append(System.lineSeparator());
            appendThrowable(builder, throwableProxy, "", 0);
        }
        return builder.toString();
    }

    private void appendThrowable(StringBuilder builder, IThrowableProxy throwableProxy, String prefix, int depth) {
        if (throwableProxy == null || depth >= MAX_THROWABLE_DEPTH) {
            return;
        }

        // 异常 message 也可能携带签名 URL 或上游请求体，必须经过同一个脱敏入口。
        builder.append(prefix)
                .append(throwableProxy.getClassName())
                .append(": ")
                .append(SensitiveLogMasker.mask(throwableProxy.getMessage()))
                .append(System.lineSeparator());

        StackTraceElementProxy[] stackTraceElements = throwableProxy.getStackTraceElementProxyArray();
        if (stackTraceElements != null) {
            for (StackTraceElementProxy stackTraceElement : stackTraceElements) {
                builder.append("\tat ")
                        .append(SensitiveLogMasker.mask(stackTraceElement.toString()))
                        .append(System.lineSeparator());
            }
        }

        IThrowableProxy[] suppressedExceptions = throwableProxy.getSuppressed();
        if (suppressedExceptions != null) {
            for (IThrowableProxy suppressed : suppressedExceptions) {
                appendThrowable(builder, suppressed, "Suppressed: ", depth + 1);
            }
        }
        appendThrowable(builder, throwableProxy.getCause(), "Caused by: ", depth + 1);
    }
}
