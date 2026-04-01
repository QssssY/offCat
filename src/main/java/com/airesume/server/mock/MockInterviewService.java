package com.airesume.server.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 模拟面试服务
 * 用于在未接入真实大模型时生成模拟的面试官回复、评分和评价报告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockInterviewService {

    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    // 模拟面试官回复模板
    private static final List<String> MOCK_REPLIES = Arrays.asList(
            "这是一个很好的问题！让我从以下几个方面来考察你...",
            "感谢你的回答。接下来我们来聊聊另一个话题：你在项目中遇到过的最大挑战是什么？",
            "你的回答很有意思。能否举一个具体的例子来说明你是如何应用这个技术的？",
            "好的，我了解了。现在让我们进入下一个问题：请谈谈你对这个技术的理解。",
            "这个回答不错！那你有没有考虑过其他的实现方案呢？",
            "非常好！你的基础很扎实。接下来我们来聊聊系统设计相关的问题。",
            "这个问题你答得不错，那我再追问一下：这种方案有什么优缺点？",
            "好的，我明白了。让我们换一个话题，谈谈你在团队协作方面的经验。"
    );

    // 模拟面试问题
    private static final List<String> MOCK_QUESTIONS = Arrays.asList(
            "请介绍一下你自己？",
            "你为什么想要申请这个岗位？",
            "请谈谈你最有成就感的一个项目？",
            "你遇到过的最大技术挑战是什么？你是如何解决的？",
            "请描述一下你的技术栈和熟悉的工具？",
            "你对我们公司有什么了解？",
            "你在团队中通常扮演什么角色？",
            "请谈谈你的职业规划？"
    );

    /**
     * 生成模拟的面试官回复
     *
     * @param sessionId     会话ID
     * @param userMessage   用户消息
     * @param messageIndex  消息索引（用于生成连贯的对话）
     * @return 面试官回复
     */
    public String generateMockReply(String sessionId, String userMessage, int messageIndex) {
        log.info("Generating mock interview reply, sessionId: {}, messageIndex: {}", sessionId, messageIndex);

        String reply;
        if (messageIndex == 0) {
            // 第一条消息，面试官先提问
            reply = "你好！欢迎参加本次模拟面试。我是你的面试官。" +
                    "我们今天的面试岗位是Java开发工程师，难度为中级。" +
                    "首先，请你做一下自我介绍？";
        } else if (messageIndex % 2 == 1) {
            // 奇数索引：面试官根据用户回答继续提问
            int questionIndex = (messageIndex / 2) % MOCK_QUESTIONS.size();
            reply = MOCK_REPLIES.get(random.nextInt(MOCK_REPLIES.size())) +
                    "\n\n" + MOCK_QUESTIONS.get(questionIndex);
        } else {
            // 偶数索引：面试官继续追问
            reply = MOCK_REPLIES.get(random.nextInt(MOCK_REPLIES.size()));
        }

        log.debug("Mock reply generated successfully, sessionId: {}", sessionId);
        return reply;
    }

    /**
     * 生成模拟的面试开场白
     *
     * @param jobRole    面试岗位
     * @param difficulty 难度级别
     * @return 开场白内容
     */
    public String generateMockOpening(String jobRole, Integer difficulty) {
        String difficultyDesc = getDifficultyDesc(difficulty);
        return String.format("你好！欢迎参加本次模拟面试。我是你的面试官。\n\n" +
                "我们今天的面试岗位是%s，难度为%s。\n\n" +
                "面试过程中，请尽量详细地回答问题，展示你的思考过程。\n\n" +
                "准备好了吗？首先，请你做一下自我介绍？", jobRole, difficultyDesc);
    }

    /**
     * 生成模拟的综合评分
     *
     * @param sessionId 会话ID
     * @return 评分（0-100）
     */
    public int generateMockScore(String sessionId) {
        log.info("Generating mock interview score, sessionId: {}", sessionId);
        int score = 60 + random.nextInt(35); // 60-94分
        log.info("Mock score generated: {}, sessionId: {}", score, sessionId);
        return score;
    }

    /**
     * 生成模拟的综合评价报告（JSON格式）
     *
     * @param sessionId 会话ID
     * @param score     评分
     * @return JSON格式的评价报告
     */
    public String generateMockEvaluationReport(String sessionId, int score) {
        log.info("Generating mock evaluation report, sessionId: {}, score: {}", sessionId, score);

        try {
            ObjectNode report = objectMapper.createObjectNode();

            // 总体评价
            report.put("overallScore", score);
            report.put("level", getLevel(score));
            report.put("summary", getSummary(score));

            // 各维度评分
            ObjectNode dimensions = objectMapper.createObjectNode();
            dimensions.put("technicalDepth", 60 + random.nextInt(35));
            dimensions.put("problemSolving", 60 + random.nextInt(35));
            dimensions.put("communication", 60 + random.nextInt(35));
            dimensions.put("systemDesign", 55 + random.nextInt(35));
            report.set("dimensions", dimensions);

            // 优点
            ArrayNode strengths = objectMapper.createArrayNode();
            strengths.add("基础知识掌握扎实");
            strengths.add("表达清晰，逻辑通顺");
            strengths.add("具有一定的项目经验");
            report.set("strengths", strengths);

            // 待改进
            ArrayNode improvements = objectMapper.createArrayNode();
            improvements.add("系统设计能力有待提升");
            improvements.add("部分技术细节理解不够深入");
            report.set("improvements", improvements);

            // 建议
            ArrayNode suggestions = objectMapper.createArrayNode();
            suggestions.add("建议多学习系统设计相关知识");
            suggestions.add("建议深入了解技术底层原理");
            report.set("suggestions", suggestions);

            // 标记为模拟数据
            report.put("isMock", true);
            report.put("mockGeneratedAt", System.currentTimeMillis());

            String jsonReport = objectMapper.writeValueAsString(report);
            log.info("Mock evaluation report generated successfully, sessionId: {}", sessionId);
            return jsonReport;

        } catch (Exception e) {
            log.error("Failed to generate mock evaluation report, sessionId: {}", sessionId, e);
            throw new RuntimeException("生成模拟评价报告失败", e);
        }
    }

    /**
     * 根据分数获取等级
     *
     * @param score 分数
     * @return 等级
     */
    private String getLevel(int score) {
        if (score >= 90) {
            return "S - 优秀";
        } else if (score >= 80) {
            return "A - 良好";
        } else if (score >= 70) {
            return "B - 中等";
        } else if (score >= 60) {
            return "C - 及格";
        } else {
            return "D - 需改进";
        }
    }

    /**
     * 根据分数获取总结
     *
     * @param score 分数
     * @return 总结
     */
    private String getSummary(int score) {
        if (score >= 85) {
            return "本次面试表现优秀，技术基础扎实，表达能力强，具有较好的解决问题能力。";
        } else if (score >= 70) {
            return "本次面试表现良好，技术基础较为扎实，能够回答大部分问题，但在某些方面还有提升空间。";
        } else if (score >= 60) {
            return "本次面试表现基本合格，具备一定的技术基础，但还需要加强系统学习和实践。";
        } else {
            return "本次面试还有较大提升空间，建议加强基础知识的学习和项目经验的积累。";
        }
    }

    /**
     * 获取难度描述
     *
     * @param difficulty 难度级别
     * @return 难度描述
     */
    private String getDifficultyDesc(Integer difficulty) {
        return switch (difficulty) {
            case 1 -> "初级";
            case 2 -> "中级";
            case 3 -> "高级";
            default -> "未知";
        };
    }
}
