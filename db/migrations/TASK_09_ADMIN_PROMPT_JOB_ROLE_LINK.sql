SET NAMES utf8mb4;

/*
  作用：
  为 Prompt 表补充 job_role_code 字段，让 Prompt 和岗位配置表形成稳定关联。

  为什么要这样做：
  之前 Prompt 只有 job_role 文本字段，属于自由输入字符串。
  这会导致 Prompt 和岗位配置脱节，无法保证管理员选择的是合法岗位。
  本次升级采用兼容方案：
  - 保留原有 job_role 作为岗位名称快照
  - 新增 job_role_code 作为稳定关联字段
  - 尝试根据已有 job_role 名称回填 job_role_code
*/

ALTER TABLE `sys_prompt`
  ADD `job_role_code` VARCHAR(64) NULL DEFAULT NULL COMMENT 'Configured job role code' AFTER `scenario_type`;

ALTER TABLE `sys_prompt`
  ADD INDEX `idx_sys_prompt_job_role_code` (`job_role_code`);

/*
  作用：
  根据已有岗位名称回填岗位编码。
  这样旧数据在名称能匹配到 sys_job_role 时，会自动升级成“编码 + 名称”的双字段模式。
*/
UPDATE `sys_prompt` p
INNER JOIN `sys_job_role` r ON p.`job_role` = r.`role_name`
SET p.`job_role_code` = r.`role_code`
WHERE p.`job_role_code` IS NULL;
