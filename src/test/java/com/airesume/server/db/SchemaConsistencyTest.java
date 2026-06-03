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

    @Test
    void shouldKeepUserCustomAiMigrationInSyncAndRepeatable() throws Exception {
        String rootMigration = readSql("../db/migrations/TASK_68_USER_CUSTOM_AI_PROVIDER.sql");
        String serverMigration = readSql("db/migrations/TASK_68_USER_CUSTOM_AI_PROVIDER.sql");

        assertEquals(rootMigration, serverMigration, "TASK_68 用户自定义 AI 迁移脚本必须在两个 SQL 目录保持一致");
        assertTrue(serverMigration.contains("SET NAMES utf8mb4;"));
        assertTrue(serverMigration.contains("CREATE TABLE IF NOT EXISTS `user_ai_config`"));
        assertTrue(serverMigration.contains("CREATE TABLE IF NOT EXISTS `user_ai_daily_usage`"));
        assertTrue(serverMigration.contains("CREATE TABLE IF NOT EXISTS `sys_config`"));
        assertTrue(serverMigration.contains("custom_ai_daily_limit"));
        assertTrue(serverMigration.contains("ai_billing_source"));
        assertTrue(serverMigration.contains("fallback_to_platform"));
    }

    @Test
    void shouldKeepUserCustomAiUsageStatsMigrationInSyncAndRepeatable() throws Exception {
        String rootMigration = readSql("../db/migrations/TASK_68_CUSTOM_AI_USAGE_STATS.sql");
        String serverMigration = readSql("db/migrations/TASK_68_CUSTOM_AI_USAGE_STATS.sql");

        assertEquals(rootMigration, serverMigration, "TASK_68 自定义 AI 统计迁移脚本必须在两个 SQL 目录保持一致");
        assertTrue(serverMigration.contains("SET NAMES utf8mb4;"));
        assertTrue(serverMigration.contains("CREATE TABLE IF NOT EXISTS `user_ai_usage_detail`"));
        assertTrue(serverMigration.contains("usage_type"));
        assertTrue(serverMigration.contains("uk_user_ai_usage_detail_user_date_type"));
        assertTrue(serverMigration.contains("idx_user_ai_usage_detail_date_type"));
    }

    @Test
    void shouldKeepInterviewPlatformFallbackBillingMigrationInSyncAndRepeatable() throws Exception {
        String rootMigration = readSql("../db/migrations/TASK_74_INTERVIEW_PLATFORM_FALLBACK_BILLING.sql");
        String serverMigration = readSql("db/migrations/TASK_74_INTERVIEW_PLATFORM_FALLBACK_BILLING.sql");

        assertEquals(rootMigration, serverMigration, "TASK_74 interview fallback billing migration must stay in sync");
        assertTrue(serverMigration.contains("SET NAMES utf8mb4;"));
        assertTrue(serverMigration.contains("interview_session"));
        assertTrue(serverMigration.contains("ai_billing_source"));
        assertTrue(serverMigration.contains("platform_fallback"));
    }

    @Test
    void shouldKeepTtsProviderMigrationsInSync() throws Exception {
        String rootEndpointMigration = readSql("../db/migrations/alter_tts_endpoint_path.sql");
        String serverEndpointMigration = readSql("db/migrations/alter_tts_endpoint_path.sql");
        String rootProviderMigration = readSql("../db/migrations/alter_tts_provider.sql");
        String serverProviderMigration = readSql("db/migrations/alter_tts_provider.sql");

        assertEquals(rootEndpointMigration, serverEndpointMigration, "tts_endpoint_path migration must stay in sync");
        assertEquals(rootProviderMigration, serverProviderMigration, "tts_provider migration must stay in sync");
        assertTrue(serverEndpointMigration.contains("tts_endpoint_path"));
        assertTrue(serverProviderMigration.contains("tts_provider"));
        assertTrue(serverProviderMigration.contains("AFTER tts_endpoint_path"));
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
        assertTrue(schema.contains("CREATE TABLE `user_ai_config`"));
        assertTrue(schema.contains("`tts_endpoint_path`"));
        assertTrue(schema.contains("`tts_provider`"));
        assertTrue(schema.contains("CREATE TABLE `user_ai_daily_usage`"));
        assertTrue(schema.contains("CREATE TABLE `user_ai_usage_detail`"));
        assertTrue(schema.contains("idx_user_ai_usage_detail_date_type"));
        assertTrue(schema.contains("CREATE TABLE `sys_config`"));
        assertTrue(schema.contains("custom_ai_daily_limit"));
        assertTrue(schema.contains("ai_billing_source"));
        assertTrue(schema.contains("fallback_to_platform"));
        assertTrue(schema.contains("platform_fallback"));
    }

    private String readSql(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
