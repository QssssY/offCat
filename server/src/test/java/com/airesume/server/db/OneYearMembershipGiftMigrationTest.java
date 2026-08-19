package com.airesume.server.db;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 一年会员存量赠送迁移的一致性与幂等性测试。
 */
class OneYearMembershipGiftMigrationTest {

    private static final String MIGRATION_PATH = "db/migrations/TASK_91_ONE_YEAR_MEMBERSHIP_GIFT.sql";

    @Test
    void shouldKeepOneYearMembershipGiftMigrationInSyncAndIdempotent() throws Exception {
        String rootMigration = readSql("../" + MIGRATION_PATH);
        String serverMigration = readSql(MIGRATION_PATH);

        assertEquals(rootMigration, serverMigration, "一年会员赠送迁移脚本必须在两个 SQL 目录保持一致");
        assertAll(
                () -> assertTrue(serverMigration.contains("START TRANSACTION")),
                () -> assertTrue(serverMigration.contains("COMMIT")),
                () -> assertTrue(serverMigration.contains("u.`role` <> 9")),
                () -> assertTrue(serverMigration.contains("FROM `sys_user` u\nINNER JOIN `tmp_one_year_membership_gift_users` gift_user ON gift_user.`id` = u.`id`\nWHERE u.`is_deleted` = 0\n  AND u.`role` <> 9")),
                () -> assertTrue(serverMigration.contains("'vip_year'")),
                () -> assertTrue(serverMigration.contains("DATE_ADD(NOW(), INTERVAL 1 YEAR)")),
                () -> assertTrue(serverMigration.contains("GREATEST")),
                () -> assertTrue(serverMigration.contains("tmp_one_year_membership_gift_users")),
                () -> assertTrue(serverMigration.contains("'membership_gift'")),
                () -> assertTrue(serverMigration.contains("'existing_user_one_year_20260819'")),
                () -> assertTrue(serverMigration.contains("NOT EXISTS"))
        );
    }

    private String readSql(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
