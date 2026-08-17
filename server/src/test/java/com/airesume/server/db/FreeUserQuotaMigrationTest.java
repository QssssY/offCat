package com.airesume.server.db;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 免费用户六项额度迁移与初始化 schema 一致性测试。
 */
class FreeUserQuotaMigrationTest {

    private static final String MIGRATION_PATH = "db/migrations/TASK_90_FREE_USER_QUOTA_100.sql";

    @Test
    void shouldKeepFreeUserQuotaMigrationInSyncAndScopedToNonAdminUsers() throws Exception {
        String rootMigration = readSql("../" + MIGRATION_PATH);
        String serverMigration = readSql(MIGRATION_PATH);

        assertEquals(rootMigration, serverMigration, "免费用户额度迁移脚本必须在两个 SQL 目录保持一致");
        assertAll(
                () -> assertTrue(serverMigration.contains("INNER JOIN `sys_user` u")),
                () -> assertTrue(serverMigration.contains("u.`role` <> 9")),
                () -> assertTrue(serverMigration.contains("GREATEST(q.`interview_quota`, 100)")),
                () -> assertTrue(serverMigration.contains("GREATEST(q.`resume_quota`, 100)")),
                () -> assertTrue(serverMigration.contains("GREATEST(q.`free_polish_left`, 100)")),
                () -> assertTrue(serverMigration.contains("GREATEST(q.`free_jd_match_left`, 100)")),
                () -> assertTrue(serverMigration.contains("GREATEST(q.`free_template_left`, 100)")),
                () -> assertTrue(serverMigration.contains("GREATEST(q.`free_offer_left`, 100)"))
        );
    }

    @Test
    void shouldUseOneHundredAsEveryFreeQuotaSchemaDefault() throws Exception {
        String rootSchema = readSql("../db/schema.sql");
        String serverSchema = readSql("db/schema.sql");

        assertEquals(rootSchema, serverSchema, "db/schema.sql 与 server/db/schema.sql 必须保持完全一致");
        assertAll(
                () -> assertTrue(serverSchema.contains("`interview_quota` INT NOT NULL DEFAULT 100")),
                () -> assertTrue(serverSchema.contains("`resume_quota` INT NOT NULL DEFAULT 100")),
                () -> assertTrue(serverSchema.contains("`free_polish_left` INT NOT NULL DEFAULT 100")),
                () -> assertTrue(serverSchema.contains("`free_jd_match_left` INT NOT NULL DEFAULT 100")),
                () -> assertTrue(serverSchema.contains("`free_template_left` INT NOT NULL DEFAULT 100")),
                () -> assertTrue(serverSchema.contains("`free_offer_left` INT NOT NULL DEFAULT 100"))
        );
    }

    private String readSql(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
