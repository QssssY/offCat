-- ============================================================
-- TASK-57: 会员系统增强 — 总额度 + 功能权益 + 非会员免费体验
-- ============================================================

-- 强制 UTF-8 编码，防止中文乱码
SET NAMES utf8mb4;

-- 1. membership_plan 表：增加每日功能限制、周期总上限、赠送额度、权益描述
ALTER TABLE membership_plan
  ADD COLUMN daily_polish_limit INT NOT NULL DEFAULT 1 COMMENT '每日AI润色次数',
  ADD COLUMN daily_jd_match_limit INT NOT NULL DEFAULT 3 COMMENT '每日JD匹配次数',
  ADD COLUMN daily_template_limit INT NOT NULL DEFAULT 5 COMMENT '每日模板使用次数',
  ADD COLUMN daily_offer_limit INT NOT NULL DEFAULT 3 COMMENT '每日Offer辅助次数',
  ADD COLUMN total_resume_quota INT NOT NULL DEFAULT 0 COMMENT '套餐周期内简历诊断总额度（0=不限）',
  ADD COLUMN total_interview_quota INT NOT NULL DEFAULT 0 COMMENT '套餐周期内面试总额度（0=不限）',
  ADD COLUMN total_polish_quota INT NOT NULL DEFAULT 0 COMMENT '套餐周期内AI润色总额度（0=不限）',
  ADD COLUMN total_jd_match_quota INT NOT NULL DEFAULT 0 COMMENT '套餐周期内JD匹配总额度（0=不限）',
  ADD COLUMN total_template_quota INT NOT NULL DEFAULT 0 COMMENT '套餐周期内模板总额度（0=不限）',
  ADD COLUMN total_offer_quota INT NOT NULL DEFAULT 0 COMMENT '套餐周期内Offer总额度（0=不限）',
  ADD COLUMN bonus_resume_quota INT NOT NULL DEFAULT 0 COMMENT '购买赠送简历诊断额度',
  ADD COLUMN bonus_interview_quota INT NOT NULL DEFAULT 0 COMMENT '购买赠送面试额度',
  ADD COLUMN benefits JSON NULL COMMENT '套餐权益描述列表（前端展示）';

-- 2. user_quota 表：增加功能日用量、周期用量、非会员免费体验剩余
ALTER TABLE user_quota
  ADD COLUMN daily_polish_used INT NOT NULL DEFAULT 0 COMMENT '今日AI润色使用次数',
  ADD COLUMN daily_jd_match_used INT NOT NULL DEFAULT 0 COMMENT '今日JD匹配使用次数',
  ADD COLUMN daily_template_used INT NOT NULL DEFAULT 0 COMMENT '今日模板使用次数',
  ADD COLUMN daily_offer_used INT NOT NULL DEFAULT 0 COMMENT '今日Offer辅助使用次数',
  ADD COLUMN cycle_resume_used INT NOT NULL DEFAULT 0 COMMENT '周期内简历诊断已用',
  ADD COLUMN cycle_interview_used INT NOT NULL DEFAULT 0 COMMENT '周期内面试已用',
  ADD COLUMN cycle_polish_used INT NOT NULL DEFAULT 0 COMMENT '周期内润色已用',
  ADD COLUMN cycle_jd_match_used INT NOT NULL DEFAULT 0 COMMENT '周期内JD匹配已用',
  ADD COLUMN cycle_template_used INT NOT NULL DEFAULT 0 COMMENT '周期内模板已用',
  ADD COLUMN cycle_offer_used INT NOT NULL DEFAULT 0 COMMENT '周期内Offer已用',
  ADD COLUMN cycle_start_time DATETIME NULL COMMENT '当前周期开始时间',
  ADD COLUMN free_polish_left INT NOT NULL DEFAULT 1 COMMENT '非会员免费润色剩余',
  ADD COLUMN free_jd_match_left INT NOT NULL DEFAULT 1 COMMENT '非会员免费JD匹配剩余',
  ADD COLUMN free_template_left INT NOT NULL DEFAULT 2 COMMENT '非会员免费模板剩余',
  ADD COLUMN free_offer_left INT NOT NULL DEFAULT 1 COMMENT '非会员免费Offer剩余';

-- 3. 为已有用户初始化免费体验次数
UPDATE user_quota SET
  free_polish_left = 1,
  free_jd_match_left = 1,
  free_template_left = 2,
  free_offer_left = 1
WHERE free_polish_left = 0
   OR free_jd_match_left = 0
   OR free_template_left = 0
   OR free_offer_left = 0;

-- 4. 更新会员套餐种子数据（添加权益描述）
UPDATE membership_plan SET
  daily_polish_limit = 1,
  daily_jd_match_limit = 3,
  daily_template_limit = 5,
  daily_offer_limit = 3,
  total_resume_quota = 0,
  total_interview_quota = 0,
  total_polish_quota = 0,
  total_jd_match_quota = 0,
  total_template_quota = 0,
  total_offer_quota = 0,
  bonus_resume_quota = 0,
  bonus_interview_quota = 0,
  benefits = CONVERT(0x5B22414920E7AE80E58E86E6B6A6E889B2EFBC88E6AF8FE4BBBDE7AE80E58E86203120E6ACA1EFBC89222C224A4420E5B297E4BD8DE58CB9E9858DE58886E69E90EFBC88E6AF8FE697A5203320E6ACA1EFBC89222C22E7AE80E58E86E6A8A1E69DBFE5BA93EFBC88E6AF8FE697A5203520E6ACA1E4BDBFE794A8EFBC89222C224F6666657220E896AAE8B584E8B088E588A4E8BE85E58AA9EFBC88E6AF8FE697A5203320E6ACA1EFBC89222C22E6A8A1E68B9FE99DA2E8AF95EFBC88E6AF8FE697A520313020E6ACA1EFBC89222C22E7AE80E58E86E8AF8AE696ADEFBC88E6AF8FE697A5203520E6ACA1EFBC89225D USING utf8mb4);
