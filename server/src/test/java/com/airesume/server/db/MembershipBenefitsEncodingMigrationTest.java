package com.airesume.server.db;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembershipBenefitsEncodingMigrationTest {

    private static final String EXPECTED_BENEFITS_JSON = "[\"AI 简历润色（每份简历 1 次）\","
            + "\"JD 岗位匹配分析（每日 3 次）\","
            + "\"简历模板库（每日 5 次使用）\","
            + "\"Offer 薪资谈判辅助（每日 3 次）\","
            + "\"模拟面试（每日 10 次）\","
            + "\"简历诊断（每日 5 次）\"]";

    private static final Pattern BENEFITS_HEX_PATTERN =
            Pattern.compile("benefits\\s*=\\s*CONVERT\\(0x([0-9A-F]+)\\s+USING\\s+utf8mb4\\)",
                    Pattern.CASE_INSENSITIVE);

    @Test
    void shouldUseUtf8HexLiteralForMembershipBenefitsRepairScript() throws Exception {
        String sql = readSql("../db/migrations/fix_benefits_encoding_hex.sql");
        String serverSql = readSql("db/migrations/fix_benefits_encoding_hex.sql");

        assertBenefitsRepairScript(sql);
        assertBenefitsRepairScript(serverSql);
    }

    @Test
    void shouldUseUtf8HexLiteralForMembershipQuotaMigrationInBothSqlDirectories() throws Exception {
        String rootMigration = readSql("../db/migrations/TASK_57_MEMBERSHIP_QUOTA_ENHANCEMENT.sql");
        String serverMigration = readSql("db/migrations/TASK_57_MEMBERSHIP_QUOTA_ENHANCEMENT.sql");

        assertHexBenefitsMigration(rootMigration);
        assertHexBenefitsMigration(serverMigration);
    }

    private void assertHexBenefitsMigration(String sql) {
        assertFalse(sql.contains("benefits = JSON_ARRAY("), "迁移脚本不能依赖客户端正确读取中文 SQL 字面量");
        assertEquals(EXPECTED_BENEFITS_JSON, extractBenefitsJson(sql));
    }

    private void assertBenefitsRepairScript(String sql) {
        assertFalse(sql.contains("\\x"), "MySQL 不会把普通字符串里的 \\x 当作可靠 UTF-8 字节写入");
        assertTrue(sql.contains("CONVERT(0x"), "修复脚本必须用十六进制字节绕过客户端文件编码");
        assertEquals(EXPECTED_BENEFITS_JSON, extractBenefitsJson(sql));
    }

    private String extractBenefitsJson(String sql) {
        Matcher matcher = BENEFITS_HEX_PATTERN.matcher(sql);
        assertTrue(matcher.find(), "未找到 benefits = CONVERT(0x... USING utf8mb4) 写入语句");
        byte[] bytes = HexFormat.of().parseHex(matcher.group(1));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String readSql(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
