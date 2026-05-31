package com.airesume.server.common.util;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicHttpsUrlValidatorTest {

    @Test
    void shouldAllowPublicHttpsModelProviderUrl() {
        String result = PublicHttpsUrlValidator.validate(
                "  https://8.8.8.8/compatible-mode/v1  ",
                "基础地址不能为空");

        assertEquals("https://8.8.8.8/compatible-mode/v1", result);
    }

    @Test
    void shouldRejectHttpUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> PublicHttpsUrlValidator.validate("http://api.mimo.example.com/v1", "基础地址不能为空"));
    }

    @Test
    void shouldRejectLocalhostUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> PublicHttpsUrlValidator.validate("https://localhost:8080/v1", "基础地址不能为空"));
    }

    @Test
    void shouldRejectPrivateIpv4Url() {
        assertThrows(IllegalArgumentException.class,
                () -> PublicHttpsUrlValidator.validate("https://192.168.1.10/v1", "基础地址不能为空"));
        assertThrows(IllegalArgumentException.class,
                () -> PublicHttpsUrlValidator.validate("https://10.0.0.5/v1", "基础地址不能为空"));
        assertThrows(IllegalArgumentException.class,
                () -> PublicHttpsUrlValidator.validate("https://172.20.0.5/v1", "基础地址不能为空"));
    }

    @Test
    void shouldRejectCloudMetadataUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> PublicHttpsUrlValidator.validate("https://169.254.169.254/latest/meta-data", "基础地址不能为空"));
        assertThrows(IllegalArgumentException.class,
                () -> PublicHttpsUrlValidator.validate("https://100.100.100.200/latest/meta-data", "基础地址不能为空"));
        assertThrows(IllegalArgumentException.class,
                () -> PublicHttpsUrlValidator.validate("https://metadata.google.internal/computeMetadata/v1", "基础地址不能为空"));
    }

    @Test
    void shouldRejectMappedPrivateIpv4Url() {
        assertThrows(IllegalArgumentException.class,
                () -> PublicHttpsUrlValidator.validate("https://[::ffff:192.168.1.10]/v1", "基础地址不能为空"));
    }

    @Test
    void shouldRejectDomainResolvedToPrivateAddress() {
        assertThrows(IllegalArgumentException.class,
                () -> PublicHttpsUrlValidator.validate(
                        "https://api.example.com/v1",
                        "基础地址不能为空",
                        host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")}));
    }

    @Test
    void shouldAllowUnresolvedPublicDomainWhenDnsCheckIsSkipped() {
        String result = PublicHttpsUrlValidator.validateWithoutDnsResolution(
                "  https://startup-only.invalid/v1  ",
                "基础地址不能为空");

        assertEquals("https://startup-only.invalid/v1", result);
    }
}
