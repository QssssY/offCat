SET NAMES utf8mb4;

-- 新建额度记录即采用 100 次免费基线，保持数据库默认值与后端初始化常量一致。
ALTER TABLE `user_quota`
  MODIFY COLUMN `interview_quota` INT NOT NULL DEFAULT 100 COMMENT 'Remaining interview quota',
  MODIFY COLUMN `resume_quota` INT NOT NULL DEFAULT 100 COMMENT 'Remaining resume quota',
  MODIFY COLUMN `free_polish_left` INT NOT NULL DEFAULT 100 COMMENT '非会员免费润色剩余',
  MODIFY COLUMN `free_jd_match_left` INT NOT NULL DEFAULT 100 COMMENT '非会员免费JD匹配剩余',
  MODIFY COLUMN `free_template_left` INT NOT NULL DEFAULT 100 COMMENT '非会员免费模板剩余',
  MODIFY COLUMN `free_offer_left` INT NOT NULL DEFAULT 100 COMMENT '非会员免费Offer剩余';

-- 为所有非管理员补足免费基础储备，保证现有会员到期后也能获得新基线；套餐日额度本身不变。
UPDATE `user_quota` q
INNER JOIN `sys_user` u
  ON u.`id` = q.`user_id`
  AND u.`is_deleted` = 0
SET q.`interview_quota` = GREATEST(q.`interview_quota`, 100),
    q.`resume_quota` = GREATEST(q.`resume_quota`, 100),
    q.`free_polish_left` = GREATEST(q.`free_polish_left`, 100),
    q.`free_jd_match_left` = GREATEST(q.`free_jd_match_left`, 100),
    q.`free_template_left` = GREATEST(q.`free_template_left`, 100),
    q.`free_offer_left` = GREATEST(q.`free_offer_left`, 100)
WHERE q.`is_deleted` = 0
  AND u.`role` <> 9;
