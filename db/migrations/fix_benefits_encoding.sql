SET NAMES utf8mb4;
UPDATE membership_plan SET benefits = JSON_ARRAY(
    'AI 简历润色（每份简历 1 次）',
    'JD 岗位匹配分析（每日 3 次）',
    '简历模板库（每日 5 次使用）',
    'Offer 薪资谈判辅助（每日 3 次）',
    '模拟面试（每日 10 次）',
    '简历诊断（每日 5 次）'
);
