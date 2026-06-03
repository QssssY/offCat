-- 新增 tts_endpoint_path 列：存储自动探测到的 TTS 合成端点路径
ALTER TABLE user_ai_config
    ADD COLUMN tts_endpoint_path VARCHAR(128) DEFAULT '/audio/speech' COMMENT 'TTS 合成端点路径，由发现接口自动探测';
