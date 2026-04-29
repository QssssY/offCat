package com.airesume.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * AI Resume 项目启动类
 *
 * 项目简介：
 * - 智能模拟面试与简历诊断系统
 * - 支持简历上传、AI 诊断、模拟面试、评分报告等功能
 * - 采用前后端分离架构，后端基于 Spring Boot 3.x
 *
 * 核心模块：
 * 1. 认证授权模块（Auth）- 用户登录、JWT 认证
 * 2. 额度管理模块（Quota）- 简历诊断和面试次数管理
 * 3. 简历诊断模块（Resume）- PDF 上传、AI 诊断、结果展示
 * 4. 模拟面试模块（Interview）- 会话管理、AI 面试官、评分报告
 * 5. 管理端模块（Admin）- Prompt 管理、用户管理
 *
 * 技术栈：
 * - Spring Boot 3.2.3
 * - Spring Security + JWT
 * - MyBatis-Plus
 * - MySQL + Redis
 * - RabbitMQ（异步任务）
 * - Reactor（流式响应）
 *
 * @author AI Resume Team
 */
@SpringBootApplication
@EnableConfigurationProperties
@MapperScan("com.airesume.server.mapper")
public class ServerApplication {

    /**
     * 项目入口方法
     *
     * 启动说明：
     * 1. 确保 MySQL、Redis、RabbitMQ 已启动
     * 2. 配置数据库连接（application.yml）
     * 3. 如需使用真实 AI，配置 API Key 环境变量
     * 4. 访问 http://localhost:8080 验证服务启动成功
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

}
