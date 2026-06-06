package com.airesume.server.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.config.OssConfig;
import com.aliyun.oss.OSS;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * OSS 服务安全日志回归测试。
 * 重点验证 OSS 凭据错误和配置错误不会把 AccessKey 原文写入日志或异常消息。
 */
class OssServiceImplTest {

    private static final String ACCESS_KEY_ID = "LTAI5tSensitiveKey7890";
    private static final String ACCESS_KEY_SECRET = "aliyun-access-key-secret-value";

    @Test
    void shouldRejectEnabledOssWithMissingRequiredConfig() {
        OssConfig config = new OssConfig();
        config.setEnabled(true);
        config.setEndpoint(" ");
        config.setBucketName("community-bucket");
        config.setAccessKeyId(ACCESS_KEY_ID);
        config.setAccessKeySecret("");
        OssServiceImpl service = new OssServiceImpl(config);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.generateSignedUrl("community/1/20260605/abcdef.jpg"));

        assertThat(exception.getMessage()).contains("OSS 配置缺失");
        assertThat(exception.getMessage()).contains("endpoint");
        assertThat(exception.getMessage()).contains("accessKeySecret");
        assertThat(exception.getMessage()).doesNotContain(ACCESS_KEY_ID);
        assertThat(exception.getMessage()).doesNotContain(ACCESS_KEY_SECRET);
    }

    @Test
    void shouldMaskAccessKeyWhenClientInitializationFails() {
        OssConfig config = validConfig();
        FailingOssServiceImpl service = new FailingOssServiceImpl(config);
        Logger logger = (Logger) LoggerFactory.getLogger(OssServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.generateSignedUrl("community/1/20260605/abcdef.jpg"));

            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(exception.getMessage()).contains("OSS 客户端初始化失败");
            assertThat(exception.getMessage()).doesNotContain(ACCESS_KEY_ID);
            assertThat(exception.getMessage()).doesNotContain(ACCESS_KEY_SECRET);
            assertThat(logs).contains("endpoint: oss-cn-hangzhou.aliyuncs.com");
            assertThat(logs).contains("bucket: community-bucket");
            assertThat(logs).contains("accessKeyId: LTAI****7890");
            assertThat(logs).contains("errorType: IllegalStateException");
            assertThat(logs).doesNotContain(ACCESS_KEY_ID);
            assertThat(logs).doesNotContain(ACCESS_KEY_SECRET);
            assertThat(logs).doesNotContain("InvalidAccessKeyId");
        } finally {
            logger.detachAppender(appender);
        }
    }

    private OssConfig validConfig() {
        OssConfig config = new OssConfig();
        config.setEnabled(true);
        config.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        config.setBucketName("community-bucket");
        config.setAccessKeyId(ACCESS_KEY_ID);
        config.setAccessKeySecret(ACCESS_KEY_SECRET);
        return config;
    }

    private static class FailingOssServiceImpl extends OssServiceImpl {

        FailingOssServiceImpl(OssConfig ossConfig) {
            super(ossConfig);
        }

        @Override
        protected OSS buildClient() {
            throw new IllegalStateException("InvalidAccessKeyId: " + ACCESS_KEY_ID + " / " + ACCESS_KEY_SECRET);
        }
    }
}
