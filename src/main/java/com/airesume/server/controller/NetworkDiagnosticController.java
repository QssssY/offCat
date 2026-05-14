package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.common.util.NetworkDiagnosticUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网络诊断控制器
 *
 * 所属模块：诊断工具模块 - REST 接口层
 * 职责：提供网络诊断相关的 API 接口，帮助排查 DNS、端口、代理等问题
 *
 * 【接口说明】
 * - GET /api/diagnostic/network - 执行完整网络诊断
 * - GET /api/diagnostic/dns - DNS 解析检测
 * - GET /api/diagnostic/proxy - 代理配置检测
 * - GET /api/diagnostic/ports - 端口占用检测
 * - GET /api/diagnostic/http - HTTP 连通性检测
 * - GET /api/diagnostic/ping - 执行 ping 命令
 * - GET /api/diagnostic/nslookup - 执行 nslookup 命令
 *
 * @author AI Resume Team
 */
@RestController
@RequestMapping("/api/diagnostic")
@Slf4j
@Profile("dev")
public class NetworkDiagnosticController {

    /**
     * 执行完整网络诊断
     *
     * 【功能】
     * 一键执行所有网络检测项，生成完整诊断报告
     *
     * 【检测项】
     * 1. 基础环境信息（操作系统、Java 版本等）
     * 2. 代理配置检测
     * 3. DNS 解析检测
     * 4. HTTP 连通性检测
     * 5. 常用端口占用检测
     *
     * @param targetUrl 目标 URL（可选，默认使用豆包 API 地址）
     * @return 诊断报告字符串
     */
    @GetMapping("/network")
    public Result<String> runFullDiagnosis(
            @RequestParam(required = false) String targetUrl) {

        String url = targetUrl != null ? targetUrl : "https://ark.cn-beijing.volces.com/api/coding/v3";
        log.info("[网络诊断] 执行完整诊断, targetUrl: {}", url);

        try {
            String report = NetworkDiagnosticUtil.runFullDiagnosis(url);
            return Result.success("网络诊断完成", report);
        } catch (Exception e) {
            log.error("[网络诊断] 完整诊断执行失败", e);
            return Result.error("网络诊断执行失败: " + e.getMessage());
        }
    }

    /**
     * DNS 解析检测
     *
     * 【功能】
     * 检测指定主机名是否能正常解析为 IP 地址
     *
     * @param host 主机名（如 ark.cn-beijing.volces.com）
     * @return DNS 解析结果
     */
    @GetMapping("/dns")
    public Result<String> checkDns(
            @RequestParam(defaultValue = "ark.cn-beijing.volces.com") String host) {

        log.info("[网络诊断] DNS 解析检测, host: {}", host);

        try {
            String result = NetworkDiagnosticUtil.checkDnsResolution(host);
            return Result.success("DNS 检测完成", result);
        } catch (Exception e) {
            log.error("[网络诊断] DNS 检测失败", e);
            return Result.error("DNS 检测失败: " + e.getMessage());
        }
    }

    /**
     * 代理配置检测
     *
     * 【功能】
     * 检查当前 JVM 和系统的代理配置
     *
     * @return 代理配置信息
     */
    @GetMapping("/proxy")
    public Result<String> checkProxy() {
        log.info("[网络诊断] 代理配置检测");

        try {
            String result = NetworkDiagnosticUtil.checkProxySettings();
            return Result.success("代理配置检测完成", result);
        } catch (Exception e) {
            log.error("[网络诊断] 代理配置检测失败", e);
            return Result.error("代理配置检测失败: " + e.getMessage());
        }
    }

    /**
     * 常用端口占用检测
     *
     * 【功能】
     * 检查常用端口（8080、3306、6379、5672 等）是否被占用
     *
     * @return 端口占用情况
     */
    @GetMapping("/ports")
    public Result<String> checkPorts() {
        log.info("[网络诊断] 常用端口占用检测");

        try {
            String result = NetworkDiagnosticUtil.checkCommonPorts();
            return Result.success("端口检测完成", result);
        } catch (Exception e) {
            log.error("[网络诊断] 端口检测失败", e);
            return Result.error("端口检测失败: " + e.getMessage());
        }
    }

    /**
     * HTTP 连通性检测
     *
     * 【功能】
     * 检测能否正常连接到指定 URL
     *
     * @param url 目标 URL
     * @return 连通性检测结果
     */
    @GetMapping("/http")
    public Result<String> checkHttp(
            @RequestParam(defaultValue = "https://ark.cn-beijing.volces.com/api/coding/v3") String url) {

        log.info("[网络诊断] HTTP 连通性检测, url: {}", url);

        try {
            String result = NetworkDiagnosticUtil.checkHttpConnectivity(url);
            return Result.success("HTTP 连通性检测完成", result);
        } catch (Exception e) {
            log.error("[网络诊断] HTTP 连通性检测失败", e);
            return Result.error("HTTP 连通性检测失败: " + e.getMessage());
        }
    }

    /**
     * 执行 ping 命令
     *
     * 【功能】
     * 调用系统 ping 命令检测网络连通性
     *
     * @param host 目标主机
     * @param count ping 次数
     * @return ping 输出
     */
    @GetMapping("/ping")
    public Result<List<String>> ping(
            @RequestParam(defaultValue = "ark.cn-beijing.volces.com") String host,
            @RequestParam(defaultValue = "4") int count) {

        log.info("[网络诊断] 执行 ping, host: {}, count: {}", host, count);

        try {
            List<String> result = NetworkDiagnosticUtil.ping(host, count);
            return Result.success("ping 执行完成", result);
        } catch (Exception e) {
            log.error("[网络诊断] ping 执行失败", e);
            return Result.error("ping 执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行 nslookup 命令
     *
     * 【功能】
     * 调用系统 nslookup 命令查询 DNS 记录
     *
     * @param host 目标主机
     * @return nslookup 输出
     */
    @GetMapping("/nslookup")
    public Result<List<String>> nslookup(
            @RequestParam(defaultValue = "ark.cn-beijing.volces.com") String host) {

        log.info("[网络诊断] 执行 nslookup, host: {}", host);

        try {
            List<String> result = NetworkDiagnosticUtil.nslookup(host);
            return Result.success("nslookup 执行完成", result);
        } catch (Exception e) {
            log.error("[网络诊断] nslookup 执行失败", e);
            return Result.error("nslookup 执行失败: " + e.getMessage());
        }
    }

    /**
     * 端口占用检测（单个端口）
     *
     * 【功能】
     * 检查指定端口是否被占用
     *
     * @param port 端口号
     * @return 端口占用状态
     */
    @GetMapping("/port/{port}")
    public Result<Map<String, Object>> checkPort(
            @PathVariable int port) {

        log.info("[网络诊断] 单个端口检测, port: {}", port);

        try {
            boolean inUse = NetworkDiagnosticUtil.isPortInUse(port);
            Map<String, Object> result = new HashMap<>();
            result.put("port", port);
            result.put("inUse", inUse);
            result.put("status", inUse ? "已占用" : "可用");
            return Result.success(result);
        } catch (Exception e) {
            log.error("[网络诊断] 端口检测失败", e);
            return Result.error("端口检测失败: " + e.getMessage());
        }
    }
}
