SET NAMES utf8mb4;

-- 同一次执行固定赠送到期时间，确保所有存量用户口径一致。
SET @gift_expire_time := DATE_ADD(NOW(), INTERVAL 1 YEAR);

DROP TEMPORARY TABLE IF EXISTS `tmp_one_year_membership_gift_users`;
CREATE TEMPORARY TABLE `tmp_one_year_membership_gift_users` AS
SELECT u.`id`
FROM `sys_user` u
WHERE u.`is_deleted` = 0
  AND u.`role` <> 9
  AND NOT EXISTS (
      SELECT 1
      FROM `user_notification` n
      WHERE n.`user_id` = u.`id`
        AND n.`biz_type` = 'membership_gift'
        AND n.`biz_id` = 'existing_user_one_year_20260819'
  );

START TRANSACTION;

-- 管理员角色同时承担鉴权语义，不能覆盖为 VIP；已拥有更长期限的会员不会被缩短。
UPDATE `sys_user` u
INNER JOIN `tmp_one_year_membership_gift_users` gift_user ON gift_user.`id` = u.`id`
SET u.`role` = 1,
    u.`membership_plan_code` = 'vip_year',
    u.`vip_expire_time` = GREATEST(COALESCE(u.`vip_expire_time`, @gift_expire_time), @gift_expire_time),
    u.`update_time` = NOW()
WHERE u.`is_deleted` = 0;

-- 通知记录同时作为幂等标记；即使用户删除通知，逻辑删除记录仍可阻止重复赠送。
INSERT INTO `user_notification` (
    `id`, `user_id`, `type`, `title`, `content`, `biz_type`, `biz_id`, `read_status`
)
SELECT UUID_SHORT(),
       u.`id`,
       'system',
       '一年会员已到账',
       CONCAT('感谢你一直以来的支持，已补发一年会员权益，有效期至 ',
              DATE_FORMAT(u.`vip_expire_time`, '%Y-%m-%d %H:%i'), '。'),
       'membership_gift',
       'existing_user_one_year_20260819',
       0
FROM `sys_user` u
INNER JOIN `tmp_one_year_membership_gift_users` gift_user ON gift_user.`id` = u.`id`;

COMMIT;

DROP TEMPORARY TABLE `tmp_one_year_membership_gift_users`;
