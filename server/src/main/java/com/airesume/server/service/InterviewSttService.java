package com.airesume.server.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 语音面试云端语音识别（STT）服务。
 * <p>
 * 只服务语音面试的语音输入兜底：浏览器 Web Speech 不可用时，前端录音上传，由本服务调用云端
 * OpenAI 兼容的语音转文字接口返回文本。与面试对话 AI、语音播报 TTS 完全独立。
 */
public interface InterviewSttService {

    /**
     * 云端语音识别当前是否可用（已启用且配置了 API Key）。
     *
     * @return true 表示前端可在浏览器识别失败时走云端兜底
     */
    boolean isCloudSttAvailable();

    /**
     * 将音频转写为文本。
     *
     * @param audio    前端上传的单段音频
     * @param language 识别语言（如 zh、en），为空时由上游自动判定
     * @return 识别出的文本，可能为空字符串（表示这段音频没有有效语音）
     */
    String transcribe(MultipartFile audio, String language);
}
