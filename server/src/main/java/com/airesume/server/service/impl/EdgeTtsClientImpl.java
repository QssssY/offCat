package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.service.EdgeTtsClient;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Edge Read Aloud WebSocket 协议实现。
 * <p>
 * EdgeTTS 没有项目内 API Key，实际音频通过 Microsoft Edge 在线朗读通道返回；
 * 本实现只把协议细节封装在单一客户端中，调用方继续复用现有云端 TTS 播放链路。
 */
@Slf4j
@Service
public class EdgeTtsClientImpl implements EdgeTtsClient {

    private static final String TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4";
    private static final String SEC_MS_GEC_VERSION = "1-143.0.3650.75";
    private static final String EDGE_WSS_ENDPOINT =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1";
    private static final String EDGE_ORIGIN = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold";
    private static final String EDGE_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0";
    private static final String OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3";
    private static final long WINDOWS_EPOCH_OFFSET_SECONDS = 11_644_473_600L;
    private static final long GEC_ROUND_SECONDS = 300L;
    private static final long WINDOWS_TICKS_PER_SECOND = 10_000_000L;
    private static final DateTimeFormatter EDGE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US)
                    .withZone(ZoneOffset.UTC);

    @Override
    public byte[] synthesize(String text, String voiceId, Duration timeout) {
        String normalizedText = trimToNull(text);
        String normalizedVoice = trimToNull(voiceId);
        if (normalizedText == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "TTS 文本不能为空");
        }
        if (normalizedVoice == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "EdgeTTS 音色不能为空");
        }

        Duration effectiveTimeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? Duration.ofSeconds(15)
                : timeout;
        EdgeWebSocketListener listener = new EdgeWebSocketListener();
        WebSocket webSocket = null;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(effectiveTimeout)
                    .build();
            webSocket = openWebSocket(client, listener, effectiveTimeout, Instant.now());

            String requestId = UUID.randomUUID().toString().replace("-", "");
            webSocket.sendText(buildSpeechConfigMessage(), true).join();
            webSocket.sendText(buildSsmlMessage(requestId, normalizedVoice, normalizedText), true).join();
            byte[] audioBytes = listener.awaitAudio(effectiveTimeout);
            if (audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "EdgeTTS 返回音频为空");
            }
            return audioBytes;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            Throwable rootCause = unwrapAsyncFailure(ex);
            log.warn("EdgeTTS 合成失败, voice: {}, errorType: {}, rootCauseType: {}, rootCause: {}",
                    normalizedVoice,
                    ex.getClass().getSimpleName(),
                    rootCause.getClass().getSimpleName(),
                    rootCause.getMessage());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, resolveFailureMessage(rootCause));
        } finally {
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            }
        }
    }

    private WebSocket openWebSocket(HttpClient client, EdgeWebSocketListener listener,
                                    Duration effectiveTimeout, Instant requestInstant) throws Exception {
        try {
            return buildWebSocket(client, listener, effectiveTimeout, requestInstant);
        } catch (ExecutionException ex) {
            Throwable rootCause = unwrapAsyncFailure(ex);
            Optional<Instant> retryInstant = resolveForbiddenHandshakeServerDate(rootCause);
            if (retryInstant.isPresent()) {
                log.warn("EdgeTTS 首次握手 403，按上游 Date 重算 Sec-MS-GEC 后重试一次, serverDate: {}",
                        retryInstant.get());
                return buildWebSocket(client, listener, effectiveTimeout, retryInstant.get());
            }
            throw ex;
        }
    }

    private WebSocket buildWebSocket(HttpClient client, EdgeWebSocketListener listener,
                                     Duration effectiveTimeout, Instant requestInstant) throws Exception {
        WebSocket.Builder webSocketBuilder = client.newWebSocketBuilder();
        buildWebSocketHeaders().forEach(webSocketBuilder::header);
        return webSocketBuilder.buildAsync(buildWebSocketUri(requestInstant), listener)
                .get(effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    Map<String, String> buildWebSocketHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Pragma", "no-cache");
        headers.put("Cache-Control", "no-cache");
        headers.put("Origin", EDGE_ORIGIN);
        headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("User-Agent", EDGE_USER_AGENT);
        // Edge Read Aloud 上游会把 MUID 作为浏览器会话标识之一；每次请求生成新值，避免复用固定指纹。
        headers.put("Cookie", "muid=" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT) + ";");
        return headers;
    }

    URI buildWebSocketUri(Instant now) {
        String connectionId = UUID.randomUUID().toString().replace("-", "");
        String secMsGec = generateSecMsGec(now == null ? Instant.now() : now);
        return URI.create(EDGE_WSS_ENDPOINT
                + "?TrustedClientToken=" + TRUSTED_CLIENT_TOKEN
                + "&Sec-MS-GEC=" + secMsGec
                + "&Sec-MS-GEC-Version=" + SEC_MS_GEC_VERSION
                + "&ConnectionId=" + connectionId);
    }

    String buildSpeechConfigMessage() {
        return "X-Timestamp:" + formatEdgeTimestamp(Instant.now()) + "\r\n"
                + "Content-Type:application/json; charset=utf-8\r\n"
                + "Path:speech.config\r\n\r\n"
                + "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{"
                + "\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},"
                + "\"outputFormat\":\"" + OUTPUT_FORMAT + "\"}}}}";
    }

    String buildSsmlMessage(String requestId, String voiceId, String text) {
        return "X-RequestId:" + requestId + "\r\n"
                + "Content-Type:application/ssml+xml\r\n"
                + "X-Timestamp:" + formatEdgeTimestamp(Instant.now()) + "\r\n"
                + "Path:ssml\r\n\r\n"
                + "<speak version='1.0' xml:lang='zh-CN'>"
                + "<voice name='" + escapeXml(voiceId) + "'>"
                + escapeXml(text)
                + "</voice></speak>";
    }

    static byte[] extractAudioPayload(ByteBuffer data) {
        byte[] frame = new byte[data.remaining()];
        data.get(frame);
        if (frame.length <= 2) {
            return new byte[0];
        }
        int headerLength = ((frame[0] & 0xff) << 8) | (frame[1] & 0xff);
        int audioStart = headerLength + 2;
        if (headerLength <= 0 || audioStart > frame.length) {
            return new byte[0];
        }
        String header = new String(frame, 2, Math.min(headerLength, frame.length - 2), StandardCharsets.UTF_8);
        if (!header.contains("Path:audio") || audioStart == frame.length) {
            return new byte[0];
        }
        return Arrays.copyOfRange(frame, audioStart, frame.length);
    }

    static String generateSecMsGec(Instant now) {
        long unixSeconds = now == null ? Instant.now().getEpochSecond() : now.getEpochSecond();
        long roundedWindowsTicks = ((unixSeconds + WINDOWS_EPOCH_OFFSET_SECONDS) / GEC_ROUND_SECONDS)
                * GEC_ROUND_SECONDS
                * WINDOWS_TICKS_PER_SECOND;
        return sha256Hex(roundedWindowsTicks + TRUSTED_CLIENT_TOKEN);
    }

    static Throwable unwrapAsyncFailure(Throwable error) {
        Throwable current = error;
        while ((current instanceof ExecutionException || current instanceof CompletionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null ? error : current;
    }

    static Optional<Instant> resolveForbiddenHandshakeServerDate(Throwable rootCause) {
        if (!(rootCause instanceof WebSocketHandshakeException handshakeException)
                || handshakeException.getResponse().statusCode() != 403) {
            return Optional.empty();
        }
        return handshakeException.getResponse().headers().firstValue("Date").flatMap(date -> {
            try {
                return Optional.of(DateTimeFormatter.RFC_1123_DATE_TIME.parse(date, Instant::from));
            } catch (DateTimeParseException ex) {
                return Optional.empty();
            }
        });
    }

    private String resolveFailureMessage(Throwable rootCause) {
        String message = rootCause == null ? "" : String.valueOf(rootCause.getMessage()).toLowerCase(Locale.ROOT);
        if (message.contains("403") || message.contains("forbidden") || message.contains("invalid response status")) {
            return "EdgeTTS 上游拒绝连接，可能被限流或协议已变更，请稍后重试或切换其它 TTS";
        }
        return "EdgeTTS 合成失败，请稍后重试";
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.US_ASCII));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String formatEdgeTimestamp(Instant instant) {
        return EDGE_TIMESTAMP_FORMATTER.format(instant == null ? Instant.now() : instant);
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class EdgeWebSocketListener implements WebSocket.Listener {
        private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        private final StringBuilder textBuffer = new StringBuilder();
        private final CompletableFuture<byte[]> audioFuture = new CompletableFuture<>();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                String message = textBuffer.toString();
                textBuffer.setLength(0);
                if (message.contains("Path:turn.end")) {
                    audioFuture.complete(audioBuffer.toByteArray());
                }
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] payload = extractAudioPayload(data);
            if (payload.length > 0) {
                audioBuffer.writeBytes(payload);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            audioFuture.completeExceptionally(error);
        }

        private byte[] awaitAudio(Duration timeout) throws Exception {
            return audioFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
