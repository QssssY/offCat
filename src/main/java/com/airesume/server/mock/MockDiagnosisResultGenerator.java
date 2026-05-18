package com.airesume.server.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 模拟诊断结果生成器
 * 用于在未接入真实大模型时生成模拟的结构化诊断结果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockDiagnosisResultGenerator {

    private final ObjectMapper objectMapper;
    // 使用 ThreadLocalRandom.current() 替代共享 Random 实例，避免多线程竞争

    /**
     * 生成模拟的简历诊断结果
     *
     * @param fileUrl 简历文件地址（用于记录日志）
     * @return JSON格式的诊断结果字符串
     */
    public String generateMockDiagnosisResult(String fileUrl) {
        log.info("Generating mock diagnosis result for file: {}", fileUrl);

        try {
            ObjectNode result = objectMapper.createObjectNode();

            // 1. 基本信息评估
            ObjectNode basicInfo = objectMapper.createObjectNode();
            basicInfo.put("score", java.util.concurrent.ThreadLocalRandom.current().nextInt(20) + 70);
            basicInfo.put("hasName", true);
            basicInfo.put("hasPhone", true);
            basicInfo.put("hasEmail", true);
            basicInfo.put("hasGithub", java.util.concurrent.ThreadLocalRandom.current().nextBoolean());
            basicInfo.put("hasBlog", java.util.concurrent.ThreadLocalRandom.current().nextBoolean());
            result.set("basicInfo", basicInfo);

            // 2. 工作经历评估
            ObjectNode workExperience = objectMapper.createObjectNode();
            workExperience.put("score", java.util.concurrent.ThreadLocalRandom.current().nextInt(30) + 60);
            workExperience.put("totalYears", java.util.concurrent.ThreadLocalRandom.current().nextInt(10) + 1);
            workExperience.put("companyCount", java.util.concurrent.ThreadLocalRandom.current().nextInt(4) + 1);
            workExperience.put("hasQuantifiableResults", java.util.concurrent.ThreadLocalRandom.current().nextBoolean());
            ArrayNode suggestions1 = objectMapper.createArrayNode();
            suggestions1.add("建议增加更多量化成果描述");
            suggestions1.add("工作经历时间线清晰，继续保持");
            workExperience.set("suggestions", suggestions1);
            result.set("workExperience", workExperience);

            // 3. 项目经验评估
            ObjectNode projectExperience = objectMapper.createObjectNode();
            projectExperience.put("score", java.util.concurrent.ThreadLocalRandom.current().nextInt(25) + 65);
            projectExperience.put("projectCount", java.util.concurrent.ThreadLocalRandom.current().nextInt(5) + 1);
            projectExperience.put("hasTechStack", true);
            projectExperience.put("hasResponsibilities", true);
            ArrayNode suggestions2 = objectMapper.createArrayNode();
            suggestions2.add("建议补充项目中使用的具体技术栈版本");
            projectExperience.set("suggestions", suggestions2);
            result.set("projectExperience", projectExperience);

            // 4. 技能清单评估
            ObjectNode skills = objectMapper.createObjectNode();
            skills.put("score", java.util.concurrent.ThreadLocalRandom.current().nextInt(15) + 75);
            List<String> mockSkills = Arrays.asList("Java", "Spring Boot", "MySQL", "Redis", "Git", "Docker");
            ArrayNode skillList = objectMapper.createArrayNode();
            int skillCount = java.util.concurrent.ThreadLocalRandom.current().nextInt(4) + 3;
            for (int i = 0; i < skillCount; i++) {
                skillList.add(mockSkills.get(i % mockSkills.size()));
            }
            skills.set("skillList", skillList);
            result.set("skills", skills);

            // 5. 个人定位评估
            ObjectNode positioning = objectMapper.createObjectNode();
            positioning.put("score", java.util.concurrent.ThreadLocalRandom.current().nextInt(25) + 65);
            positioning.put("hasSummary", java.util.concurrent.ThreadLocalRandom.current().nextBoolean());
            positioning.put("hasClearPositioning", java.util.concurrent.ThreadLocalRandom.current().nextBoolean());
            ArrayNode positioningSuggestions = objectMapper.createArrayNode();
            positioningSuggestions.add("建议增加个人总结/职业目标描述");
            positioningSuggestions.add("核心竞争力描述需更突出");
            positioning.set("suggestions", positioningSuggestions);
            result.set("positioningEvaluation", positioning);

            // 6. 总体评价
            ObjectNode overall = objectMapper.createObjectNode();
            int totalScore = (basicInfo.get("score").asInt() + workExperience.get("score").asInt()
                    + projectExperience.get("score").asInt() + skills.get("score").asInt()
                    + positioning.get("score").asInt()) / 5;
            overall.put("totalScore", totalScore);
            overall.put("level", getLevel(totalScore));
            ArrayNode highlights = objectMapper.createArrayNode();
            highlights.add("简历结构完整");
            highlights.add("工作经历描述清晰");
            overall.set("highlights", highlights);
            ArrayNode improvements = objectMapper.createArrayNode();
            improvements.add("建议增加项目成果的数据支撑");
            improvements.add("技能描述可以更具体");
            overall.set("improvements", improvements);
            result.set("overall", overall);

            // 6. 模拟处理信息
            result.put("isMock", true);
            result.put("mockGeneratedAt", System.currentTimeMillis());

            String jsonResult = objectMapper.writeValueAsString(result);
            log.info("Mock diagnosis result generated successfully, totalScore: {}", totalScore);
            return jsonResult;

        } catch (Exception e) {
            log.error("Failed to generate mock diagnosis result", e);
            throw new RuntimeException("生成模拟诊断结果失败", e);
        }
    }

    /**
     * 根据分数获取等级
     *
     * @param score 分数
     * @return 等级描述
     */
    private String getLevel(int score) {
        if (score >= 90) {
            return "S";
        } else if (score >= 75) {
            return "A";
        } else if (score >= 60) {
            return "B";
        } else if (score >= 40) {
            return "C";
        } else {
            return "D";
        }
    }
}
