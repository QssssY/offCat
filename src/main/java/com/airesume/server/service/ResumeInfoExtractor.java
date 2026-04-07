package com.airesume.server.service;

import com.airesume.server.dto.resume.ResumeDiagnosisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简历信息提取服务
 *
 * 【用途】
 * 从简历原文中使用正则表达式提取基础信息
 * 作为 AI 返回结果的补充和备份，确保 basicInfoDetails 字段有值
 *
 * 【提取字段】
 * - name（姓名）
 * - email（邮箱）
 * - phone（电话）
 * - location（所在地）
 * - currentCompany（当前公司）
 * - github（GitHub链接）
 * - blog（博客/网站链接）
 */
@Slf4j
@Service
public class ResumeInfoExtractor {

    /**
     * 邮箱正则表达式
     * 匹配常见的邮箱格式，如：xxx@xxx.com, xxx@xxx.cn, xxx@xxx.edu.cn 等
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"
    );

    /**
     * 手机号正则表达式
     * 匹配中国大陆手机号，支持：
     * - 11位纯数字
     * - 带前缀 +86 或 86
     * - 带分隔符 138-0013-8000 或 138 0013 8000
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?86[\\s-]?)?1[3-9]\\d[\\s-]?\\d{4}[\\s-]?\\d{4}"
    );

    /**
     * GitHub链接正则表达式
     * 匹配 github.com 或 github.io 链接
     */
    private static final Pattern GITHUB_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?github\\.com/[a-zA-Z0-9_-]+|" +
            "(?:https?://)?[a-zA-Z0-9_-]+\\.github\\.io/?[a-zA-Z0-9_/-]*"
    );

    /**
     * 博客/网站链接正则表达式
     * 匹配常见的个人网站/博客链接，排除 GitHub 链接
     */
    private static final Pattern BLOG_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?[a-zA-Z0-9][a-zA-Z0-9-]*\\.[a-zA-Z]{2,6}(?:/[^\\s]*)?"
    );

    /**
     * 姓名正则表达式
     * 匹配2-4个中文字符的姓名，结合上下文关键词
     */
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(?:姓名|名字|Name)[：:：]?\\s*([\\u4e00-\\u9fa5]{2,4})|" +
            "^\\s*([\\u4e00-\\u9fa5]{2,4})\\s*$"
    );

    /**
     * 所在地正则表达式
     * 匹配省市区格式的地址
     */
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "(?:所在地|住址|地址|居住地址|Location)[：:：]?\\s*([\\u4e00-\\u9fa50-9]+(?:省|市|区|县)[\\u4e00-\\u9fa50-9]*)|" +
            "([\\u4e00-\\u9fa5]{2,3}(?:省|市)[\\u4e00-\\u9fa50-9]*(?:区|县)?)"
    );

    /**
     * 当前公司正则表达式
     * 匹配公司信息，结合上下文关键词
     */
    private static final Pattern COMPANY_PATTERN = Pattern.compile(
            "(?:公司|当前公司|任职公司|工作单位|Company)[：:：]?\\s*([\\u4e00-\\u9fa5a-zA-Z0-9（）()]+(?:有限公司|股份有限公司|集团|科技)?)"
    );

    /**
     * 从简历文本中提取基础信息
     *
     * @param resumeText 简历原文文本
     * @return 基础信息详情对象
     */
    public ResumeDiagnosisResult.BasicInfoDetails extractBasicInfo(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            log.warn("简历文本为空，无法提取基础信息");
            return ResumeDiagnosisResult.BasicInfoDetails.builder().build();
        }

        ResumeDiagnosisResult.BasicInfoDetails details = ResumeDiagnosisResult.BasicInfoDetails.builder().build();

        // 提取邮箱
        details.setEmail(extractEmail(resumeText));

        // 提取电话
        details.setPhone(extractPhone(resumeText));

        // 提取GitHub
        details.setGithub(extractGithub(resumeText));

        // 提取博客（排除GitHub链接）
        details.setBlog(extractBlog(resumeText, details.getGithub()));

        // 提取姓名
        details.setName(extractName(resumeText));

        // 提取所在地
        details.setLocation(extractLocation(resumeText));

        // 提取当前公司
        details.setCurrentCompany(extractCompany(resumeText));

        log.info("简历基础信息提取完成: name={}, email={}, phone={}, location={}, company={}",
                maskInfo(details.getName()),
                maskEmail(details.getEmail()),
                maskPhone(details.getPhone()),
                details.getLocation(),
                details.getCurrentCompany());

        return details;
    }

    /**
     * 提取邮箱
     */
    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            String email = matcher.group().trim();
            // 简单验证邮箱格式
            if (email.contains("@") && email.contains(".")) {
                return email;
            }
        }
        return "";
    }

    /**
     * 提取手机号
     */
    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (matcher.find()) {
            String phone = matcher.group().trim();
            // 清理分隔符，只保留数字和+号
            return phone.replaceAll("[\\s-]", "");
        }
        return "";
    }

    /**
     * 提取GitHub链接
     */
    private String extractGithub(String text) {
        Matcher matcher = GITHUB_PATTERN.matcher(text);
        if (matcher.find()) {
            String url = matcher.group().trim();
            // 确保有协议前缀
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }
            return url;
        }
        return "";
    }

    /**
     * 提取博客/网站链接（排除GitHub）
     */
    private String extractBlog(String text, String alreadyFoundGithub) {
        Matcher matcher = BLOG_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = matcher.group().trim();
            // 排除GitHub链接和常见的非个人网站
            if (url.contains("github.com") || url.contains("github.io")) {
                continue;
            }
            if (url.contains("baidu.com") || url.contains("qq.com") ||
                url.contains("163.com") || url.contains("gmail.com")) {
                continue;
            }
            // 确保有协议前缀
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }
            return url;
        }
        return "";
    }

    /**
     * 提取姓名
     */
    private String extractName(String text) {
        // 策略1：先尝试带关键词的匹配
        Matcher matcher = NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (name != null && name.length() >= 2 && name.length() <= 4) {
                return name;
            }
        }

        // 策略2：从简历开头几行找（简历通常开头是姓名）
        String[] lines = text.split("\\n");
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();
            if (line.matches("[\\u4e00-\\u9fa5]{2,4}")) {
                // 排除一些常见的非姓名字
                if (!line.equals("简历") && !line.equals("个人") &&
                    !line.equals("求职") && !line.equals("基本")) {
                    return line;
                }
            }
        }

        return "";
    }

    /**
     * 提取所在地
     */
    private String extractLocation(String text) {
        Matcher matcher = LOCATION_PATTERN.matcher(text);
        if (matcher.find()) {
            String location = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (location != null && !location.isBlank()) {
                return location.trim();
            }
        }
        return "";
    }

    /**
     * 提取当前公司
     */
    private String extractCompany(String text) {
        Matcher matcher = COMPANY_PATTERN.matcher(text);
        if (matcher.find()) {
            String company = matcher.group(1);
            if (company != null && !company.isBlank()) {
                return company.trim();
            }
        }
        return "";
    }

    /**
     * 脱敏姓名（用于日志）
     */
    private String maskInfo(String info) {
        if (info == null || info.isBlank()) {
            return "空";
        }
        if (info.length() <= 1) {
            return "*";
        }
        return info.charAt(0) + "*".repeat(info.length() - 1);
    }

    /**
     * 脱敏邮箱（用于日志）
     */
    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "空";
        }
        int atIndex = email.indexOf("@");
        if (atIndex > 0) {
            return (atIndex > 2 ? email.substring(0, 2) : email.substring(0, 1)) +
                   "***@" + email.substring(atIndex + 1);
        }
        return "***";
    }

    /**
     * 脱敏手机号（用于日志）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "空";
        }
        if (phone.length() >= 11) {
            return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        }
        return "***";
    }
}
