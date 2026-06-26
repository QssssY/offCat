package com.airesume.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 简历 PDF 混合解析配置。
 * 统一管理文本阈值、图片页优先策略和 OCR 参数。
 */
@Component
@ConfigurationProperties(prefix = "app.resume")
public class ResumeParseConfig {

    /**
     * 解析策略配置。
     */
    private final Parse parse = new Parse();

    /**
     * OCR 配置。
     */
    private final Ocr ocr = new Ocr();

    public Parse getParse() {
        return parse;
    }

    public Ocr getOcr() {
        return ocr;
    }

    public static class Parse {

        /**
         * 页文本长度达到该阈值时，直接视为文本页。
         */
        private int textThreshold = 50;

        /**
         * 图片页优先策略，默认先走多模态。
         */
        private String imagePriority = "multimodal-first";

        public int getTextThreshold() {
            return textThreshold;
        }

        public void setTextThreshold(int textThreshold) {
            this.textThreshold = textThreshold;
        }

        public String getImagePriority() {
            return imagePriority;
        }

        public void setImagePriority(String imagePriority) {
            this.imagePriority = imagePriority;
        }
    }

    public static class Ocr {

        /**
         * 是否启用 OCR 兜底。
         */
        private boolean enabled = true;

        /**
         * Tesseract 可执行命令。
         */
        private String command = "tesseract";

        /**
         * OCR 语言包。
         */
        private String lang = "chi_sim+eng";

        /**
         * PDF 渲染 DPI。
         */
        private int dpi = 200;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public int getDpi() {
            return dpi;
        }

        public void setDpi(int dpi) {
            this.dpi = dpi;
        }
    }
}
