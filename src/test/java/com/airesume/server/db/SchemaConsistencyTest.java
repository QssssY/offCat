package com.airesume.server.db;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定根目录与 server 目录的 schema 双副本一致，避免后续只改一份导致部署漂移。
 */
class SchemaConsistencyTest {

    @Test
    void shouldKeepRootAndServerSchemaInSyncForCriticalTablesAndIndexes() throws Exception {
        String rootSchema = readSql("../db/schema.sql");
        String serverSchema = readSql("db/schema.sql");

        assertEquals(rootSchema, serverSchema, "db/schema.sql 与 server/db/schema.sql 必须保持完全一致");
        assertContainsCriticalSchema(serverSchema);
    }

    @Test
    void shouldKeepPerformanceIndexMigrationInSyncAndRepeatable() throws Exception {
        String rootMigration = readSql("../db/migrations/TASK_58_PERFORMANCE_INDEXES.sql");
        String serverMigration = readSql("db/migrations/TASK_58_PERFORMANCE_INDEXES.sql");

        assertEquals(rootMigration, serverMigration, "性能索引迁移脚本必须在两个 SQL 目录保持一致");
        assertTrue(serverMigration.contains("information_schema.STATISTICS"));
        assertTrue(serverMigration.contains("idx_community_comment_parent_time"));
        assertTrue(serverMigration.contains("`parent_comment_id`, `create_time`"));
        assertTrue(serverMigration.contains("idx_resume_task_status_failed_at"));
        assertTrue(serverMigration.contains("`status`, `failed_at`"));
    }

    @Test
    void shouldKeepCompositePerformanceIndexMigrationInSyncAndRepeatable() throws Exception {
        String rootMigration = readSql("../db/migrations/TASK_60_PERFORMANCE_COMPOSITE_INDEXES.sql");
        String serverMigration = readSql("db/migrations/TASK_60_PERFORMANCE_COMPOSITE_INDEXES.sql");

        assertEquals(rootMigration, serverMigration, "TASK_60 复合索引迁移脚本必须在两个 SQL 目录保持一致");
        assertTrue(serverMigration.contains("information_schema.STATISTICS"));
        assertTrue(serverMigration.contains("idx_resume_task_user_status_time"));
        assertTrue(serverMigration.contains("idx_interview_session_user_status_time"));
        assertTrue(serverMigration.contains("idx_notification_user_read_time"));
        assertTrue(serverMigration.contains("idx_community_post_deleted_category_time"));
        assertTrue(serverMigration.contains("idx_community_comment_reply_user_actor_time"));
    }

    @Test
    void shouldKeepAdminNotificationFilterIndexMigrationInSyncAndRepeatable() throws Exception {
        String rootMigration = readSql("../db/migrations/TASK_ADMIN_NOTIFICATION_FILTER_INDEXES.sql");
        String serverMigration = readSql("db/migrations/TASK_ADMIN_NOTIFICATION_FILTER_INDEXES.sql");

        assertEquals(rootMigration, serverMigration, "admin notification filter index migration must stay in sync");
        assertTrue(serverMigration.contains("information_schema.STATISTICS"));
        assertTrue(serverMigration.contains("idx_admin_notification_filter_time"));
        assertTrue(serverMigration.contains("`target_type`, `status`, `type`, `create_time`"));
    }

    @Test
    void shouldKeepAdminVersionLogFilterIndexMigrationInSyncAndRepeatable() throws Exception {
        String rootMigration = readSql("../db/migrations/TASK_ADMIN_VERSION_LOG_FILTER_INDEXES.sql");
        String serverMigration = readSql("db/migrations/TASK_ADMIN_VERSION_LOG_FILTER_INDEXES.sql");

        assertEquals(rootMigration, serverMigration, "admin version log filter index migration must stay in sync");
        assertTrue(serverMigration.contains("information_schema.STATISTICS"));
        assertTrue(serverMigration.contains("idx_version_log_filter_time"));
        assertTrue(serverMigration.contains("`status`, `type`, `create_time`"));
    }

    @Test
    void shouldKeepCommunityModerationMigrationUtf8SafeAndInSync() throws Exception {
        String rootMigration = readSql("../db/migrations/TASK_61_COMMUNITY_CONTENT_MODERATION.sql");
        String serverMigration = readSql("db/migrations/TASK_61_COMMUNITY_CONTENT_MODERATION.sql");

        assertEquals(rootMigration, serverMigration, "community moderation migration must stay in sync");
        assertTrue(serverMigration.contains("SET NAMES utf8mb4;"));
        assertTrue(serverMigration.contains("IN p_column_definition TEXT CHARACTER SET utf8mb4"));
        assertTrue(serverMigration.contains("IN p_index_definition TEXT CHARACTER SET utf8mb4"));
        assertTrue(serverMigration.contains("COMMENT ''审核状态：pending-待审，approved-通过，rejected-拒绝，hidden-隐藏''"));
        assertTrue(serverMigration.contains("idx_community_post_review_time"));
        assertTrue(serverMigration.contains("idx_community_comment_post_review_time"));
    }

    @Test
    void shouldKeepUserBanFieldsMigrationInSyncAndRepeatable() throws Exception {
        String rootMigration = readSql("../db/migrations/TASK_66_USER_BAN_FIELDS.sql");
        String serverMigration = readSql("db/migrations/TASK_66_USER_BAN_FIELDS.sql");

        assertEquals(rootMigration, serverMigration, "user ban fields migration must stay in sync");
        assertTrue(serverMigration.contains("SET NAMES utf8mb4;"));
        assertTrue(serverMigration.contains("IN p_column_definition TEXT CHARACTER SET utf8mb4"));
        assertTrue(serverMigration.contains("ban_reason"));
        assertTrue(serverMigration.contains("banned_until"));
        assertTrue(serverMigration.contains("idx_sys_user_banned_until"));
    }

    private void assertContainsCriticalSchema(String schema) {
        assertTrue(schema.contains("CREATE TABLE `user_settings`"));
        assertTrue(schema.contains("idx_user_settings_resume_retention"));
        assertTrue(schema.contains("CREATE TABLE `community_comment`"));
        assertTrue(schema.contains("idx_community_comment_parent_time"));
        assertTrue(schema.contains("idx_resume_task_status_failed_at"));
        assertTrue(schema.contains("idx_resume_task_user_status_time"));
        assertTrue(schema.contains("idx_interview_session_user_status_time"));
        assertTrue(schema.contains("idx_notification_user_read_time"));
        assertTrue(schema.contains("idx_community_post_deleted_category_time"));
        assertTrue(schema.contains("idx_community_comment_reply_user_actor_time"));
        assertTrue(schema.contains("idx_admin_notification_filter_time"));
        assertTrue(schema.contains("idx_version_log_filter_time"));
        assertTrue(schema.contains("ban_reason"));
        assertTrue(schema.contains("banned_until"));
        assertTrue(schema.contains("idx_sys_user_banned_until"));
        assertTrue(schema.contains("idx_chat_log_message_role"));
        assertTrue(schema.contains("CONVERT(0x"));
    }

    private String readSql(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
