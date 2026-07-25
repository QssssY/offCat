-- TTS Provider 预设支持：新增 tts_provider 列标识 TTS 服务商类型
ALTER TABLE user_ai_config
    ADD COLUMN tts_provider VARCHAR(32) DEFAULT NULL COMMENT 'TTS 提供商标识：openai/mimo，NULL 或空按 OpenAI 兜底'
    AFTER tts_endpoint_path;
