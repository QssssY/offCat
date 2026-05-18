package com.airesume.server.util;

import lombok.experimental.UtilityClass;

/**
 * 文本规范化工具类
 * 统一处理换行、制表、零宽字符、多余空格等
 */
@UtilityClass
public class TextNormalizeUtil {

    /**
     * 规范化文本：统一换行符、去除零宽字符、压缩多余空格
     *
     * @param text 原始文本
     * @return 规范化后的文本
     */
    public String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\t', ' ')
                .replaceAll("[\\u200B-\\u200F\\uFEFF]", "")
                .replaceAll(" {2,}", " ")
                .trim();
    }
}
