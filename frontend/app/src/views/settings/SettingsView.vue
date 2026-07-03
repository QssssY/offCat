<template>
  <div class="settings-view">
    <header class="settings-header">
      <div>
        <h1>设置中心</h1>
        <p>管理账号、安全、面试偏好、隐私数据和本机显示通知偏好。</p>
      </div>
    </header>

    <div class="settings-layout settings-workspace">
      <aside class="settings-nav" aria-label="设置分组">
        <button
          v-for="section in sections"
          :key="section.key"
          type="button"
          class="settings-nav-item"
          :class="{ active: activeSection === section.key }"
          @click="activeSection = section.key"
        >
          <FeatureIcon
            :name="section.icon"
            size="md"
            class="settings-nav-icon"
            loading="eager"
            fetch-priority="auto"
          />
          <span>{{ section.label }}</span>
        </button>
      </aside>

      <main class="settings-content">
        <Transition name="section-fade" mode="out-in">
        <section v-if="activeSection === 'profile'" key="profile" class="settings-panel" aria-labelledby="profile-title">
          <div class="panel-heading">
            <div>
              <h2 id="profile-title">账号资料</h2>
              <p>查看当前账号信息、订阅状态和注册时间。</p>
            </div>
          </div>

          <div class="settings-panel-body profile-workspace">
            <div class="profile-overview-card">
              <OptimizedImage :sources="optimizedImages.userAvatar" alt="用户头像" img-class="profile-avatar" />
              <div class="profile-main">
                <div class="profile-name-row">
                  <div class="profile-name">{{ displayName }}</div>
                  <n-tag :type="roleTagType" round :bordered="false">{{ roleText }}</n-tag>
                </div>
                <div class="profile-meta">{{ userInfo?.username || '--' }}</div>
                <p>当前账号已接入简历诊断、模拟面试、通知和成长数据能力，设置会保存在本机和账号配置中。</p>
              </div>
            </div>

            <div class="info-grid profile-info-grid">
              <div class="info-item">
                <span>账号状态</span>
                <strong>{{ statusText }}</strong>
              </div>
              <div class="info-item">
                <span>当前身份</span>
                <strong>{{ roleText }}</strong>
              </div>
              <div class="info-item">
                <span>订阅套餐</span>
                <strong>{{ membershipPlanText }}</strong>
              </div>
              <div class="info-item">
                <span>注册时间</span>
                <strong>{{ profileRegisterTimeText }}</strong>
              </div>
            </div>

            <div class="profile-support-grid" aria-label="账号能力说明">
              <div class="profile-support-card">
                <strong>账号内权益</strong>
                <span>会员身份、订阅套餐和每日额度跟随当前账号，换设备登录后仍以账号资料为准。</span>
              </div>
              <div class="profile-support-card">
                <strong>本机偏好</strong>
                <span>主题、语音、通知显示和面试默认项保存在当前浏览器，适合按设备分别配置。</span>
              </div>
              <div class="profile-support-card">
                <strong>常用数据</strong>
                <span>简历诊断、模拟面试、成长记录和通知中心会围绕当前账号持续归档。</span>
              </div>
            </div>
          </div>
        </section>

        <section v-else-if="activeSection === 'interview'" key="interview" class="settings-panel" aria-labelledby="interview-title">
          <div class="panel-heading">
            <FeatureIcon name="ai-interviewer" size="lg" class="panel-heading-icon" />
            <div class="panel-heading-copy">
              <h2 id="interview-title">面试偏好</h2>
              <p>设置进入模拟面试时优先带入的默认配置，偏好仅保存在当前浏览器。</p>
            </div>
          </div>

          <div class="settings-panel-body">
            <div class="sub-nav-tabs" role="tablist" aria-label="面试偏好子分组">
              <button
                v-for="tab in interviewSubTabs"
                :key="tab.key"
                type="button"
                class="sub-nav-tab"
                :class="{ active: interviewSubTab === tab.key }"
                role="tab"
                :aria-selected="interviewSubTab === tab.key"
                @click="interviewSubTab = tab.key"
              >
                {{ tab.label }}
              </button>
            </div>

            <Transition name="security-panel" mode="out-in">
              <div :key="interviewSubTab" class="preference-list">
              <template v-if="interviewSubTab === 'basic'">
                <div class="preference-row stacked">
                  <div>
                    <strong>默认面试岗位</strong>
                    <span>只在岗位仍处于启用状态时自动回填，避免旧岗位配置污染新会话。</span>
                  </div>
                  <n-select
                    v-model:value="interviewPreferenceForm.defaultInterviewJobRoleCode"
                    class="preference-select"
                    filterable
                    :options="defaultInterviewJobSelectOptions"
                    @update:value="handleDefaultJobChange"
                  />
                </div>
                <div class="preference-row stacked">
                  <div>
                    <strong>默认面试级别</strong>
                    <span>进入面试入口页时默认选中的难度级别。</span>
                  </div>
                  <n-select
                    v-model:value="interviewPreferenceForm.defaultInterviewDifficulty"
                    class="preference-select"
                    :options="difficultyPreferenceOptions"
                    @update:value="handleInterviewPreferenceSave"
                  />
                </div>
                <div class="preference-row stacked">
                  <div>
                    <strong>默认面试模式</strong>
                    <span>进入面试入口页时默认选中的面试官模式。</span>
                  </div>
                  <n-select
                    v-model:value="interviewPreferenceForm.defaultInterviewMode"
                    class="preference-select"
                    :options="interviewModeOptions"
                    @update:value="handleInterviewPreferenceSave"
                  />
                </div>
                <div class="preference-row stacked">
                  <div>
                    <strong>默认反馈模式</strong>
                    <span>进入面试入口页时默认选中的反馈节奏。</span>
                  </div>
                  <n-select
                    v-model:value="interviewPreferenceForm.defaultFeedbackMode"
                    class="preference-select"
                    :options="feedbackModeOptions"
                    @update:value="handleInterviewPreferenceSave"
                  />
                </div>
                <div class="preference-row stacked">
                  <div>
                    <strong>默认交互方式</strong>
                    <span>进入模拟面试入口页时默认选择文字面试或语音面试；浏览器不支持语音能力时会保持文字面试。</span>
                  </div>
                  <n-select
                    v-model:value="interviewPreferenceForm.defaultInterviewInteractionType"
                    class="preference-select"
                    :options="interactionModeOptions"
                    @update:value="handleInterviewPreferenceSave"
                  />
                </div>
              </template>

              <template v-else-if="interviewSubTab === 'voice'">
              <div class="preference-row stacked voice-preference-row">
                <div class="voice-preference-main">
                  <div class="voice-preference-copy">
                    <strong>AI 播报声音</strong>
                    <span>可选择浏览器系统语音，也可切换 EdgeTTS 云端音色；浏览器偏好只保存在本机。</span>
                  </div>
                  <div class="voice-control">
                    <n-select
                      v-model:value="interviewPreferenceForm.voicePreferredType"
                      class="preference-select"
                      :options="voicePreferredTypeOptions"
                      @update:value="handleVoicePreferredTypeChange"
                    />
                    <n-button
                      type="primary"
                      secondary
                      class="voice-preview-button"
                      title="试听"
                      aria-label="试听当前 AI 播报声音"
                      :disabled="isCloudTtsVoiceSelected ? ttsPreviewing : !previewTextToSpeech.isSupported.value"
                      @click="handleVoicePreview"
                    >
                    <FeatureIcon name="announcement" size="md" class="voice-preview-icon" />
                    </n-button>
                  </div>
                </div>
                <p
                  class="voice-selection-status"
                  :class="{ degraded: !isCloudTtsVoiceSelected && (previewTextToSpeech.voicePreferenceStatus.value.isDegraded || isChromeBrowserVoiceLimited) }"
                  data-testid="browser-tts-voice-status"
                >
                  {{ browserTtsVoiceStatusText }}
                </p>
              </div>
              <div
                v-if="interviewPreferenceForm.voicePreferredType === 'custom'"
                class="preference-row stacked"
              >
                <div>
                  <strong>浏览器 voice 列表</strong>
                  <span>不同浏览器和系统安装的语音包不同，找不到已选音色时会回到默认中文自然音色。</span>
                </div>
                <n-select
                  v-model:value="selectedBrowserVoiceKey"
                  class="preference-select browser-voice-select"
                  filterable
                  :options="browserVoiceOptions"
                  :disabled="browserVoiceOptions.length === 0"
                  placeholder="当前浏览器暂无可用 voice"
                />
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>AI 播报语速</strong>
                  <span>默认略慢，便于听清面试官问题。</span>
                </div>
                <n-slider
                  v-model:value="interviewPreferenceForm.voiceSpeakingRate"
                  class="preference-slider"
                  :min="0.7"
                  :max="1.2"
                  :step="0.01"
                  :format-tooltip="formatSpeechRate"
                  @update:value="handleInterviewPreferenceSave"
                />
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>AI 播报音调</strong>
                  <span>控制浏览器语音合成的音调高低。</span>
                </div>
                <n-slider
                  v-model:value="interviewPreferenceForm.voicePitch"
                  class="preference-slider"
                  :min="0.8"
                  :max="1.3"
                  :step="0.01"
                  :format-tooltip="formatSpeechPitch"
                  @update:value="handleInterviewPreferenceSave"
                />
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>AI 播报音量</strong>
                  <span>只影响浏览器 TTS 播报音量，不改变系统音量。</span>
                </div>
                <n-slider
                  v-model:value="interviewPreferenceForm.voiceVolume"
                  class="preference-slider"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :format-tooltip="formatSpeechVolume"
                  @update:value="handleInterviewPreferenceSave"
                />
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>取消静音后恢复</strong>
                  <span>选择取消静音后是否立即继续收音。</span>
                </div>
                <n-select
                  v-model:value="interviewPreferenceForm.voiceMuteResumeMode"
                  class="preference-select"
                  :options="voiceMuteResumeOptions"
                  @update:value="handleInterviewPreferenceSave"
                />
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>自动提交等待时间</strong>
                  <span>用户停止说话后等待多久自动发送本轮回答。</span>
                </div>
                <n-select
                  v-model:value="interviewPreferenceForm.voiceAutoSubmitDelayMs"
                  class="preference-select"
                  :options="voiceAutoSubmitDelayOptions"
                  @update:value="handleInterviewPreferenceSave"
                />
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>语音识别语言</strong>
                  <span>自动模式会在外企面试官使用英文，其它面试模式使用中文普通话。</span>
                </div>
                <n-select
                  v-model:value="interviewPreferenceForm.voiceRecognitionLanguage"
                  class="preference-select"
                  :options="voiceRecognitionLanguageOptions"
                  @update:value="handleInterviewPreferenceSave"
                />
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>重置语音偏好</strong>
                  <span>恢复 AI 播报声音、语速、音调、音量、静音恢复、自动提交和识别语言的默认值。</span>
                </div>
                <n-button secondary class="preference-action" @click="handleVoicePreferenceReset">
                  重置偏好
                </n-button>
              </div>
              </template>

              </div>
            </Transition>
          </div>
        </section>

        <section v-else-if="activeSection === 'customAi'" key="customAi" class="settings-panel" aria-labelledby="custom-ai-title">
          <div class="panel-heading">
            <FeatureIcon name="membership-center" size="lg" class="panel-heading-icon" />
            <div class="panel-heading-copy">
              <h2 id="custom-ai-title">自定义 AI 接入</h2>
              <p>接入你自己的 OpenAI 兼容 API，命中时不消耗平台额度。</p>
            </div>
            <n-tooltip trigger="hover" placement="top">
              <template #trigger>
              <n-button
                secondary
                circle
                class="data-overview-refresh-btn"
                :class="{ 'is-refreshing': userAiConfigLoading }"
                :disabled="userAiConfigLoading"
                @click="fetchUserAiConfigState"
              >
              <FeatureIcon name="retry" size="md" class="settings-refresh-icon" />
              </n-button>
              </template>
              刷新配置
            </n-tooltip>
          </div>

          <div class="settings-panel-body">
            <div class="cai-usage-bar">
              <div class="cai-usage-info">
                <span class="cai-usage-label">今日调用</span>
                <span class="cai-usage-numbers">
                  <strong>{{ userAiUsage?.used ?? 0 }}</strong>
                  <span class="cai-usage-sep">/</span>
                  <span>{{ userAiUsage?.limit ?? 50 }}</span>
                </span>
              </div>
              <div class="cai-usage-track">
                <div
                  class="cai-usage-fill"
                  :style="{ width: Math.min(100, ((userAiUsage?.used ?? 0) / Math.max(1, userAiUsage?.limit ?? 50)) * 100) + '%' }"
                  :class="{ warning: (userAiUsage?.used ?? 0) / Math.max(1, userAiUsage?.limit ?? 50) > 0.8 }"
                />
              </div>
              <span class="cai-usage-remaining">剩余 {{ userAiUsage?.remaining ?? Math.max(0, (userAiUsage?.limit ?? 50) - (userAiUsage?.used ?? 0)) }} 次</span>
            </div>

            <div class="cai-type-slots" aria-label="已保存的自定义 AI 配置">
              <div
                v-for="slot in userAiConfigSlots"
                :key="slot.type"
                class="cai-slot"
                :class="{ active: slot.config, editing: userAiConfigForm.configType === slot.type }"
                @click="handleUserAiConfigTypeChange(slot.type)"
              >
                <div class="cai-slot-header">
                  <FeatureIcon :name="slot.icon" size="sm" class="cai-slot-icon" />
                  <span class="cai-slot-type">{{ slot.label }}</span>
                  <n-switch
                    v-if="slot.config"
                    size="small"
                    :value="Boolean(slot.config.enabled)"
                    :loading="userAiToggleLoadingType === slot.type"
                    @click.stop
                    @update:value="(value) => handleUserAiConfigToggle(slot.type, value)"
                  />
                </div>
                <template v-if="slot.config">
                  <div class="cai-slot-detail">
                    <span class="cai-slot-name">{{ slot.config.providerName || slot.config.model || '未命名' }}</span>
                    <span class="cai-slot-endpoint">{{ slot.config.baseUrl }}</span>
                    <span v-if="slot.type !== 'resume' && slot.config.ttsConfigured" class="cai-slot-tts">已配置 TTS</span>
                  </div>
                  <div class="cai-slot-footer">
                    <span
                      class="cai-slot-status"
                      :class="slot.config.verificationStatus === 'verified' ? 'ok' : slot.config.verificationStatus === 'failed' ? 'fail' : 'pending'"
                    >
                      <span class="cai-status-dot" />
                      {{ userAiVerificationText(slot.config.verificationStatus) }}
                    </span>
                    <button class="cai-slot-del" @click.stop="handleUserAiConfigDelete(slot.type)" title="删除配置">×</button>
                  </div>
                </template>
                <div v-else class="cai-slot-empty">
                  <span>+ 点击配置</span>
                </div>
              </div>
            </div>

            <div class="cai-form-card">
              <div class="cai-form-title">
                <FeatureIcon :name="currentSlotIconName" size="sm" />
                <span>{{ userAiConfigTypeLabelMap[userAiConfigForm.configType] || '通用兜底' }} 配置</span>
                <n-button
                  size="small"
                  :loading="userAiModelFetching"
                  secondary
                  class="cai-form-title-action"
                  :disabled="!userAiConfigForm.baseUrl?.trim() || !userAiConfigForm.apiKey?.trim()"
                  @click="handleUserAiModelsFetch"
                >获取模型</n-button>
              </div>

              <el-form class="cai-form" label-position="top">
                <div class="cai-form-row-2col">
                  <el-form-item label="配置名称">
                    <el-input v-model.trim="userAiConfigForm.providerName" maxlength="64" show-word-limit placeholder="例如 DeepSeek / OpenRouter" />
                  </el-form-item>
                  <el-form-item label="模型">
                    <el-select
                      v-if="userAiModelOptions.length"
                      v-model="userAiConfigForm.model"
                      filterable
                      allow-create
                      default-first-option
                      clearable
                      placeholder="选择或输入模型名"
                    >
                      <el-option
                        v-for="model in userAiModelOptions"
                        :key="model.id"
                        :label="model.name || model.id"
                        :value="model.id"
                      />
                    </el-select>
                    <el-input
                      v-else
                      v-model.trim="userAiConfigForm.model"
                      maxlength="128"
                      placeholder="例如 deepseek-chat / gpt-4o"
                    />
                  </el-form-item>
                </div>
                <el-form-item label="API 基础地址">
                  <el-input v-model.trim="userAiConfigForm.baseUrl" maxlength="512" placeholder="https://api.example.com/v1" />
                </el-form-item>
                <el-form-item label="API Key">
                  <el-input v-model="userAiConfigForm.apiKey" type="password" show-password maxlength="1024" placeholder="保存或更新配置时必须填写真实 Key" />
                </el-form-item>

                <div class="cai-form-toggle-row">
                  <div class="cai-form-toggle-info">
                    <strong>支持图片识别（Vision）</strong>
                    <span>仅简历配置使用此能力，面试与通用配置自动忽略。</span>
                  </div>
                  <n-switch v-model:value="userAiConfigForm.supportsMultimodal" />
                </div>

                <div v-if="isTtsSupportedAiConfig" class="cai-tts-section">
                  <button
                    type="button"
                    class="cai-tts-heading"
                    data-testid="custom-ai-tts-toggle"
                    :aria-expanded="userTtsConfigExpanded"
                    @click="userTtsConfigExpanded = !userTtsConfigExpanded"
                  >
                    <div class="cai-tts-heading-copy">
                      <strong>TTS 语音合成</strong>
                      <span>配置 OpenAI 兼容 /audio/speech，用于后续接入云端面试官播报。</span>
                    </div>
                    <span class="cai-tts-status" :class="{ active: hasAnyTtsFormValue }">
                      {{ hasAnyTtsFormValue ? '已填写' : '未启用' }}
                    </span>
                    <FeatureIcon name="expand" size="xs" class="cai-tts-toggle-icon" :class="{ expanded: userTtsConfigExpanded }" />
                  </button>
                  <p v-if="userTtsRuntimeStatusText" class="cai-tts-runtime-status">
                    {{ userTtsRuntimeStatusText }}
                  </p>
                  <div v-if="userTtsConfigExpanded" class="cai-tts-body">
                    <div class="cai-tts-discover-row">
                      <el-form-item label="TTS 服务商" class="cai-tts-provider-field">
                        <el-select
                          v-model="userAiConfigForm.ttsProvider"
                          placeholder="选择服务商"
                          @change="handleTtsProviderChange"
                        >
                          <el-option
                            v-for="p in TTS_PROVIDER_PRESETS"
                            :key="p.value"
                            :label="p.label"
                            :value="p.value"
                            :disabled="p.disabled"
                          />
                        </el-select>
                      </el-form-item>
                      <el-button
                        class="cai-tts-discover-btn"
                        :loading="ttsDiscovering"
                        @click="handleTtsDiscovery"
                        :disabled="!userAiConfigForm.ttsBaseUrl?.trim() || (!isUserEdgeTtsProvider && !userAiConfigForm.ttsApiKey?.trim())"
                      >获取模型/音色</el-button>
                    </div>
                    <div class="cai-form-row-2col">
                      <el-form-item label="TTS 地址">
                        <el-input v-model.trim="userAiConfigForm.ttsBaseUrl" maxlength="512" placeholder="https://api.example.com/v1" />
                      </el-form-item>
                      <el-form-item label="TTS 模型">
                        <el-select
                          v-if="ttsDiscoveryResult?.success && ttsDiscoveryResult.models?.length"
                          v-model="userAiConfigForm.ttsModel"
                          filterable
                          allow-create
                          default-first-option
                          placeholder="选择或输入模型名"
                        >
                          <el-option
                            v-for="m in ttsDiscoveryResult.models"
                            :key="m.id"
                            :label="m.name"
                            :value="m.id"
                          />
                        </el-select>
                        <el-input
                          v-else
                          v-model.trim="userAiConfigForm.ttsModel"
                          maxlength="128"
                          placeholder="例如 tts-1 / doubao-tts"
                        />
                      </el-form-item>
                    </div>
                    <div class="cai-form-row-2col">
                      <el-form-item label="TTS API Key">
                        <el-input v-model="userAiConfigForm.ttsApiKey" type="password" show-password maxlength="1024" placeholder="测试或保存 TTS 时必须填写真实 Key" />
                      </el-form-item>
                      <el-form-item label="音色 ID" class="cai-tts-voice-field">
                        <div class="cai-tts-voice-row">
                          <el-select
                            v-if="ttsDiscoveryResult?.success && ttsDiscoveryResult.voices?.length"
                            v-model="userAiConfigForm.ttsVoiceId"
                            filterable
                            allow-create
                            default-first-option
                            placeholder="选择或输入音色"
                          >
                            <el-option
                              v-for="v in ttsDiscoveryResult.voices"
                              :key="v.id"
                              :label="v.name"
                              :value="v.id"
                            />
                          </el-select>
                          <el-input
                            v-else
                            v-model.trim="userAiConfigForm.ttsVoiceId"
                            maxlength="128"
                            placeholder="例如 alloy / zh_female_xiaoxiao"
                          />
                          <n-button
                            type="primary"
                            secondary
                            class="voice-preview-button tts-voice-preview-btn"
                            title="试听"
                            aria-label="试听当前 TTS 音色"
                            :loading="ttsPreviewing"
                            :disabled="!userAiConfigForm.ttsVoiceId?.trim()"
                            @click="handleTtsVoicePreview"
                          >
                            <FeatureIcon v-if="!ttsPreviewing" name="announcement" size="md" class="voice-preview-icon" />
                          </n-button>
                        </div>
                      </el-form-item>
                    </div>
                    <div v-if="ttsDiscoveryResult" class="cai-test-result" :class="{ failed: !ttsDiscoveryResult.success }">
                      <span class="cai-test-icon">{{ ttsDiscoveryResult.success ? '✓' : '✗' }}</span>
                      <span>{{
                        ttsDiscoveryResult.success
                          ? (ttsDiscoveryResult.voiceDiscoverySupported
                              ? `发现 ${ttsDiscoveryResult.models?.length || 0} 个模型、${ttsDiscoveryResult.voices?.length || 0} 个音色`
                              : `发现 ${ttsDiscoveryResult.models?.length || 0} 个模型，音色为预设列表`)
                          : ttsDiscoveryResult.message
                      }}</span>
                    </div>
                    <div v-if="userTtsConnectivityResult" class="cai-test-result" :class="{ failed: !userTtsConnectivityResult.success }">
                      <span class="cai-test-icon">{{ userTtsConnectivityResult.success ? '✓' : '✗' }}</span>
                      <span>{{ userTtsConnectivityResult.message }}</span>
                      <span v-if="userTtsConnectivityResult.errorType" class="cai-test-latency">{{ userTtsConnectivityResult.errorType }}</span>
                      <span v-else-if="userTtsConnectivityResult.latencyMs" class="cai-test-latency">{{ userTtsConnectivityResult.latencyMs }}ms</span>
                    </div>
                    <div class="cai-form-actions compact">
                      <n-button :loading="userTtsConnectivityTesting" secondary @click="handleUserTtsConnectivityTest">
                        测试 TTS
                      </n-button>
                    </div>
                  </div>
                </div>

                <div v-if="userAiConnectivityResult" class="cai-test-result" :class="{ failed: !userAiConnectivityResult.success }">
                  <span class="cai-test-icon">{{ userAiConnectivityResult.success ? '✓' : '✗' }}</span>
                  <span>{{ userAiConnectivityResult.message }}</span>
                  <span v-if="userAiConnectivityResult.errorType" class="cai-test-latency">{{ userAiConnectivityResult.errorType }}</span>
                  <span v-else-if="userAiConnectivityResult.latencyMs" class="cai-test-latency">{{ userAiConnectivityResult.latencyMs }}ms</span>
                </div>

                <div class="cai-form-actions">
                  <n-button :loading="userAiConnectivityTesting" secondary @click="handleUserAiConnectivityTest">
                    连通测试
                  </n-button>
                  <n-button :loading="userAiConfigSaving" type="primary" @click="handleUserAiConfigSave">
                    保存配置
                  </n-button>
                </div>
              </el-form>
            </div>

            <p class="cai-hint">优先级：简历配置 → 面试配置 → 通用兜底 → 平台 AI</p>
          </div>
        </section>

        <section v-else-if="activeSection === 'security'" key="security" class="settings-panel" aria-labelledby="security-title">
          <div class="panel-heading">
            <div>
              <h2 id="security-title">账号安全</h2>
              <p>修改登录密码、安全问题，并管理不可恢复的账号注销操作。</p>
            </div>
          </div>

          <div class="settings-panel-body">
          <div class="security-mode-tabs" role="tablist" aria-label="账号安全操作类型">
            <button
              type="button"
              class="security-mode-tab"
              :class="{ active: securityMode === 'password' }"
              role="tab"
              :aria-selected="securityMode === 'password'"
              @click="handleSecurityModeChange('password')"
            >
              修改密码
            </button>
            <button
              type="button"
              class="security-mode-tab"
              :class="{ active: securityMode === 'securityQuestion' }"
              role="tab"
              :aria-selected="securityMode === 'securityQuestion'"
              @click="handleSecurityModeChange('securityQuestion')"
            >
              修改安全问题
            </button>
            <button
              type="button"
              class="security-mode-tab danger"
              :class="{ active: securityMode === 'accountDeletion' }"
              role="tab"
              :aria-selected="securityMode === 'accountDeletion'"
              @click="handleSecurityModeChange('accountDeletion')"
            >
              注销账号
            </button>
          </div>

          <Transition name="security-panel" mode="out-in">
            <el-form
              v-if="securityMode === 'password'"
              key="password"
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-position="top"
              class="settings-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
              </el-form-item>
              <el-button type="primary" :loading="passwordSaving" @click="handlePasswordSave">
                保存密码
              </el-button>
            </el-form>

            <el-form
              v-else-if="securityMode === 'securityQuestion'"
              key="securityQuestion"
              ref="securityFormRef"
              :model="securityForm"
              :rules="securityRules"
              label-position="top"
              class="settings-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="securityForm.oldPassword" type="password" show-password autocomplete="current-password" />
              </el-form-item>
              <el-form-item label="安全问题" prop="securityQuestion">
                <el-select v-model="securityForm.securityQuestion" filterable allow-create default-first-option>
                  <el-option
                    v-for="item in securityQuestionOptions"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="安全答案" prop="securityAnswer">
                <el-input v-model="securityForm.securityAnswer" maxlength="100" show-word-limit />
              </el-form-item>
              <el-button type="primary" :loading="securitySaving" @click="handleSecuritySave">
                保存安全问题
              </el-button>
            </el-form>

            <div v-else key="accountDeletion" class="account-delete-zone">
              <div class="account-delete-context account-delete-alert" role="alert">
                <FeatureIcon name="account-security" size="md" class="settings-alert-icon" />
                <span>注销后不可恢复，系统将永久清理你的面试、简历、通知等所有数据。</span>
              </div>

              <el-form
                ref="accountDeleteFormRef"
                :model="accountDeleteForm"
                :rules="accountDeleteRules"
                label-position="top"
                class="settings-form account-delete-form"
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input v-model="accountDeleteForm.oldPassword" type="password" show-password autocomplete="current-password" />
                </el-form-item>
                <el-form-item label="再次输入当前密码" prop="confirmPassword">
                  <el-input v-model="accountDeleteForm.confirmPassword" type="password" show-password autocomplete="current-password" />
                </el-form-item>
                <el-form-item label="安全问题" prop="securityAnswer">
                  <div
                    class="security-question-card"
                    :class="{
                      loading: accountDeleteQuestionLoading,
                      expanded: accountDeleteQuestionExpanded,
                      error: Boolean(accountDeleteQuestionError)
                    }"
                  >
                    <div
                      id="account-delete-security-question"
                      class="security-question-text"
                    >
                      {{ accountDeleteQuestionText }}
                    </div>
                    <div class="security-question-actions">
                      <el-button
                        v-if="accountDeleteQuestionError"
                        link
                        type="primary"
                        @click="fetchAccountDeleteSecurityQuestion"
                      >
                        重新加载
                      </el-button>
                      <button
                        v-if="shouldShowAccountDeleteQuestionToggle"
                        type="button"
                        class="security-question-toggle"
                        :aria-expanded="accountDeleteQuestionExpanded"
                        aria-controls="account-delete-security-question"
                        @click="accountDeleteQuestionExpanded = !accountDeleteQuestionExpanded"
                      >
                        {{ accountDeleteQuestionExpanded ? '收起' : '展开' }}
                      </button>
                    </div>
                  </div>
                  <el-input
                    v-model="accountDeleteForm.securityAnswer"
                    maxlength="100"
                    show-word-limit
                    autocomplete="off"
                    placeholder="请输入安全问题答案"
                  />
                </el-form-item>
                <el-button
                  type="danger"
                  :disabled="accountDeleteQuestionLoading || Boolean(accountDeleteQuestionError)"
                  @click="handleAccountDeleteSubmit"
                >
                  确认注销
                </el-button>
              </el-form>
            </div>
          </Transition>

          <!-- 注销确认弹窗 -->
          <el-dialog
            v-model="accountDeleteConfirmDialogVisible"
            title="注销账号"
            width="440px"
            :close-on-click-modal="false"
            class="account-delete-dialog"
            destroy-on-close
            @open="onDialogOpen"
            @closed="onDialogClosed"
          >
            <div class="delete-dialog-body">
              <!-- 第一段红色警告框 -->
              <div class="delete-warning-box">
              <div class="delete-warning-icon">
                <FeatureIcon name="account-security" size="md" />
              </div>
                <div class="delete-warning-text">
                  <strong>此操作不可恢复！</strong>
                  <p>你的账号及所有关联数据（简历诊断记录、模拟面试历史、通知、成长数据等）将被<strong>永久删除</strong>，无法恢复。</p>
                </div>
              </div>
              <!-- 第二段红色确认框 -->
              <div class="delete-warning-box">
                <p class="delete-confirm-hint">为防止意外，确认继续操作请输入以下内容：</p>
                <code class="delete-dialog-code">{{ accountDeleteExpectedText }}</code>
                <el-input
                  v-model="accountDeleteConfirmText"
                  :placeholder="accountDeleteExpectedText"
                  class="delete-confirm-input"
                />
              </div>
            </div>
            <template #footer>
              <el-button @click="accountDeleteConfirmDialogVisible = false">取消</el-button>
              <el-button
                type="danger"
                :loading="accountDeleting"
                :disabled="accountDeleteConfirmText !== accountDeleteExpectedText || accountDeleteCountdown > 0"
                @click="handleDialogConfirm"
              >
                {{ dialogConfirmButtonText }}
              </el-button>
            </template>
          </el-dialog>
          </div>

        </section>

        <section v-else-if="activeSection === 'privacy'" key="privacy" class="settings-panel" aria-labelledby="privacy-title">
          <div class="panel-heading">
            <div>
              <h2 id="privacy-title">隐私与数据</h2>
              <p>查看账号数据概览，管理当前浏览器保存的本机设置缓存。</p>
            </div>
            <n-tooltip trigger="hover" placement="top">
              <template #trigger>
              <n-button
                secondary
                circle
                class="data-overview-refresh-btn"
                :class="{ 'is-refreshing': growthOverviewLoading }"
                :disabled="growthOverviewLoading"
                @click="fetchGrowthOverview"
              >
              <FeatureIcon name="retry" size="md" class="settings-refresh-icon" />
              </n-button>
              </template>
              刷新数据
            </n-tooltip>
          </div>

          <div class="settings-panel-body">
          <div class="info-grid data-overview-grid">
            <div class="info-item">
              <span>登录账号</span>
              <strong>{{ userInfo?.username || '--' }}</strong>
            </div>
            <div class="info-item">
              <span>当前身份</span>
              <strong>{{ roleText }}</strong>
            </div>
            <div class="info-item">
              <span>简历诊断次数</span>
              <strong>{{ growthSummary.resumeDiagnosisCount }}</strong>
            </div>
            <div class="info-item">
              <span>模拟面试次数</span>
              <strong>{{ growthSummary.mockInterviewCount }}</strong>
            </div>
            <div class="info-item">
              <span>JD 匹配次数</span>
              <strong>{{ growthSummary.jobMatchCount }}</strong>
            </div>
            <div class="info-item">
              <span>AI 润色次数</span>
              <strong>{{ growthSummary.polishCount }}</strong>
            </div>
          </div>

          <div v-if="growthOverviewError" class="inline-warning">
            {{ growthOverviewError }}
          </div>

          <div class="preference-list">
            <div class="preference-row">
              <div>
                <strong>清空本地缓存</strong>
                <span>仅清理设置偏好、主题偏好和通知筛选缓存；不会清理用户登录态或管理端登录态。</span>
              </div>
              <n-button type="warning" secondary @click="handleClearLocalCacheConfirm">
                清空本地缓存
              </n-button>
            </div>
            <div class="preference-row data-retention-row">
              <div>
                <strong>数据保留说明</strong>
                <span>账号数据由服务端按当前策略保留；面试记录和简历诊断记录可在数据管理中设置自动清理天数，手动清理仍需二次确认。</span>
              </div>
            </div>
          </div>
          </div>
        </section>

        <section v-else-if="activeSection === 'dataManagement'" key="dataManagement" class="settings-panel" aria-labelledby="data-management-title">
          <div class="panel-heading">
            <div>
              <h2 id="data-management-title">数据管理</h2>
              <p>管理历史记录手动清理与自动清理偏好；自动清理只在保存后按服务端低峰任务执行。</p>
            </div>
          </div>

          <div class="settings-panel-body">
          <div class="preference-list">
            <div class="preference-row danger-row">
              <div>
                <strong>面试记录清理</strong>
                <span>批量清理当前账号下的历史面试会话、聊天记录和岗位定向上下文。</span>
              </div>
              <n-button type="error" secondary :loading="interviewHistoryClearing" @click="handleInterviewHistoryClearConfirm">
                清理记录
              </n-button>
            </div>
            <div class="preference-row danger-row">
              <div>
                <strong>简历诊断清理</strong>
                <span>批量清理当前账号下的简历诊断、JD 匹配、AI 润色记录和上传文件。</span>
              </div>
              <n-button type="error" secondary :loading="resumeHistoryClearing" @click="handleResumeHistoryClearConfirm">
                清理记录
              </n-button>
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>面试记录保留天数</strong>
                <span>{{ retentionPreferenceText }}</span>
              </div>
              <n-select
                v-model:value="interviewPreferenceForm.interviewRetentionDays"
                class="preference-select"
                :options="retentionDayOptions"
              />
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>简历诊断保留天数</strong>
                <span>{{ resumeRetentionPreferenceText }}</span>
              </div>
              <n-select
                v-model:value="interviewPreferenceForm.resumeRetentionDays"
                class="preference-select"
                :loading="userSettingsSaving"
                :options="retentionDayOptions"
              />
            </div>
            <div class="preference-row data-management-save-row">
              <div>
                <strong>保存数据管理设置</strong>
                <span>保留天数只在点击保存后同步到服务端，避免修改面试偏好时触发后端写入。</span>
              </div>
              <n-button type="primary" :loading="userSettingsSaving" @click="handleDataManagementSettingsSave">
                保存设置
              </n-button>
            </div>
          </div>
          </div>
        </section>

        <section v-else-if="activeSection === 'feedback'" key="feedback" class="settings-panel" aria-labelledby="feedback-title">
          <div class="panel-heading">
            <div>
              <h2 id="feedback-title">问题反馈</h2>
              <p>提交使用过程中遇到的问题或功能建议，管理员会在后台集中跟进。</p>
            </div>
          </div>

          <div class="settings-panel-body">
          <el-form
            ref="feedbackFormRef"
            :model="feedbackForm"
            :rules="feedbackRules"
            label-position="top"
            class="settings-form feedback-form"
          >
            <el-form-item label="反馈类型" prop="type">
              <el-select v-model="feedbackForm.type" class="full-width">
                <el-option label="问题反馈" value="bug" />
                <el-option label="功能建议" value="suggestion" />
                <el-option label="体验问题" value="experience" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="标题" prop="title">
              <el-input v-model="feedbackForm.title" maxlength="100" show-word-limit placeholder="简要描述问题或建议" />
            </el-form-item>
            <el-form-item label="详细内容" prop="content">
              <el-input
                v-model="feedbackForm.content"
                type="textarea"
                :rows="7"
                maxlength="2000"
                show-word-limit
                placeholder="请描述出现问题的页面、操作步骤、期望结果或建议内容"
              />
            </el-form-item>
            <el-form-item label="联系方式（选填）" prop="contact">
              <el-input v-model="feedbackForm.contact" maxlength="100" show-word-limit placeholder="邮箱、手机号或其他便于联系的信息" />
            </el-form-item>
            <el-button type="primary" :loading="feedbackSubmitting" @click="handleFeedbackSubmit">
              提交反馈
            </el-button>
          </el-form>
          <div class="settings-fill-note">
            <strong>反馈会进入后台集中处理</strong>
            <span>请尽量描述页面、操作步骤和期望结果，便于定位问题。</span>
          </div>
          </div>
        </section>

        <section v-else-if="activeSection === 'appearance'" key="appearance" class="settings-panel" aria-labelledby="appearance-title">
          <div class="panel-heading">
            <div>
              <h2 id="appearance-title">外观偏好</h2>
              <p>选择当前浏览器使用的显示模式，偏好会自动保存在本机。</p>
            </div>
            <div class="appearance-status">
              <n-tag round :bordered="false">当前：{{ resolvedThemeText }}</n-tag>
              <span>已保存到当前浏览器</span>
            </div>
          </div>

          <div class="settings-panel-body">
          <div class="appearance-options" role="radiogroup" aria-label="外观模式">
            <button
              v-for="option in themeOptions"
              :key="option.value"
              type="button"
              class="appearance-option"
              :class="{ active: themeChoice === option.value }"
              role="radio"
              :aria-checked="themeChoice === option.value"
              @click="handleThemeChange(option.value)"
            >
              <span class="appearance-preview" :class="option.previewClass">
                <span></span>
                <span></span>
                <span></span>
              </span>
              <strong>{{ option.label }}</strong>
              <em>{{ option.description }}</em>
            </button>
          </div>
          <div class="settings-fill-note">
            <strong>主题会立即同步到用户端页面</strong>
            <span>当前设置保存在浏览器，本页控件会跟随亮色、暗色或系统主题切换。</span>
          </div>
          </div>
        </section>

        <section v-else-if="activeSection === 'notification'" key="notification" class="settings-panel" aria-labelledby="notification-title">
          <div class="panel-heading">
            <div>
              <h2 id="notification-title">通知偏好</h2>
              <p>仅影响当前浏览器的显示偏好。</p>
            </div>
          </div>

          <div class="settings-panel-body">
          <div class="preference-list">
            <div class="preference-row">
              <div>
                <strong>顶部实时通知提醒</strong>
                <span>关闭后不建立实时通知连接，也不显示顶部通知铃铛。</span>
              </div>
              <n-switch
                v-model:value="notificationForm.notificationRealtimeEnabled"
                aria-label="顶部实时通知提醒"
                @update:value="handleNotificationPreferenceSave"
              />
            </div>
            <div class="preference-row">
              <div>
                <strong>进入通知中心默认只看未读</strong>
                <span>打开通知中心时自动带入未读筛选。</span>
              </div>
              <n-switch
                v-model:value="notificationForm.notificationDefaultUnreadOnly"
                aria-label="进入通知中心默认只看未读"
                @update:value="handleNotificationPreferenceSave"
              />
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>通知中心默认类型</strong>
                <span>进入通知中心时自动选择对应类型。</span>
              </div>
              <n-select
                v-model:value="notificationForm.notificationDefaultType"
                class="notification-type-select"
                :options="notificationTypeOptions"
                @update:value="handleNotificationPreferenceSave"
              />
            </div>
          </div>
          <div class="settings-fill-note">
            <strong>通知显示只影响当前浏览器</strong>
            <span>关闭实时提醒不会删除通知记录，你仍然可以进入通知中心查看历史消息。</span>
          </div>
          </div>
        </section>

        <section v-else-if="activeSection === 'onboarding'" key="onboarding" class="settings-panel" aria-labelledby="onboarding-title">
          <div class="panel-heading">
            <div>
              <h2 id="onboarding-title">新手引导</h2>
              <p>需要重新熟悉功能入口时，可以再次查看引导。</p>
            </div>
          </div>

          <div class="settings-panel-body compact-action-panel">
            <div class="onboarding-intro-grid" aria-label="新手引导内容说明">
              <div class="onboarding-intro-card">
                <strong>简历诊断</strong>
                <span>从上传简历开始，查看评分、优化建议和历史诊断记录。</span>
              </div>
              <div class="onboarding-intro-card">
                <strong>模拟面试</strong>
                <span>熟悉岗位、难度、语音通话和面试报告的主要入口。</span>
              </div>
              <div class="onboarding-intro-card">
                <strong>模板与社区</strong>
                <span>了解模板库、社区分享和报告复盘可以放在哪里继续使用。</span>
              </div>
              <div class="onboarding-intro-card">
                <strong>会员与设置</strong>
                <span>确认额度、订阅状态、通知偏好和本机显示设置的位置。</span>
              </div>
            </div>
            <div class="settings-fill-note onboarding-action-note">
              <strong>重新熟悉功能入口</strong>
              <span>引导会按常用路径串起核心功能，不会修改你的简历、面试记录或当前偏好。</span>
            </div>
            <n-button type="primary" secondary @click="showOnboarding = true">
              重新查看新手引导
            </n-button>
          </div>
        </section>

        <section v-else-if="activeSection === 'membership'" key="membership" class="settings-panel" aria-labelledby="membership-title">
          <div class="panel-heading">
            <div>
              <h2 id="membership-title">会员与额度</h2>
              <p>查看当前身份、到期时间和可用额度。</p>
            </div>
            <n-button type="primary" secondary @click="router.push('/membership')">
              查看会员中心
            </n-button>
          </div>

          <div class="settings-panel-body">
          <div class="settings-fill-note membership-note">
            <strong>{{ membershipPlanText }}</strong>
            <span>额度信息来自当前账号资料，进入会员中心可查看套餐权益和续费入口。</span>
          </div>

          <div class="info-grid quota-grid">
            <div class="info-item">
              <span>当前身份</span>
              <strong>{{ roleText }}</strong>
            </div>
            <div class="info-item">
              <span>VIP 到期时间</span>
              <strong>{{ vipExpireTimeText }}</strong>
            </div>
            <div class="info-item">
              <span>简历诊断额度</span>
              <strong>{{ userInfo?.resumeQuota ?? 0 }}</strong>
            </div>
            <div class="info-item">
              <span>模拟面试额度</span>
              <strong>{{ userInfo?.interviewQuota ?? 0 }}</strong>
            </div>
            <div class="info-item">
              <span>VIP 今日简历额度</span>
              <strong>{{ userInfo?.vipDailyResumeQuota ?? 0 }}</strong>
            </div>
            <div class="info-item">
              <span>VIP 今日面试额度</span>
              <strong>{{ userInfo?.vipDailyInterviewQuota ?? 0 }}</strong>
            </div>
          </div>
          </div>
        </section>
        </Transition>
      </main>
    </div>

    <OnboardingGuide v-model:visible="showOnboarding" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { NButton, NSelect, NSlider, NSwitch, NTag, NTooltip } from 'naive-ui'
import { deleteAccount, getCurrentAccountSecurityQuestion, updatePassword, updateSecurityQuestion } from '@/api/auth'
import { createUserFeedback } from '@/api/feedback'
import { getGrowthOverview } from '@/api/growth'
import { clearInterviewHistory, getInterviewJobRoles } from '@/api/interview'
import { getMembershipPlans } from '@/api/membership'
import { clearResumeHistory } from '@/api/resume'
import {
  deleteUserAiConfig,
  discoverTtsModelsAndVoices,
  fetchUserAiModels,
  getUserAiConfigs,
  getSystemTtsStatus,
  getUserAiUsage,
  previewTtsVoice,
  saveUserAiConfig,
  testUserAiConnectivity,
  testUserTtsConnectivity,
  toggleUserAiConfig
} from '@/api/userAiConfig'
import { getUserSettings, saveUserSettings } from '@/api/userSettings'
import OnboardingGuide from '@/components/OnboardingGuide.vue'
import { useTextToSpeech } from '@/composables/useTextToSpeech'
import { FEEDBACK_MODE_OPTIONS, INTERACTION_MODE_OPTIONS, INTERVIEW_MODE_OPTIONS } from '@/constants/interview'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { removeToken } from '@/utils/auth'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import OptimizedImage from '@/components/common/OptimizedImage.vue'
import { optimizedImages } from '@/utils/optimizedImages'
import {
  BROWSER_TTS_VOICE_PRESET_GROUPS,
  clearLocalSettingsCache,
  DEFAULT_SETTINGS_PREFERENCES,
  EDGE_CLOUD_TTS_VOICES,
  getEdgeCloudTtsPreferenceValue,
  getEdgeCloudTtsVoiceId,
  getBrowserTtsPresetParameters,
  getSettingsPreferences,
  isEdgeCloudTtsVoicePreference,
  saveSettingsPreferences
} from '@/utils/settingsPreferences'

defineOptions({ name: 'SettingsView' })

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

const activeSection = ref('profile')
const showOnboarding = ref(false)
const securityMode = ref('password')
const interviewSubTab = ref('basic')
const passwordFormRef = ref(null)
const securityFormRef = ref(null)
const accountDeleteFormRef = ref(null)
const passwordSaving = ref(false)
const securitySaving = ref(false)
const accountDeleting = ref(false)
const interviewHistoryClearing = ref(false)
const resumeHistoryClearing = ref(false)
const userSettingsSaving = ref(false)
const feedbackFormRef = ref(null)
const feedbackSubmitting = ref(false)

const sections = [
  { key: 'profile', label: '账号资料', icon: 'user-profile' },
  { key: 'interview', label: '面试偏好', icon: 'ai-interviewer' },
  { key: 'customAi', label: '自定义 AI', icon: 'membership-center' },
  { key: 'security', label: '账号安全', icon: 'account-security' },
  { key: 'privacy', label: '隐私与数据', icon: 'data-cleanup' },
  { key: 'dataManagement', label: '数据管理', icon: 'data-management' },
  { key: 'feedback', label: '问题反馈', icon: 'feedback-center' },
  { key: 'appearance', label: '外观偏好', icon: 'settings' },
  { key: 'notification', label: '通知偏好', icon: 'notification-center' },
  { key: 'onboarding', label: '新手引导', icon: 'onboarding-task' },
  { key: 'membership', label: '会员与额度', icon: 'membership-credits' }
]

const interviewSubTabs = [
  { key: 'basic', label: '面试偏好' },
  { key: 'voice', label: '语音通话' }
]

const themeOptions = [
  { value: 'system', label: '跟随系统', description: '随设备系统自动切换', previewClass: 'system' },
  { value: 'light', label: '亮色', description: '适合白天和明亮环境', previewClass: 'light' },
  { value: 'dark', label: '暗色', description: '适合夜间和低亮环境', previewClass: 'dark' }
]

const userInfo = computed(() => userStore.userInfo)
const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '用户')
const isVipUser = computed(() => userStore.isVip())
const isAdmin = computed(() => userInfo.value?.role === 9)
const membershipPlans = ref([])
const interviewJobOptions = ref([])
const growthOverview = ref(null)
const growthOverviewLoading = ref(false)
const growthOverviewError = ref('')

const roleText = computed(() => {
  if (isAdmin.value) return '管理员'
  if (isVipUser.value) return '会员用户'
  return '普通用户'
})

const roleTagType = computed(() => {
  if (isAdmin.value) return 'warning'
  if (isVipUser.value) return 'success'
  return 'info'
})

const statusText = computed(() => {
  if (userInfo.value?.status === 0) return '已禁用'
  if (userInfo.value?.status === 1) return '正常'
  return '--'
})

const vipExpireTimeText = computed(() => {
  if (!userInfo.value?.vipExpireTime) return '--'
  const date = new Date(userInfo.value.vipExpireTime)
  if (Number.isNaN(date.getTime())) return '--'
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
})

const profileRegisterTimeText = computed(() => {
  // 账号资料区展示用户主表 create_time；后端未返回或历史数据异常时保持明确占位。
  const createTime = userInfo.value?.createTime
  if (!createTime) return '--'
  const date = new Date(createTime)
  if (Number.isNaN(date.getTime())) return '--'
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
})

const getPlanNameCn = (planName) => {
  const nameMap = {
    'Monthly VIP': '月度会员',
    'Quarterly VIP': '季度会员',
    'Yearly VIP': '年度会员'
  }
  return nameMap[planName] || planName
}

const membershipPlanText = computed(() => {
  if (!isVipUser.value) return '未开通会员'

  const currentPlanCode = userInfo.value?.membershipPlanCode || ''
  const matchedPlan = membershipPlans.value.find((plan) => plan.planCode === currentPlanCode)
  // 用户侧只展示套餐名称，避免暴露内部套餐编码。
  if (matchedPlan?.planName) return getPlanNameCn(matchedPlan.planName)
  return '会员套餐'
})

const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const securityForm = ref({ oldPassword: '', securityQuestion: '', securityAnswer: '' })
const accountDeleteForm = ref({ oldPassword: '', confirmPassword: '', securityAnswer: '' })
const feedbackForm = ref({ type: 'bug', title: '', content: '', contact: '' })
const accountDeleteSecurityQuestion = ref('')
const accountDeleteQuestionLoading = ref(false)
const accountDeleteQuestionError = ref('')
const accountDeleteQuestionExpanded = ref(false)
const accountDeleteCountdown = ref(0)
let accountDeleteTimer = null
const accountDeleteConfirmText = ref('')
const accountDeleteConfirmDialogVisible = ref(false)
const notificationForm = ref(getSettingsPreferences())
const interviewPreferenceForm = ref(getSettingsPreferences())
const previewTextToSpeech = useTextToSpeech()
const userAiConfigs = ref([])
const userAiConfigLoading = ref(false)
const userAiConfigSaving = ref(false)
const userAiConnectivityTesting = ref(false)
const userAiConnectivityResult = ref(null)
const userAiUsage = ref({ used: 0, limit: 50, remaining: 50 })
const systemTtsAvailable = ref(false)
const userAiToggleLoadingType = ref('')
/** TTS Provider 预设常量 */
const TTS_PROVIDER_PRESETS = [
  {
    value: 'openai',
    label: 'OpenAI',
    disabled: false,
    defaultBaseUrl: 'https://api.openai.com/v1',
    defaultModel: 'tts-1',
    defaultVoiceId: 'alloy',
    endpointPath: '/audio/speech',
    voices: [
      { id: 'alloy', name: 'Alloy' }, { id: 'echo', name: 'Echo' },
      { id: 'fable', name: 'Fable' }, { id: 'onyx', name: 'Onyx' },
      { id: 'nova', name: 'Nova' }, { id: 'shimmer', name: 'Shimmer' }
    ]
  },
  {
    value: 'mimo',
    label: 'MiMo（小米）',
    disabled: false,
    defaultBaseUrl: 'https://api.xiaomimimo.com/v1',
    defaultModel: 'mimo-v2.5-tts',
    defaultVoiceId: 'mimo_default',
    endpointPath: '/chat/completions',
    voices: [
      { id: 'mimo_default', name: 'MiMo-默认' }, { id: '冰糖', name: '冰糖' },
      { id: '茉莉', name: '茉莉' }, { id: '苏打', name: '苏打' },
      { id: '白桦', name: '白桦' }, { id: 'Mia', name: 'Mia' },
      { id: 'Chloe', name: 'Chloe' }, { id: 'Milo', name: 'Milo' },
      { id: 'Dean', name: 'Dean' }
    ]
  },
  {
    value: 'edge',
    label: 'EdgeTTS',
    disabled: false,
    defaultBaseUrl: 'https://speech.platform.bing.com',
    defaultModel: 'edge-tts',
    defaultVoiceId: 'zh-CN-XiaoxiaoNeural',
    endpointPath: '/consumer/speech/synthesize/readaloud/edge/v1',
    voices: EDGE_CLOUD_TTS_VOICES
  },
  {
    value: 'gemini',
    label: 'Gemini',
    disabled: false,
    defaultBaseUrl: 'https://generativelanguage.googleapis.com',
    defaultModel: 'gemini-2.5-flash-preview-tts',
    defaultVoiceId: 'Kore',
    endpointPath: '/v1beta/models/{model}:generateContent',
    voices: [
      { id: 'Kore', name: 'Kore' },
      { id: 'Puck', name: 'Puck' },
      { id: 'Charon', name: 'Charon' },
      { id: 'Fenrir', name: 'Fenrir' },
      { id: 'Aoede', name: 'Aoede' }
    ]
  },
  {
    value: 'minimax',
    label: 'MiniMax',
    disabled: false,
    defaultBaseUrl: 'https://api.minimax.chat',
    defaultModel: 'speech-02-turbo',
    defaultVoiceId: 'male-qn-qingse',
    endpointPath: '/v1/t2a_v2',
    voices: [
      { id: 'male-qn-qingse', name: '青涩男声' },
      { id: 'male-qn-jingying', name: '精英男声' },
      { id: 'female-shaonv', name: '少女女声' },
      { id: 'female-yujie', name: '御姐女声' },
      { id: 'presenter_male', name: '主持男声' },
      { id: 'presenter_female', name: '主持女声' }
    ]
  },
  {
    value: 'qwen',
    label: 'Qwen',
    disabled: false,
    defaultBaseUrl: 'https://dashscope.aliyuncs.com',
    defaultModel: 'qwen3-tts-flash',
    defaultVoiceId: 'Cherry',
    endpointPath: '/api/v1/services/aigc/multimodal-generation/generation',
    voices: [
      { id: 'Cherry', name: 'Cherry' },
      { id: 'Serena', name: 'Serena' },
      { id: 'Ethan', name: 'Ethan' },
      { id: 'Chelsie', name: 'Chelsie' }
    ]
  },
  {
    value: 'xai',
    label: 'xAI',
    disabled: false,
    defaultBaseUrl: 'https://api.x.ai',
    defaultModel: 'grok-tts',
    defaultVoiceId: 'Fritz-PlayAI',
    endpointPath: '/v1/tts',
    voices: [
      { id: 'Fritz-PlayAI', name: 'Fritz' },
      { id: 'Aiden-PlayAI', name: 'Aiden' },
      { id: 'Luna-PlayAI', name: 'Luna' }
    ]
  },
]

const userTtsConfigExpanded = ref(false)
const userAiConfigForm = ref({
  configType: 'default',
  providerName: '',
  baseUrl: '',
  apiKey: '',
  model: '',
  supportsMultimodal: false,
  ttsBaseUrl: '',
  ttsApiKey: '',
  ttsModel: '',
  ttsVoiceId: '',
  ttsEndpointPath: '',
  ttsProvider: ''
})
const userTtsConnectivityTesting = ref(false)
const userTtsConnectivityResult = ref(null)
const ttsPreviewing = ref(false)
let ttsPreviewAudio = null
let ttsPreviewObjectUrl = ''
const userAiModelFetching = ref(false)
const userAiModelOptions = ref([])
const ttsDiscovering = ref(false)
const ttsDiscoveryResult = ref(null)
const isUserEdgeTtsProvider = computed(() => userAiConfigForm.value.ttsProvider === 'edge')

/** 选择 TTS Provider 时自动填入预设默认值 */
const handleTtsProviderChange = (providerId, options = {}) => {
  const preset = TTS_PROVIDER_PRESETS.find(p => p.value === providerId)
  if (!preset || preset.disabled) return
  userAiConfigForm.value.ttsProvider = providerId
  userAiConfigForm.value.ttsBaseUrl = preset.defaultBaseUrl
  if (providerId === 'edge') {
    userAiConfigForm.value.ttsApiKey = ''
  }
  userAiConfigForm.value.ttsModel = preset.defaultModel
  userAiConfigForm.value.ttsVoiceId = options.voiceId || preset.defaultVoiceId
  userAiConfigForm.value.ttsEndpointPath = preset.endpointPath
  // 用预设音色填充发现结果（跳过网络请求）
  ttsDiscoveryResult.value = {
    success: true,
    models: preset.voices ? [{ id: preset.defaultModel, name: preset.defaultModel }] : [],
    voices: preset.voices || [],
    voiceDiscoverySupported: true,
    ttsEndpointPath: preset.endpointPath
  }
}

const themeChoice = ref(themeStore.followSystem ? 'system' : themeStore.manualTheme)
const resolvedThemeText = computed(() => themeStore.resolvedTheme === 'dark' ? '暗色' : '亮色')

const difficultyPreferenceOptions = [
  { label: '初级', value: 'primary' },
  { label: '中级', value: 'intermediate' },
  { label: '高级', value: 'advanced' }
]

const interviewModeOptions = INTERVIEW_MODE_OPTIONS
const feedbackModeOptions = FEEDBACK_MODE_OPTIONS
const interactionModeOptions = INTERACTION_MODE_OPTIONS
const buildVoiceKey = (voice) => `${voice.voiceURI || ''}|||${voice.name || ''}|||${voice.lang || ''}`
const isChromeBrowser = computed(() => {
  if (typeof window === 'undefined' || !window.navigator) return false
  const userAgent = window.navigator.userAgent || ''
  return /(?:Chrome|Chromium|CriOS)\//.test(userAgent) && !/(?:Edg|EdgiOS|EdgA|OPR|Opera|SamsungBrowser)\//.test(userAgent)
})
const browserTtsChineseVoiceCount = computed(() => {
  const voiceKeys = new Set()
  previewTextToSpeech.voices.value
    .filter((voice) => String(voice?.lang || '').toLowerCase().startsWith('zh'))
    .forEach((voice) => voiceKeys.add(buildVoiceKey(voice)))
  return voiceKeys.size
})
const isChromeBrowserVoiceLimited = computed(() => (
  isChromeBrowser.value && browserTtsChineseVoiceCount.value > 0 && browserTtsChineseVoiceCount.value <= 2
))
const isCloudTtsVoiceOption = (value) => isEdgeCloudTtsVoicePreference(value)
const isCloudTtsVoiceSelected = computed(() => isCloudTtsVoiceOption(interviewPreferenceForm.value.voicePreferredType))
const getVoicePresetOptionLabel = (option, available) => {
  if (isCloudTtsVoiceOption(option.value)) return option.label
  if (!available) {
    const unavailableReason = isChromeBrowser.value ? 'Chrome 未暴露该音色' : '当前系统不可用'
    return `${option.label}（${unavailableReason}）`
  }
  if (isChromeBrowserVoiceLimited.value && !['system', 'custom'].includes(option.value)) {
    // Chrome 的 speechSynthesis 常只暴露 1-2 个中文 voice；预设仍可改变语速/音调，但不应暗示存在更多真实音色。
    return `${option.label}（Chrome 共用 ${browserTtsChineseVoiceCount.value} 种 voice）`
  }
  return option.label
}
const voicePreferredTypeOptions = computed(() => BROWSER_TTS_VOICE_PRESET_GROUPS.map((group) => ({
  type: 'group',
  label: group.label,
  key: group.label,
  children: group.options.map((option) => {
    const available = isCloudTtsVoiceOption(option.value) || previewTextToSpeech.isPresetAvailable(option.value)
    return {
      ...option,
      label: getVoicePresetOptionLabel(option, available),
      disabled: !available
    }
  })
})))
const retentionDayOptions = [
  { label: '不自动清理', value: 0 },
  { label: '保留 30 天', value: 30 },
  { label: '保留 90 天', value: 90 },
  { label: '保留 180 天', value: 180 },
  { label: '保留 365 天', value: 365 }
]

const defaultInterviewJobSelectOptions = computed(() => [
  { label: '不设默认岗位', value: '' },
  ...interviewJobOptions.value.map((job) => ({
    label: job.label,
    value: job.value
  }))
])

const voiceMuteResumeOptions = [
  { label: '自动继续识别', value: 'auto' },
  { label: '再次点击麦克风后继续', value: 'manual' }
]

const voiceAutoSubmitDelayOptions = [
  { label: '不自动提交', value: 0 },
  { label: '等待 2 秒', value: 2000 },
  { label: '等待 3 秒', value: 3000 },
  { label: '等待 5 秒', value: 5000 }
]

const voiceRecognitionLanguageOptions = [
  { label: '自动匹配面试模式', value: 'auto' },
  { label: '中文普通话', value: 'zh-CN' },
  { label: '英文', value: 'en-US' }
]

const notificationTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '简历诊断', value: 'resume' },
  { label: 'AI 润色', value: 'polish' },
  { label: '模拟面试', value: 'interview' },
  { label: '额度提醒', value: 'quota' },
  { label: '系统通知', value: 'system' },
  { label: '活动公告', value: 'activity' },
  { label: '版本公告', value: 'update' },
  { label: '维护公告', value: 'maintenance' }
]

const userAiConfigTypeOptions = [
  { label: '通用兜底', value: 'default' },
  { label: '简历能力', value: 'resume' },
  { label: '面试对话', value: 'interview' }
]

const userAiConfigTypeLabelMap = {
  default: '通用兜底',
  resume: '简历能力',
  interview: '面试对话'
}

const userAiConfigTypeLabel = (configType) => userAiConfigTypeLabelMap[configType] || configType || '--'

const userAiVerificationText = (status) => {
  if (status === 'verified') return '已验证'
  if (status === 'failed') return '验证失败'
  return '待验证'
}

const userAiSlotMeta = [
  { type: 'resume', label: '简历能力', icon: 'resume-analysis' },
  { type: 'interview', label: '面试对话', icon: 'mock-interview' },
  { type: 'default', label: '通用兜底', icon: 'settings' }
]

const userAiConfigSlots = computed(() =>
  userAiSlotMeta.map((slot) => ({
    ...slot,
    config: userAiConfigs.value.find((c) => c.configType === slot.type) || null
  }))
)

const hasAnyTtsFormValue = computed(() =>
  Boolean(
    String(userAiConfigForm.value.ttsBaseUrl || '').trim()
    || String(userAiConfigForm.value.ttsApiKey || '').trim()
    || String(userAiConfigForm.value.ttsModel || '').trim()
    || String(userAiConfigForm.value.ttsVoiceId || '').trim()
  )
)

const currentUserTtsConfigured = computed(() => {
  if (!['default', 'interview'].includes(userAiConfigForm.value.configType)) return false
  const selectedConfig = userAiConfigs.value.find((item) => item.configType === userAiConfigForm.value.configType)
  return Boolean(selectedConfig?.ttsConfigured || hasAnyTtsFormValue.value)
})

const userTtsRuntimeStatusText = computed(() => {
  if (!['default', 'interview'].includes(userAiConfigForm.value.configType)) return ''
  if (currentUserTtsConfigured.value) {
    return '当前使用自定义语音服务（优先于系统配置）'
  }
  if (systemTtsAvailable.value) {
    return '当前使用系统提供的云端语音服务'
  }
  return ''
})

const isTtsSupportedAiConfig = computed(() => ['default', 'interview'].includes(userAiConfigForm.value.configType))

const currentSlotIconName = computed(() => {
  const match = userAiSlotMeta.find((s) => s.type === userAiConfigForm.value.configType)
  return match?.icon || 'settings'
})

const growthSummary = computed(() => {
  const summary = growthOverview.value?.summary || {}
  return {
    resumeDiagnosisCount: Number(summary.resumeDiagnosisCount ?? 0),
    mockInterviewCount: Number(summary.mockInterviewCount ?? 0),
    jobMatchCount: Number(summary.jobMatchCount ?? 0),
    polishCount: Number(summary.polishCount ?? 0)
  }
})

const retentionPreferenceText = computed(() => {
  const days = Number(interviewPreferenceForm.value.interviewRetentionDays || 0)
  if (!days) {
    return '当前设置为不自动清理；保存后服务端不会按天数删除面试记录。'
  }
  return `服务端将每日低峰自动清理 ${days} 天前的已结束面试记录。`
})

const resumeRetentionPreferenceText = computed(() => {
  const days = Number(interviewPreferenceForm.value.resumeRetentionDays || 0)
  if (!days) {
    return '当前设置为不自动清理；保存后服务端不会按天数删除简历诊断记录。'
  }
  return `服务端将每日低峰自动清理 ${days} 天前已完成或失败的简历诊断记录。`
})

const formatSpeechRate = (value) => `${Number(value).toFixed(2)}x`
const formatSpeechPitch = (value) => Number(value).toFixed(2)
const formatSpeechVolume = (value) => `${Math.round(Number(value) * 100)}%`

const browserVoiceOptions = computed(() => previewTextToSpeech.voices.value.map((voice) => ({
  label: `${voice.name || 'Unknown'}${voice.lang ? ` (${voice.lang})` : ''}`,
  value: buildVoiceKey(voice),
  voice
})))

const selectedBrowserVoiceKey = computed({
  get() {
    if (!interviewPreferenceForm.value.voiceName && !interviewPreferenceForm.value.voiceURI) return ''
    return [
      interviewPreferenceForm.value.voiceURI || '',
      interviewPreferenceForm.value.voiceName || '',
      interviewPreferenceForm.value.voiceLang || ''
    ].join('|||')
  },
  set(value) {
    handleBrowserVoiceChange(value)
  }
})

const buildVoicePreferenceFromForm = () => ({
  type: interviewPreferenceForm.value.voicePreferredType,
  name: interviewPreferenceForm.value.voiceName,
  voiceURI: interviewPreferenceForm.value.voiceURI,
  lang: interviewPreferenceForm.value.voiceLang
})
previewTextToSpeech.setVoicePreference(buildVoicePreferenceFromForm())

const applyVoicePresetParameters = () => {
  const parameters = getBrowserTtsPresetParameters(interviewPreferenceForm.value.voicePreferredType)
  if (!parameters) return
  // 预设音色自带语速和音调，选择预设时同步滑块，保证设置页回显与真实面试播报一致。
  interviewPreferenceForm.value.voiceSpeakingRate = parameters.rate
  interviewPreferenceForm.value.voicePitch = parameters.pitch
}

const getResolvedBrowserTtsStyle = () => {
  const parameters = getBrowserTtsPresetParameters(interviewPreferenceForm.value.voicePreferredType)
  return {
    rate: Number(parameters?.rate ?? interviewPreferenceForm.value.voiceSpeakingRate),
    pitch: Number(parameters?.pitch ?? interviewPreferenceForm.value.voicePitch),
    volume: Number(interviewPreferenceForm.value.voiceVolume)
  }
}

const browserTtsVoiceStatusText = computed(() => {
  if (isCloudTtsVoiceSelected.value) {
    return '当前使用 EdgeTTS 云端音色；Chrome 只负责播放后端返回的音频，不依赖浏览器本地 voice。'
  }
  const status = previewTextToSpeech.voicePreferenceStatus.value
  const selectedVoiceName = status.selectedVoiceName || '浏览器默认中文 voice'
  if (!previewTextToSpeech.isSupported.value) return '当前浏览器不支持系统 TTS。'
  if (status.requestedType === 'system') return '当前使用浏览器系统默认 voice。'
  if (isChromeBrowserVoiceLimited.value) {
    const selectedText = status.usesBrowserDefaultVoice
      ? '当前实际音色由浏览器默认中文 voice 决定'
      : `当前实际音色：${selectedVoiceName}`
    return `Chrome 当前只暴露 ${browserTtsChineseVoiceCount.value} 种中文浏览器 voice，多个预设会共用同一音色；Edge 或安装更多系统语音后可用更多音色。${selectedText}。`
  }
  if (status.usesBrowserDefaultVoice) {
    return '当前使用浏览器默认中文 voice；如果 Chrome 没有暴露可区分的中文男声/女声，听感可能仍由系统默认音色决定。'
  }
  if (status.isDegraded && status.requestedType === 'male') {
    return `当前浏览器没有暴露中文男声，实际音色：${selectedVoiceName}。`
  }
  if (status.isDegraded && status.requestedType === 'female') {
    return `当前浏览器没有暴露中文女声，实际音色：${selectedVoiceName}。`
  }
  return `实际音色：${selectedVoiceName}。`
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const validateAccountDeleteConfirmPassword = (rule, value, callback) => {
  if (value !== accountDeleteForm.value.oldPassword) {
    callback(new Error('两次输入的当前密码不一致'))
    return
  }
  callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度应为 6-100 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const securityRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  securityQuestion: [{ required: true, message: '请选择或输入安全问题', trigger: 'change' }],
  securityAnswer: [
    { required: true, message: '请输入安全答案', trigger: 'blur' },
    { max: 100, message: '安全答案长度不能超过 100 个字符', trigger: 'blur' }
  ]
}

const accountDeleteRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入当前密码', trigger: 'blur' },
    { validator: validateAccountDeleteConfirmPassword, trigger: 'blur' }
  ],
  securityAnswer: [
    { required: true, message: '请输入安全问题答案', trigger: 'blur' },
    { max: 100, message: '安全答案长度不能超过 100 个字符', trigger: 'blur' }
  ]
}

const feedbackRules = {
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  title: [
    { required: true, message: '请输入反馈标题', trigger: 'blur' },
    { min: 2, max: 100, message: '反馈标题长度应为 2-100 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入反馈内容', trigger: 'blur' },
    { min: 10, max: 2000, message: '反馈内容长度应为 10-2000 个字符', trigger: 'blur' }
  ],
  contact: [{ max: 100, message: '联系方式不能超过 100 个字符', trigger: 'blur' }]
}

const accountDeleteQuestionText = computed(() => {
  if (accountDeleteQuestionLoading.value) return '正在加载安全问题...'
  if (accountDeleteQuestionError.value) return accountDeleteQuestionError.value
  return accountDeleteSecurityQuestion.value || '当前账号未加载到安全问题'
})

const shouldShowAccountDeleteQuestionToggle = computed(() => {
  return !accountDeleteQuestionLoading.value &&
    !accountDeleteQuestionError.value &&
    accountDeleteQuestionText.value.length > 36
})

const securityQuestionOptions = [
  '你的第一只宠物叫什么名字？',
  '你的出生城市是哪里？',
  '你小学班主任叫什么名字？',
  '你最喜欢的电影是什么？',
  '你母亲的名字是什么？',
  '你的第一辆车是什么品牌？',
  '你高中学校的名称是什么？',
  '你最好的朋友叫什么名字？'
]

const syncCloudTtsVoicePreference = () => {
  if (!isCloudTtsVoiceSelected.value) return
  handleTtsProviderChange('edge', {
    voiceId: getEdgeCloudTtsVoiceId(interviewPreferenceForm.value.voicePreferredType)
  })
  userTtsConfigExpanded.value = true
}

const syncPreferenceForms = (preferences) => {
  const nextPreferences = { ...preferences }
  notificationForm.value = nextPreferences
  interviewPreferenceForm.value = { ...nextPreferences }
  previewTextToSpeech.setVoicePreference(buildVoicePreferenceFromForm())
  // 回显已保存的 EdgeTTS 云端音色时，同步保持 TTS 表单指向 EdgeTTS，避免下拉与试听链路不一致。
  syncCloudTtsVoicePreference()
}

const buildServerSettingsPayload = () => ({
  interviewRetentionDays: Number(interviewPreferenceForm.value.interviewRetentionDays || 0),
  resumeRetentionDays: Number(interviewPreferenceForm.value.resumeRetentionDays || 0)
})

const applyServerSettingsToLocalPreferences = (serverSettings) => {
  const merged = saveSettingsPreferences({
    ...getSettingsPreferences(),
    interviewRetentionDays: Number(serverSettings?.interviewRetentionDays ?? 0),
    resumeRetentionDays: Number(serverSettings?.resumeRetentionDays ?? 0)
  })
  syncPreferenceForms(merged)
}

const fetchUserSettings = async () => {
  try {
    const res = await getUserSettings()
    applyServerSettingsToLocalPreferences(res?.data || {})
  } catch {
    ElMessage.warning('服务端设置暂时无法加载，当前页面保留本机偏好展示')
  }
}

const fetchInterviewJobOptions = async () => {
  try {
    const res = await getInterviewJobRoles()
    const rawList = Array.isArray(res?.data) ? res.data : []
    interviewJobOptions.value = rawList.map((item) => ({
      label: item.roleName,
      value: item.roleCode || item.roleName,
      roleCode: item.roleCode || '',
      roleName: item.roleName
    }))
  } catch {
    interviewJobOptions.value = []
  }
}

const fetchGrowthOverview = async () => {
  growthOverviewLoading.value = true
  growthOverviewError.value = ''
  try {
    const res = await getGrowthOverview()
    growthOverview.value = res?.data || null
  } catch (err) {
    growthOverview.value = null
    growthOverviewError.value = err?.message || '账号数据概览暂时无法加载，请稍后重试。'
  } finally {
    growthOverviewLoading.value = false
  }
}

const buildUserAiConfigPayload = () => {
  const payload = {
    configType: String(userAiConfigForm.value.configType || 'default').trim(),
    providerName: String(userAiConfigForm.value.providerName || '').trim(),
    baseUrl: String(userAiConfigForm.value.baseUrl || '').trim(),
    apiKey: String(userAiConfigForm.value.apiKey || '').trim(),
    model: String(userAiConfigForm.value.model || '').trim(),
    supportsMultimodal: Boolean(userAiConfigForm.value.supportsMultimodal)
  }
  if (['default', 'interview'].includes(payload.configType)) {
    payload.ttsBaseUrl = String(userAiConfigForm.value.ttsBaseUrl || '').trim()
    payload.ttsApiKey = String(userAiConfigForm.value.ttsApiKey || '').trim()
    payload.ttsModel = String(userAiConfigForm.value.ttsModel || '').trim()
    payload.ttsVoiceId = String(userAiConfigForm.value.ttsVoiceId || '').trim()
    payload.ttsEndpointPath = String(userAiConfigForm.value.ttsEndpointPath || '').trim()
    payload.ttsProvider = String(userAiConfigForm.value.ttsProvider || '').trim()
  }
  return payload
}

const validateUserAiConfigForm = () => {
  const payload = buildUserAiConfigPayload()
  // 自定义 AI 配置直接影响出网调用，保存前在前端先做非空和白名单校验，后端仍负责最终安全校验。
  if (!['default', 'resume', 'interview'].includes(payload.configType)) {
    ElMessage.warning('配置类型不正确')
    return null
  }
  if (!payload.baseUrl || !payload.apiKey || !payload.model) {
    ElMessage.warning('请填写 API 地址、API Key 和模型')
    return null
  }
  const ttsKeyRequired = payload.ttsProvider !== 'edge'
  const ttsValues = ttsKeyRequired
    ? [payload.ttsBaseUrl, payload.ttsApiKey, payload.ttsModel, payload.ttsVoiceId]
    : [payload.ttsBaseUrl, payload.ttsModel, payload.ttsVoiceId]
  if (['default', 'interview'].includes(payload.configType) && ttsValues.some(Boolean) && !ttsValues.every(Boolean)) {
    ElMessage.warning(ttsKeyRequired ? '启用 TTS 时请完整填写 TTS 地址、Key、模型和音色' : '启用 EdgeTTS 时请完整填写 TTS 地址、模型和音色')
    return null
  }
  return payload
}

const buildUserTtsConnectivityPayload = () => ({
  ttsBaseUrl: String(userAiConfigForm.value.ttsBaseUrl || '').trim(),
  ttsApiKey: String(userAiConfigForm.value.ttsApiKey || '').trim(),
  ttsModel: String(userAiConfigForm.value.ttsModel || '').trim(),
  ttsVoiceId: String(userAiConfigForm.value.ttsVoiceId || '').trim(),
  ttsEndpointPath: String(userAiConfigForm.value.ttsEndpointPath || '').trim(),
  ttsProvider: String(userAiConfigForm.value.ttsProvider || '').trim()
})

const validateUserTtsConfigForm = () => {
  if (!isTtsSupportedAiConfig.value) {
    ElMessage.warning('TTS 仅用于通用兜底或面试对话配置')
    return null
  }
  const payload = buildUserTtsConnectivityPayload()
  const ttsKeyRequired = payload.ttsProvider !== 'edge'
  if (!payload.ttsBaseUrl || (ttsKeyRequired && !payload.ttsApiKey) || !payload.ttsModel || !payload.ttsVoiceId) {
    ElMessage.warning(ttsKeyRequired ? '请完整填写 TTS 地址、API Key、模型和音色' : '请完整填写 EdgeTTS 地址、模型和音色')
    return null
  }
  return payload
}

const shouldPersistEdgeCloudTtsPreference = (payload) => (
  ['default', 'interview'].includes(payload.configType) &&
  payload.ttsProvider === 'edge' &&
  Boolean(payload.ttsBaseUrl && payload.ttsModel && payload.ttsVoiceId)
)

const handleUserAiModelsFetch = async () => {
  const baseUrl = String(userAiConfigForm.value.baseUrl || '').trim()
  const apiKey = String(userAiConfigForm.value.apiKey || '').trim()
  if (!baseUrl || !apiKey) {
    ElMessage.warning('请先填写 API 地址和 API Key')
    return
  }

  userAiModelFetching.value = true
  try {
    const res = await fetchUserAiModels({ baseUrl, apiKey })
    const data = res?.data || {}
    if (!data.success) {
      throw new Error(data.errorMessage || data.message || '模型列表获取失败')
    }
    // 模型发现只回填候选项；失败或为空都不覆盖用户手动填写的模型名。
    userAiModelOptions.value = Array.isArray(data.models) ? data.models : []
    if (!userAiConfigForm.value.model && userAiModelOptions.value[0]?.id) {
      userAiConfigForm.value.model = userAiModelOptions.value[0].id
    }
    ElMessage.success(data.message || '模型列表获取成功')
  } catch (err) {
    ElMessage.error(err?.message || '模型列表获取失败，请手动输入模型名')
  } finally {
    userAiModelFetching.value = false
  }
}

const fetchUserAiConfigState = async () => {
  userAiConfigLoading.value = true
  try {
    const [configRes, usageRes, systemTtsRes] = await Promise.all([
      getUserAiConfigs(),
      getUserAiUsage(),
      getSystemTtsStatus()
    ])
    userAiConfigs.value = Array.isArray(configRes?.data) ? configRes.data : []
    userAiUsage.value = usageRes?.data || { used: 0, limit: 50, remaining: 50 }
    systemTtsAvailable.value = Boolean(systemTtsRes?.data?.systemTtsAvailable)
  } catch (err) {
    ElMessage.warning(err?.message || '用户自定义 AI 配置暂时无法加载')
  } finally {
    userAiConfigLoading.value = false
  }
}

const fillUserAiConfigForm = (item) => {
  userAiConfigForm.value = {
    configType: item?.configType || 'default',
    providerName: item?.providerName || '',
    baseUrl: item?.baseUrl || '',
    // 后端只返回脱敏 Key，编辑时必须重新输入真实 Key，避免把脱敏值误提交。
    apiKey: '',
    model: item?.model || '',
    supportsMultimodal: Boolean(item?.supportsMultimodal),
    ttsBaseUrl: item?.ttsBaseUrl || '',
    // 后端只返回脱敏 TTS Key，编辑或测试时必须重新输入真实 Key。
    ttsApiKey: '',
    ttsModel: item?.ttsModel || '',
    ttsVoiceId: item?.ttsVoiceId || '',
    ttsEndpointPath: item?.ttsEndpointPath || '',
    ttsProvider: item?.ttsProvider || ''
  }
  userAiConnectivityResult.value = null
  userTtsConnectivityResult.value = null
  userAiModelOptions.value = []
  ttsDiscoveryResult.value = null
  userTtsConfigExpanded.value = false
  syncCloudTtsVoicePreference()
}

const handleUserAiConfigTypeChange = (configType) => {
  const matched = userAiConfigs.value.find((item) => item.configType === configType)
  if (matched) {
    fillUserAiConfigForm(matched)
    return
  }
  userAiConfigForm.value = {
    configType,
    providerName: '',
    baseUrl: '',
    apiKey: '',
    model: '',
    supportsMultimodal: false,
    ttsBaseUrl: '',
    ttsApiKey: '',
    ttsModel: '',
    ttsVoiceId: '',
    ttsEndpointPath: '',
    ttsProvider: ''
  }
  userAiConnectivityResult.value = null
  userTtsConnectivityResult.value = null
  userAiModelOptions.value = []
  ttsDiscoveryResult.value = null
  userTtsConfigExpanded.value = false
  syncCloudTtsVoicePreference()
}

const handleUserAiConnectivityTest = async () => {
  const payload = validateUserAiConfigForm()
  if (!payload) return
  userAiConnectivityTesting.value = true
  userAiConnectivityResult.value = null
  try {
    const res = await testUserAiConnectivity({
      baseUrl: payload.baseUrl,
      apiKey: payload.apiKey,
      model: payload.model,
      supportsMultimodal: payload.supportsMultimodal
    })
    userAiConnectivityResult.value = res?.data || { success: true, message: '连通测试成功' }
    ElMessage.success(userAiConnectivityResult.value.message || '连通测试成功')
  } catch (err) {
    userAiConnectivityResult.value = {
      success: false,
      message: err?.message || '连通测试失败'
    }
    ElMessage.error(userAiConnectivityResult.value.message)
  } finally {
    userAiConnectivityTesting.value = false
  }
}

const handleUserTtsConnectivityTest = async () => {
  const payload = validateUserTtsConfigForm()
  if (!payload) return
  userTtsConnectivityTesting.value = true
  userTtsConnectivityResult.value = null
  try {
    const res = await testUserTtsConnectivity(payload)
    userTtsConnectivityResult.value = res?.data || { success: true, message: 'TTS 连通测试成功' }
    ElMessage.success(userTtsConnectivityResult.value.message || 'TTS 连通测试成功')
  } catch (err) {
    userTtsConnectivityResult.value = {
      success: false,
      message: err?.message || 'TTS 连通测试失败'
    }
    ElMessage.error(userTtsConnectivityResult.value.message)
  } finally {
    userTtsConnectivityTesting.value = false
  }
}

/** 释放试听音频资源，防止 ObjectURL 泄漏。 */
const releaseTtsPreviewAudio = () => {
  if (ttsPreviewAudio) {
    ttsPreviewAudio.pause()
    ttsPreviewAudio.onended = null
    ttsPreviewAudio.onerror = null
    ttsPreviewAudio = null
  }
  if (ttsPreviewObjectUrl) {
    URL.revokeObjectURL(ttsPreviewObjectUrl)
    ttsPreviewObjectUrl = ''
  }
}

/** 试听当前 TTS 音色：合成最短音频后立即播放。 */
const handleTtsVoicePreview = async () => {
  const payload = validateUserTtsConfigForm()
  if (!payload) return
  releaseTtsPreviewAudio()
  ttsPreviewing.value = true
  try {
    const blob = await previewTtsVoice(payload)
    ttsPreviewObjectUrl = URL.createObjectURL(blob)
    ttsPreviewAudio = new Audio(ttsPreviewObjectUrl)
    ttsPreviewAudio.onended = () => releaseTtsPreviewAudio()
    await ttsPreviewAudio.play()
  } catch (err) {
    ElMessage.error(err?.message || 'TTS 试听失败，请检查配置后重试')
  } finally {
    ttsPreviewing.value = false
  }
}

const handleTtsDiscovery = async () => {
  if (!userAiConfigForm.value.ttsBaseUrl?.trim() || (!isUserEdgeTtsProvider.value && !userAiConfigForm.value.ttsApiKey?.trim())) {
    ElMessage.warning(isUserEdgeTtsProvider.value ? '请先填写 TTS 地址' : '请先填写 TTS 地址和 API Key')
    return
  }
  ttsDiscovering.value = true
  ttsDiscoveryResult.value = null
  try {
    const res = await discoverTtsModelsAndVoices({
      ttsBaseUrl: userAiConfigForm.value.ttsBaseUrl,
      ttsApiKey: userAiConfigForm.value.ttsApiKey,
      ttsProvider: userAiConfigForm.value.ttsProvider
    })
    const data = res?.data
    ttsDiscoveryResult.value = data
    if (data?.success) {
      // 将探测到的端点路径存入表单，随配置保存
      if (data.ttsEndpointPath) {
        userAiConfigForm.value.ttsEndpointPath = data.ttsEndpointPath
      }
      ElMessage.success(data.message || '获取成功')
    } else {
      ElMessage.warning(data?.message || '获取失败')
    }
  } catch (err) {
    ttsDiscoveryResult.value = {
      success: false, models: [], voices: [], voiceDiscoverySupported: false,
      message: err?.message || 'TTS 模型/音色获取失败'
    }
    ElMessage.error(ttsDiscoveryResult.value.message)
  } finally {
    ttsDiscovering.value = false
  }
}

const handleUserAiConfigSave = async () => {
  const payload = validateUserAiConfigForm()
  if (!payload) return
  userAiConfigSaving.value = true
  try {
    await saveUserAiConfig(payload)
    if (shouldPersistEdgeCloudTtsPreference(payload)) {
      syncPreferenceForms(saveSettingsPreferences({
        ...interviewPreferenceForm.value,
        voicePreferredType: getEdgeCloudTtsPreferenceValue(payload.ttsVoiceId),
        voiceName: '',
        voiceURI: '',
        voiceLang: ''
      }))
    }
    ElMessage.success('自定义 AI 配置已保存')
    userAiConfigForm.value.apiKey = ''
    await fetchUserAiConfigState()
  } catch (err) {
    ElMessage.error(err?.message || '自定义 AI 配置保存失败')
  } finally {
    userAiConfigSaving.value = false
  }
}

const handleUserAiConfigToggle = async (configType, enabled) => {
  userAiToggleLoadingType.value = configType
  try {
    await toggleUserAiConfig(configType, Boolean(enabled))
    ElMessage.success(enabled ? '自定义 AI 配置已启用' : '自定义 AI 配置已禁用')
    await fetchUserAiConfigState()
  } finally {
    userAiToggleLoadingType.value = ''
  }
}

const handleUserAiConfigDelete = async (configType) => {
  try {
    await ElMessageBox.confirm(
      `将删除「${userAiConfigTypeLabel(configType)}」自定义 AI 配置，删除后该场景会回到通用配置或平台 AI。`,
      '删除自定义 AI 配置',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteUserAiConfig(configType)
    ElMessage.success('自定义 AI 配置已删除')
    await fetchUserAiConfigState()
  } catch {
    // 用户取消或接口失败时由全局提示处理，当前表单状态保留。
  }
}

const resetPasswordForm = () => {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordFormRef.value?.resetFields()
}

const resetSecurityForm = () => {
  securityForm.value = { oldPassword: '', securityQuestion: '', securityAnswer: '' }
  securityFormRef.value?.resetFields()
}

const handleSecurityModeChange = (value) => {
  if (securityMode.value === value) return
  securityMode.value = value
  // 切换安全操作时清空未展示表单，防止两个高风险表单的输入状态互相干扰。
  if (value === 'password') {
    resetSecurityForm()
    resetAccountDeleteForm()
    clearAccountDeleteTimer()
    return
  }
  if (value === 'securityQuestion') {
    resetPasswordForm()
    resetAccountDeleteForm()
    clearAccountDeleteTimer()
    return
  }
  resetPasswordForm()
  resetSecurityForm()
  resetAccountDeleteForm()
  // 立即获取安全问题（表单需要展示），但不开始倒计时
  fetchAccountDeleteSecurityQuestion()
}

const handlePasswordSave = async () => {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }

  passwordSaving.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    removeToken()
    userStore.clearUserInfo()
    router.push('/login')
  } finally {
    passwordSaving.value = false
  }
}

const handleSecuritySave = async () => {
  if (!securityFormRef.value) return
  try {
    await securityFormRef.value.validate()
  } catch {
    return
  }

  securitySaving.value = true
  try {
    await updateSecurityQuestion({
      oldPassword: securityForm.value.oldPassword,
      securityQuestion: securityForm.value.securityQuestion,
      securityAnswer: securityForm.value.securityAnswer
    })
    securityForm.value = { oldPassword: '', securityQuestion: '', securityAnswer: '' }
    securityFormRef.value?.resetFields()
    ElMessage.success('安全问题已保存')
  } finally {
    securitySaving.value = false
  }
}

const resetAccountDeleteForm = () => {
  accountDeleteForm.value = { oldPassword: '', confirmPassword: '', securityAnswer: '' }
  accountDeleteQuestionExpanded.value = false
  accountDeleteConfirmText.value = ''
  accountDeleteFormRef.value?.resetFields()
}

// 需要输入的确认文字：用户名 + 确认注销
const accountDeleteExpectedText = computed(() => `${userInfo.value?.username || ''}确认注销`)

// 弹窗打开时开始倒计时
const onDialogOpen = () => {
  startAccountDeleteCountdown()
}

// 弹窗关闭时重置倒计时和输入
const onDialogClosed = () => {
  accountDeleteConfirmText.value = ''
  clearAccountDeleteTimer()
}

// 弹窗确认按钮文案
const dialogConfirmButtonText = computed(() => {
  if (accountDeleting.value) return '正在注销'
  if (accountDeleteCountdown.value > 0) return `等待 ${accountDeleteCountdown.value} 秒`
  return '确认注销'
})

const clearAccountDeleteTimer = () => {
  if (accountDeleteTimer) {
    clearInterval(accountDeleteTimer)
    accountDeleteTimer = null
  }
  accountDeleteCountdown.value = 0
}

const startAccountDeleteCountdown = () => {
  clearAccountDeleteTimer()
  accountDeleteCountdown.value = 15
  accountDeleteTimer = setInterval(() => {
    accountDeleteCountdown.value = Math.max(0, accountDeleteCountdown.value - 1)
    if (accountDeleteCountdown.value === 0) {
      clearAccountDeleteTimer()
    }
  }, 1000)
}

const fetchAccountDeleteSecurityQuestion = async () => {
  accountDeleteQuestionLoading.value = true
  accountDeleteQuestionError.value = ''
  accountDeleteQuestionExpanded.value = false
  try {
    const res = await getCurrentAccountSecurityQuestion()
    accountDeleteSecurityQuestion.value = res?.data?.securityQuestion || ''
    if (!accountDeleteSecurityQuestion.value) {
      accountDeleteQuestionError.value = '当前账号未设置安全问题，暂不能注销账号'
    }
  } catch (err) {
    accountDeleteSecurityQuestion.value = ''
    accountDeleteQuestionError.value = err?.message || '安全问题加载失败，暂不能注销账号'
  } finally {
    accountDeleteQuestionLoading.value = false
  }
}

const handleAccountDelete = async (payload) => {
  accountDeleting.value = true
  try {
    await deleteAccount(payload)
    ElMessage.success('账号已注销')
    removeToken()
    userStore.clearUserInfo()
    router.push('/login')
  } finally {
    accountDeleting.value = false
  }
}

const handleAccountDeleteSubmit = async () => {
  if (!accountDeleteFormRef.value) return
  try {
    await accountDeleteFormRef.value.validate()
  } catch {
    return
  }
  // 验证通过后仍进入强确认弹窗，要求用户输入指定文本，避免一次误点直接注销账号。
  accountDeleteConfirmText.value = ''
  accountDeleteConfirmDialogVisible.value = true
}

// 弹窗中确认 → 发送注销请求
const handleDialogConfirm = async () => {
  if (accountDeleteConfirmText.value !== accountDeleteExpectedText.value) return
  if (accountDeleteCountdown.value > 0) return
  try {
    await handleAccountDelete({
      oldPassword: accountDeleteForm.value.oldPassword,
      confirmPassword: accountDeleteForm.value.confirmPassword,
      securityAnswer: accountDeleteForm.value.securityAnswer
    })
  } catch {
    // 错误已在 handleAccountDelete 或全局拦截器中处理
  }
}

const handleInterviewHistoryClear = async () => {
  interviewHistoryClearing.value = true
  try {
    const res = await clearInterviewHistory()
    const deletedCount = Number(res?.data?.deletedCount ?? 0)
    ElMessage.success(`已清理 ${deletedCount} 条面试记录`)
    await fetchGrowthOverview()
  } finally {
    interviewHistoryClearing.value = false
  }
}

const handleInterviewHistoryClearConfirm = async () => {
  try {
    await ElMessageBox.confirm(
      '将清理当前账号下的全部历史面试会话和聊天记录，操作不可恢复。',
      '清理面试记录',
      {
        confirmButtonText: '确认清理',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await handleInterviewHistoryClear()
  } catch {
    // 用户取消或接口失败时保留现有页面状态。
  }
}

const handleResumeHistoryClear = async () => {
  resumeHistoryClearing.value = true
  try {
    const res = await clearResumeHistory()
    const deletedCount = Number(res?.data?.deletedCount ?? 0)
    ElMessage.success(`已清理 ${deletedCount} 条简历诊断记录`)
    await fetchGrowthOverview()
  } finally {
    resumeHistoryClearing.value = false
  }
}

const handleResumeHistoryClearConfirm = async () => {
  try {
    await ElMessageBox.confirm(
      '将清理当前账号下的全部简历诊断、JD 匹配、AI 润色记录和上传文件，操作不可恢复。',
      '清理简历诊断记录',
      {
        confirmButtonText: '确认清理',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await handleResumeHistoryClear()
  } catch {
    // 用户取消或接口失败时保留现有页面状态。
  }
}

const handleThemeChange = (value) => {
  themeChoice.value = value
  if (value === 'system') {
    themeStore.setFollowSystem(true)
    return
  }
  themeStore.setTheme(value)
}

const handleNotificationPreferenceSave = () => {
  syncPreferenceForms(saveSettingsPreferences(notificationForm.value))
}

const handleInterviewPreferenceSave = () => {
  syncPreferenceForms(saveSettingsPreferences(interviewPreferenceForm.value))
}

const handleVoicePreferredTypeChange = () => {
  if (isCloudTtsVoiceSelected.value) {
    interviewPreferenceForm.value.voiceName = ''
    interviewPreferenceForm.value.voiceURI = ''
    interviewPreferenceForm.value.voiceLang = ''
    handleTtsProviderChange('edge', {
      voiceId: getEdgeCloudTtsVoiceId(interviewPreferenceForm.value.voicePreferredType)
    })
    userTtsConfigExpanded.value = true
    previewTextToSpeech.setVoicePreference(buildVoicePreferenceFromForm())
    return
  }
  if (interviewPreferenceForm.value.voicePreferredType !== 'custom') {
    interviewPreferenceForm.value.voiceName = ''
    interviewPreferenceForm.value.voiceURI = ''
    interviewPreferenceForm.value.voiceLang = ''
    applyVoicePresetParameters()
  } else if (!selectedBrowserVoiceKey.value && browserVoiceOptions.value.length > 0) {
    selectedBrowserVoiceKey.value = browserVoiceOptions.value[0].value
    return
  }
  previewTextToSpeech.setVoicePreference(buildVoicePreferenceFromForm())
  handleInterviewPreferenceSave()
}

const handleBrowserVoiceChange = (value) => {
  const matchedOption = browserVoiceOptions.value.find((item) => item.value === value)
  const voice = matchedOption?.voice
  interviewPreferenceForm.value.voiceName = voice?.name || ''
  interviewPreferenceForm.value.voiceURI = voice?.voiceURI || ''
  interviewPreferenceForm.value.voiceLang = voice?.lang || ''
  previewTextToSpeech.setVoicePreference(buildVoicePreferenceFromForm())
  handleInterviewPreferenceSave()
}

const handleVoicePreview = async () => {
  if (isCloudTtsVoiceSelected.value) {
    await handleTtsVoicePreview()
    return
  }
  const browserTtsStyle = getResolvedBrowserTtsStyle()
  previewTextToSpeech.rate.value = browserTtsStyle.rate
  previewTextToSpeech.pitch.value = browserTtsStyle.pitch
  previewTextToSpeech.volume.value = browserTtsStyle.volume
  previewTextToSpeech.setVoicePreference(buildVoicePreferenceFromForm())
  // Chrome 的 voices 可能只有在用户点击手势内唤醒后才加载，试听前先预热以避免落到机械的系统默认音色。
  previewTextToSpeech.prepareForUserGesture?.()
  previewTextToSpeech.speak('你好，我是你的 AI 面试官。')
}

const handleVoicePreferenceReset = () => {
  const resetPreferences = {
    voiceSpeakingRate: DEFAULT_SETTINGS_PREFERENCES.voiceSpeakingRate,
    voicePitch: DEFAULT_SETTINGS_PREFERENCES.voicePitch,
    voiceVolume: DEFAULT_SETTINGS_PREFERENCES.voiceVolume,
    voiceMuteResumeMode: DEFAULT_SETTINGS_PREFERENCES.voiceMuteResumeMode,
    voiceAutoSubmitDelayMs: DEFAULT_SETTINGS_PREFERENCES.voiceAutoSubmitDelayMs,
    voiceRecognitionLanguage: DEFAULT_SETTINGS_PREFERENCES.voiceRecognitionLanguage,
    voiceRecognitionEngine: DEFAULT_SETTINGS_PREFERENCES.voiceRecognitionEngine,
    voicePreferredType: DEFAULT_SETTINGS_PREFERENCES.voicePreferredType,
    voiceName: DEFAULT_SETTINGS_PREFERENCES.voiceName,
    voiceURI: DEFAULT_SETTINGS_PREFERENCES.voiceURI,
    voiceLang: DEFAULT_SETTINGS_PREFERENCES.voiceLang
  }
  syncPreferenceForms(saveSettingsPreferences({
    ...interviewPreferenceForm.value,
    ...resetPreferences
  }))
  previewTextToSpeech.setVoicePreference(buildVoicePreferenceFromForm())
  ElMessage.success('语音偏好已恢复默认')
}

const handleDataManagementSettingsSave = async () => {
  const previousPreferences = getSettingsPreferences()
  userSettingsSaving.value = true
  try {
    const res = await saveUserSettings(buildServerSettingsPayload())
    syncPreferenceForms(saveSettingsPreferences({
      ...interviewPreferenceForm.value,
      ...res?.data
    }))
    ElMessage.success('设置已保存')
  } catch (err) {
    syncPreferenceForms(previousPreferences)
    throw err
  } finally {
    userSettingsSaving.value = false
  }
}

const handleDefaultJobChange = (value) => {
  const matchedJob = interviewJobOptions.value.find((item) => item.value === value)
  interviewPreferenceForm.value.defaultInterviewJobRole = matchedJob?.roleName || ''
  interviewPreferenceForm.value.defaultInterviewJobRoleCode = matchedJob?.roleCode || ''
  handleInterviewPreferenceSave()
}

const handleClearLocalCache = () => {
  // 清理范围只覆盖本机设置缓存，不能触碰登录 token，避免"清缓存"变成隐式退出登录。
  const defaults = clearLocalSettingsCache()
  syncPreferenceForms(defaults)
  themeChoice.value = 'light'
  ElMessage.success('本地设置缓存已清空，登录状态已保留')
}

const handleClearLocalCacheConfirm = async () => {
  try {
    await ElMessageBox.confirm(
      '将清理设置偏好、主题偏好和通知筛选缓存，不会退出当前账号。',
      '清空本地缓存',
      {
        confirmButtonText: '确认清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    handleClearLocalCache()
  } catch {
    // 用户取消清理时不需要额外提示，避免干扰设置页操作。
  }
}

const resetFeedbackForm = () => {
  feedbackForm.value = { type: 'bug', title: '', content: '', contact: '' }
  feedbackFormRef.value?.clearValidate()
}

const handleFeedbackSubmit = async () => {
  if (!feedbackFormRef.value) return
  try {
    await feedbackFormRef.value.validate()
  } catch {
    return
  }

  feedbackSubmitting.value = true
  try {
    await createUserFeedback({
      type: feedbackForm.value.type,
      title: feedbackForm.value.title.trim(),
      content: feedbackForm.value.content.trim(),
      contact: feedbackForm.value.contact.trim()
    })
    ElMessage.success('反馈已提交')
    resetFeedbackForm()
  } finally {
    feedbackSubmitting.value = false
  }
}

watch(
  () => [
    userAiConfigForm.value.baseUrl,
    userAiConfigForm.value.apiKey
  ],
  () => {
    userAiModelOptions.value = []
  }
)

watch(activeSection, (value) => {
  if (value === 'security' && securityMode.value === 'accountDeletion') {
    // 每次重新进入注销页签都重置表单和确认状态，重新获取安全问题。
    resetAccountDeleteForm()
    fetchAccountDeleteSecurityQuestion()
    return
  }
  clearAccountDeleteTimer()
})

onMounted(async () => {
  const tasks = []
  if (!userStore.userInfo) tasks.push(userStore.fetchUserInfo())
  tasks.push(fetchUserSettings())
  tasks.push(fetchInterviewJobOptions())
  tasks.push(fetchGrowthOverview())
  tasks.push(fetchUserAiConfigState())
  tasks.push(
    getMembershipPlans().then((res) => {
      membershipPlans.value = Array.isArray(res?.data) ? res.data : []
    }).catch(() => {
      membershipPlans.value = []
    })
  )
  await Promise.all(tasks)
})

onBeforeUnmount(() => {
  clearAccountDeleteTimer()
  releaseTtsPreviewAudio()
})

</script>

<style scoped>
.settings-view {
  --settings-ease: cubic-bezier(0.25, 1, 0.5, 1);
  --settings-panel-shadow: 0 18px 48px rgba(255, 140, 66, 0.09);
  --settings-nav-sticky-top: calc(var(--header-height, 82px) + 24px);
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  min-height: calc(100dvh - var(--header-height, 82px) - 96px);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.settings-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.settings-header h1 {
  margin: 0;
  color: var(--text-title);
  font-size: 24px;
  line-height: 1.3;
}

.settings-header p,
.panel-heading p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.6;
}

.settings-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
  align-items: stretch;
  flex: 1;
}

.settings-workspace {
  min-height: 0;
  width: 100%;
}

.settings-nav {
  position: sticky;
  top: calc(var(--header-height, 82px) + 24px);
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
  min-height: min(720px, calc(100dvh - var(--header-height, 82px) - 128px));
  border: 1px solid var(--border-card);
  border-radius: 16px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bg-card) 96%, var(--orange-main) 4%), var(--bg-card));
  box-shadow: 0 12px 34px rgba(255, 140, 66, 0.07);
}

.settings-nav-item {
  position: relative;
  width: 100%;
  min-height: 50px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text-body);
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.18s var(--settings-ease),
    color 0.18s var(--settings-ease),
    transform 0.15s var(--settings-ease),
    box-shadow 0.18s var(--settings-ease);
}

.settings-nav-item::after {
  content: "";
  position: absolute;
  right: 12px;
  top: 50%;
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--orange-main);
  opacity: 0;
  transform: translateY(-50%) scale(0.6);
  transition:
    opacity 0.18s var(--settings-ease),
    transform 0.18s var(--settings-ease);
}

.settings-nav-item:hover,
.settings-nav-item.active {
  background: linear-gradient(135deg, var(--orange-light-bg), color-mix(in srgb, var(--bg-card) 88%, var(--orange-main) 12%));
  color: var(--orange-deep);
}

.settings-nav-item:hover {
  box-shadow: 0 8px 20px rgba(255, 140, 66, 0.1);
  transform: translateY(-1px);
}

.settings-nav-item.active {
  box-shadow: 0 1px 8px rgba(255, 140, 66, 0.1);
}

.settings-nav-item.active::after {
  opacity: 1;
  transform: translateY(-50%) scale(1);
}

.settings-nav-icon {
  transition: transform 0.2s cubic-bezier(0.25, 1, 0.5, 1);
}

.settings-nav-item:hover .settings-nav-icon,
.settings-nav-item.active .settings-nav-icon {
  transform: translateY(-1px) scale(1.06);
}

.settings-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.settings-panel {
  min-height: min(720px, calc(100dvh - var(--header-height, 82px) - 128px));
  display: flex;
  flex-direction: column;
  padding: 24px;
  border: 1px solid var(--border-card);
  border-radius: 18px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bg-card) 98%, var(--orange-main) 2%), var(--bg-card));
  box-shadow: var(--settings-panel-shadow);
}

.settings-panel-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  gap: 14px;
  margin-bottom: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border-divider);
}

.panel-heading > div {
  min-width: 0;
}

.panel-heading > div:first-of-type {
  flex: 1;
}

.panel-heading-copy {
  flex: 1;
}

.panel-heading-icon {
  flex: 0 0 auto;
  margin-top: 1px;
}

.panel-heading h2 {
  margin: 0;
  color: var(--text-title);
}

.panel-heading h2 {
  font-size: 20px;
}

.profile-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  margin-bottom: 18px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: var(--bg-page);
}

.profile-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
}

.profile-summary :deep(.profile-avatar) {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.profile-name {
  color: var(--text-title);
  font-size: 18px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.profile-meta {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
}

.profile-workspace {
  gap: 18px;
}

.profile-overview-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--border-card) 82%, var(--orange-main) 18%);
  border-radius: 16px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--bg-page) 82%, var(--orange-main) 18%), var(--bg-card));
}

.profile-overview-card :deep(.profile-avatar) {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.profile-main p {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.65;
}

.profile-info-grid {
  margin-bottom: 0;
}

.profile-support-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.profile-support-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--border-card) 84%, var(--orange-main) 16%);
  border-radius: 12px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bg-page) 92%, var(--orange-main) 8%), var(--bg-page));
}

.profile-support-card strong,
.profile-support-card span {
  display: block;
}

.profile-support-card strong {
  color: var(--text-title);
  font-size: 14px;
}

.profile-support-card span {
  margin-top: 7px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 0;
}

.info-item {
  position: relative;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: var(--bg-page);
  overflow: hidden;
  transition:
    border-color 0.2s var(--settings-ease),
    box-shadow 0.2s var(--settings-ease),
    transform 0.16s var(--settings-ease);
}

.info-item::before {
  content: "";
  position: absolute;
  inset: auto 14px 12px auto;
  width: 18px;
  height: 3px;
  border-radius: 999px;
  background: var(--orange-main);
  opacity: 0;
  transform: scaleX(0.45);
  transform-origin: right center;
  transition:
    opacity 0.2s var(--settings-ease),
    transform 0.2s var(--settings-ease);
}

.info-item:hover {
  border-color: var(--orange-border);
  box-shadow: 0 8px 22px rgba(255, 140, 66, 0.08);
  transform: translateY(-1px);
}

.info-item:hover::before {
  opacity: 1;
  transform: scaleX(1);
}

.info-item span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.info-item strong {
  display: block;
  margin-top: 8px;
  color: var(--text-title);
  font-size: 16px;
  overflow-wrap: anywhere;
}

.settings-form {
  max-width: 520px;
  --el-input-height: 40px;
  --el-component-size: 40px;
}

.settings-form :deep(.el-input__inner) {
  font-size: 14px;
}

.feedback-form {
  width: 100%;
}

.full-width {
  width: 100%;
}

/* ── 注销账号区域 ── */
.account-delete-zone {
  width: 100%;
  max-width: 520px;
}

.account-delete-alert {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 20px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-card) 88%, #f56c6c 12%);
  border: 1px solid color-mix(in srgb, var(--border-card) 50%, #f56c6c 50%);
  color: #b42318;
  font-size: 13px;
  line-height: 1.5;
}

.settings-alert-icon {
  flex-shrink: 0;
  margin-top: 1px;
  color: #f56c6c;
}

.account-delete-form {
  width: 100%;
  max-width: none;
  box-sizing: border-box;
  padding: 20px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: color-mix(in srgb, var(--bg-page) 92%, #f56c6c 8%);
}

.account-delete-form .el-button--danger {
  min-width: 168px;
}

.security-question-card {
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 10px;
  border: 1px solid var(--border-card);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-body);
  font-size: 13px;
}

.security-question-card.loading {
  color: var(--text-muted);
}

.security-question-card.error {
  border-color: color-mix(in srgb, var(--border-card) 50%, #f56c6c 50%);
  color: #b42318;
}

.security-question-text {
  min-width: 0;
  line-height: 1.55;
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.security-question-card.expanded .security-question-text {
  display: block;
  overflow: visible;
}

.security-question-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.security-question-toggle {
  min-height: 28px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--orange-deep);
  font-size: 13px;
  cursor: pointer;
}

.security-question-toggle:focus-visible {
  outline: 2px solid var(--orange-main);
  outline-offset: 2px;
}

.security-mode-tabs,
.sub-nav-tabs {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--border-divider);
  overflow-x: auto;
}

.security-mode-tab,
.sub-nav-tab {
  position: relative;
  flex: 0 0 auto;
  min-height: 40px;
  padding: 8px 18px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--text-body);
  font-size: 14px;
  cursor: pointer;
  transition:
    color 0.2s var(--settings-ease),
    border-color 0.2s var(--settings-ease),
    background-color 0.2s var(--settings-ease),
    transform 0.16s var(--settings-ease);
}

.security-mode-tab:hover,
.security-mode-tab.active,
.sub-nav-tab:hover,
.sub-nav-tab.active {
  color: var(--orange-main);
}

.security-mode-tab.danger {
  color: #b42318;
}

.security-mode-tab.active,
.sub-nav-tab.active {
  border-bottom-color: var(--orange-main);
  font-weight: 600;
}

.security-mode-tab.danger.active {
  border-bottom-color: #f56c6c;
  color: #b42318;
}

.security-mode-tab:focus-visible,
.sub-nav-tab:focus-visible {
  outline: 2px solid var(--orange-main);
  outline-offset: 2px;
}

.security-panel-enter-active,
.security-panel-leave-active {
  transition:
    opacity 0.2s var(--settings-ease),
    transform 0.2s var(--settings-ease);
}

.security-panel-enter-from,
.security-panel-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.section-fade-enter-active,
.section-fade-leave-active {
  transition:
    opacity 0.25s var(--settings-ease),
    transform 0.25s var(--settings-ease);
}

.section-fade-enter-from,
.section-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.appearance-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.4;
}

.appearance-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.appearance-option {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: var(--bg-page);
  color: var(--text-body);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s var(--settings-ease),
    background-color 0.2s var(--settings-ease),
    box-shadow 0.2s var(--settings-ease),
    transform 0.16s var(--settings-ease);
}

.appearance-option:hover,
.appearance-option.active {
  border-color: var(--orange-main);
  background: var(--bg-card);
}

.appearance-option.active {
  box-shadow: 0 0 0 2px rgba(255, 140, 66, 0.14);
}

.appearance-option:hover {
  transform: translateY(-1px);
}

.appearance-option:focus-visible {
  outline: 2px solid var(--orange-main);
  outline-offset: 2px;
}

.appearance-option strong {
  color: var(--text-title);
  font-size: 15px;
}

.appearance-option em {
  color: var(--text-muted);
  font-size: 13px;
  font-style: normal;
  line-height: 1.5;
}

.appearance-preview {
  width: 100%;
  height: 76px;
  display: grid;
  grid-template-columns: 28px 1fr;
  grid-template-rows: 18px 1fr;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid var(--border-card);
}

.appearance-preview span:first-child {
  grid-row: 1 / 3;
  border-radius: 6px;
}

.appearance-preview span:nth-child(2),
.appearance-preview span:nth-child(3) {
  border-radius: 6px;
}

.appearance-preview.light {
  background: #f8fafc;
}

.appearance-preview.light span:first-child {
  background: #ffffff;
}

.appearance-preview.light span:nth-child(2) {
  background: #ff8c42;
}

.appearance-preview.light span:nth-child(3) {
  background: #e5e7eb;
}

.appearance-preview.dark {
  background: #111827;
  border-color: #374151;
}

.appearance-preview.dark span:first-child {
  background: #1f2937;
}

.appearance-preview.dark span:nth-child(2) {
  background: #ff8c42;
}

.appearance-preview.dark span:nth-child(3) {
  background: #374151;
}

.appearance-preview.system {
  background: linear-gradient(90deg, #f8fafc 0 50%, #111827 50% 100%);
}

.appearance-preview.system span:first-child {
  background: linear-gradient(90deg, #ffffff 0 50%, #1f2937 50% 100%);
}

.appearance-preview.system span:nth-child(2) {
  background: #ff8c42;
}

.appearance-preview.system span:nth-child(3) {
  background: linear-gradient(90deg, #e5e7eb 0 50%, #374151 50% 100%);
}

.preference-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preference-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px;
  background: var(--bg-page);
  border: 1px solid var(--border-card);
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(255, 140, 66, 0.04);
  transition:
    border-color 0.2s var(--settings-ease),
    box-shadow 0.2s var(--settings-ease),
    transform 0.16s var(--settings-ease);
}

.preference-row:hover {
  border-color: var(--orange-border);
  box-shadow: 0 10px 26px rgba(255, 140, 66, 0.08);
  transform: translateY(-1px);
}

.preference-row.stacked {
  align-items: flex-start;
}

.preference-row.danger-row {
  background: color-mix(in srgb, var(--bg-card) 92%, #f56c6c 8%);
}

.data-retention-row {
  align-items: flex-start;
}

.preference-row strong,
.preference-row span {
  display: block;
}

.preference-row > div:first-child {
  min-width: 0;
}

.preference-row strong {
  color: var(--text-title);
  font-size: 14px;
}

.preference-row span {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.notification-type-select {
  width: 220px;
  flex-shrink: 0;
}

.preference-select {
  width: 260px;
  flex-shrink: 0;
}

.preference-slider {
  width: 260px;
  flex-shrink: 0;
}

.browser-voice-select {
  width: min(100%, 360px);
  max-width: 100%;
  min-width: 0;
  flex-shrink: 1;
}

/* ── Custom AI: usage bar ── */
.cai-usage-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-page);
  border: 1px solid var(--border-card);
  border-radius: 12px;
}

.cai-usage-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-shrink: 0;
}

.cai-usage-label {
  color: var(--text-muted);
  font-size: 13px;
}

.cai-usage-numbers {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  font-size: 13px;
  color: var(--text-muted);
}

.cai-usage-numbers strong {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-title);
  line-height: 1;
}

.cai-usage-sep {
  margin: 0 1px;
  opacity: 0.4;
}

.cai-usage-track {
  flex: 1;
  height: 6px;
  background: var(--border-card);
  border-radius: 3px;
  overflow: hidden;
}

.cai-usage-fill {
  height: 100%;
  background: var(--orange-main);
  border-radius: 3px;
  transition: width 0.4s ease;
}

.cai-usage-fill.warning {
  background: #ef4444;
}

.cai-usage-remaining {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-muted);
  opacity: 0.7;
}

/* ── Custom AI: type slots ── */
.cai-type-slots {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.cai-slot {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  background: var(--bg-page);
  border: 1.5px solid var(--border-card);
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
}

.cai-slot:hover {
  border-color: color-mix(in srgb, var(--orange-main) 50%, transparent);
  transform: translateY(-1px);
}

.cai-slot.editing {
  border-color: var(--orange-main);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--orange-main) 12%, transparent);
}

.cai-slot-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cai-slot-icon {
  flex-shrink: 0;
}

.cai-slot-type {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-title);
}

.cai-slot-detail {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.cai-slot-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-title);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cai-slot-endpoint {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  opacity: 0.7;
}

.cai-slot-tts {
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
  font-size: 11px;
  font-weight: 700;
}

.cai-slot-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.cai-slot-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 500;
}

.cai-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.cai-slot-status.ok {
  color: #059669;
}

.cai-slot-status.ok .cai-status-dot {
  background: #10b981;
  box-shadow: 0 0 4px rgba(16, 185, 129, 0.5);
}

.cai-slot-status.fail {
  color: #dc2626;
}

.cai-slot-status.fail .cai-status-dot {
  background: #ef4444;
}

.cai-slot-status.pending {
  color: var(--text-muted);
}

.cai-slot-status.pending .cai-status-dot {
  background: var(--text-muted);
  opacity: 0.5;
}

.cai-slot-del {
  all: unset;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  font-size: 14px;
  color: var(--text-muted);
  border-radius: 4px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s, background 0.15s;
}

.cai-slot:hover .cai-slot-del {
  opacity: 1;
}

.cai-slot-del:hover {
  color: #dc2626;
  background: rgba(239, 68, 68, 0.08);
}

.cai-slot-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  color: var(--text-muted);
  font-size: 13px;
  opacity: 0.6;
}

/* ── Custom AI: form card ── */
.cai-form-card {
  padding: 20px;
  background: var(--bg-page);
  border: 1px solid var(--border-card);
  border-radius: 14px;
}

.cai-form-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-divider);
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
}

.cai-form-title-action {
  margin-left: auto;
}

.cai-form-title :deep(.feature-icon) {
  flex-shrink: 0;
}

.cai-form .el-form-item {
  margin-bottom: 16px;
}

/* 自定义 AI 表单：统一输入控件高度，避免过扁 */
.cai-form {
  --cai-form-control-height: 40px;
  --cai-form-control-inner-height: 32px;
  --el-input-height: var(--cai-form-control-height);
  --el-component-size: var(--cai-form-control-height);
  --el-select-height: var(--cai-form-control-height);
  font-size: 14px;
}

.cai-form :deep(.el-input__inner) {
  font-size: 14px;
}

.cai-form :deep(.el-input__wrapper),
.cai-form :deep(.el-select__wrapper) {
  height: var(--cai-form-control-height);
  min-height: var(--cai-form-control-height);
  padding: 4px 12px;
  box-sizing: border-box;
}

.cai-form :deep(.el-select) {
  --el-input-height: var(--cai-form-control-height);
  --el-component-size: var(--cai-form-control-height);
  height: var(--cai-form-control-height);
}

.cai-form :deep(.el-select .el-input__wrapper),
.cai-form :deep(.el-select .el-select__wrapper) {
  height: var(--cai-form-control-height);
  min-height: var(--cai-form-control-height);
  padding: 4px 12px;
  box-sizing: border-box;
}

.cai-form :deep(.el-textarea__inner) {
  padding: 8px 12px;
}

.cai-form-row-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.cai-form-row-2col .el-form-item {
  margin-bottom: 16px;
}

.cai-form-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 16px;
  background: color-mix(in srgb, var(--bg-card) 60%, var(--bg-page));
  border: 1px solid var(--border-card);
  border-radius: 10px;
}

.cai-form-toggle-info {
  min-width: 0;
}

.cai-form-toggle-info strong {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-title);
}

.cai-form-toggle-info span {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.4;
}

.cai-tts-section {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
  margin-bottom: 16px;
  background: color-mix(in srgb, var(--bg-card) 66%, var(--bg-page));
  border: 1px solid var(--border-card);
  border-radius: 12px;
  overflow: hidden;
}

/* 获取模型/音色：服务商选择 + 按钮同行对齐 */
.cai-tts-discover-row {
  display: flex;
  gap: 12px;
}

.cai-tts-provider-field {
  flex: 1;
}

.cai-tts-discover-btn {
  align-self: flex-end;
  flex-shrink: 0;
  height: var(--cai-form-control-height);
  margin-bottom: 16px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 音色 ID + 试听按钮横向排列 */
.cai-tts-voice-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.cai-tts-voice-row .el-select,
.cai-tts-voice-row .el-input {
  flex: 1;
}

/* TTS 试听喇叭按钮：复用面试偏好的样式，居中图标 */
.tts-voice-preview-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.cai-tts-heading {
  all: unset;
  box-sizing: border-box;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  cursor: pointer;
}

.cai-tts-heading:focus-visible {
  outline: 2px solid var(--orange-main);
  outline-offset: -2px;
}

.cai-tts-heading-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.cai-tts-heading strong {
  font-size: 14px;
  color: var(--text-title);
}

.cai-tts-heading span {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}

.cai-tts-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(96, 165, 250, 0.14);
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.cai-tts-status.active {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.cai-tts-runtime-status {
  margin: -4px 16px 12px;
  padding: 8px 10px;
  border: 1px solid rgba(96, 165, 250, 0.18);
  border-radius: 8px;
  background: rgba(96, 165, 250, 0.08);
  color: #2563eb;
  font-size: 12px;
  line-height: 1.5;
}

.cai-tts-toggle-icon {
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
  transition: transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.cai-tts-toggle-icon.expanded {
  transform: rotate(180deg);
}

.cai-tts-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 16px 20px;
}

.cai-test-result {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 14px;
  font-size: 13px;
  color: #047857;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 10px;
}

.cai-test-result.failed {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.2);
}

.cai-test-icon {
  font-weight: 700;
  font-size: 15px;
}

.cai-test-latency {
  margin-left: auto;
  font-size: 12px;
  opacity: 0.7;
  font-variant-numeric: tabular-nums;
}

.cai-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cai-form-actions.compact {
  justify-content: flex-start;
}

.cai-hint {
  margin: 0;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  opacity: 0.6;
}

/* ── Custom AI: refresh button (复用 data-overview-refresh-btn 样式) ── */


.preference-action {
  flex: 0 0 auto;
}

.preference-action:active,
.voice-preview-button:active:not(.is-disabled) {
  transform: scale(0.97);
}

.voice-preference-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
}

.voice-preference-main {
  width: 100%;
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto;
  gap: 18px;
  align-items: center;
}

.voice-preference-copy {
  min-width: 0;
}

.voice-selection-status {
  width: min(100%, 720px);
  margin: 0;
  color: var(--settings-muted);
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.voice-selection-status.degraded {
  color: #b45309;
}
.voice-control {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.voice-control .preference-select {
  min-width: 0;
}

.voice-preview-button {
  width: 52px;
  min-width: 52px;
  height: 48px;
  padding: 0;
  border-radius: 10px;
  transition:
    border-color 0.2s cubic-bezier(0.25, 1, 0.5, 1),
    background-color 0.2s cubic-bezier(0.25, 1, 0.5, 1),
    color 0.2s cubic-bezier(0.25, 1, 0.5, 1),
    box-shadow 0.2s cubic-bezier(0.25, 1, 0.5, 1),
    transform 0.16s cubic-bezier(0.25, 1, 0.5, 1);
}

.voice-preview-button:hover:not(.is-disabled):not(.n-button--disabled) {
  color: #fff;
  background: var(--orange-main);
  border-color: var(--orange-main);
  box-shadow: 0 8px 18px rgba(255, 140, 66, 0.22);
  transform: translateY(-1px);
}

.voice-preview-button:active:not(.is-disabled):not(.n-button--disabled) {
  box-shadow: 0 4px 10px rgba(255, 140, 66, 0.18);
  transform: translateY(0) scale(0.96);
}

.voice-preview-button:focus-visible {
  outline: 2px solid var(--orange-main);
  outline-offset: 2px;
}

.voice-preview-button.is-disabled,
.voice-preview-button.n-button--disabled {
  box-shadow: none;
  transform: none;
}

.voice-preview-button:hover:not(.is-disabled):not(.n-button--disabled) .voice-preview-icon {
  transform: scale(1.08);
}

.voice-preview-icon {
  transition: transform 0.2s cubic-bezier(0.25, 1, 0.5, 1);
}

:global(.browser-voice-select-popper) {
  width: min(360px, calc(100vw - 24px)) !important;
  max-width: calc(100vw - 24px);
}

:global(.browser-voice-select-popper .el-select-dropdown__wrap) {
  max-height: min(300px, calc(100vh - 180px));
}

:global(.browser-voice-select-popper .el-select-dropdown__item) {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.voice-preference-heading {
  margin-bottom: 12px;
}

.voice-preference-heading h3 {
  margin: 0;
  color: var(--text-title);
  font-size: 16px;
}

.voice-preference-heading p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.danger-zone {
  margin-top: 24px;
  border: 1px solid color-mix(in srgb, var(--border-card) 72%, #f56c6c 28%);
  border-radius: 10px;
  overflow: hidden;
}

.data-overview-refresh-btn {
  width: 48px;
  height: 48px;
  transition: background-color 0.25s, border-color 0.25s, box-shadow 0.25s;
}

.data-overview-refresh-btn:hover {
  border-color: var(--orange-main);
  color: var(--orange-main);
  box-shadow: 0 0 0 3px rgba(255, 140, 66, 0.1);
}

.settings-refresh-icon {
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.data-overview-refresh-btn:hover .settings-refresh-icon,
.data-overview-refresh-btn.is-refreshing .settings-refresh-icon {
  animation: data-overview-spin 0.75s linear infinite;
}

@keyframes data-overview-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.data-overview-grid {
  margin-bottom: 0;
}

.inline-warning {
  padding: 12px 14px;
  border: 1px solid var(--orange-border);
  border-radius: 10px;
  background: var(--orange-light-bg);
  color: var(--orange-deep);
  font-size: 13px;
  line-height: 1.6;
}

.quota-grid {
  margin-bottom: 0;
}

.settings-fill-note {
  margin-top: auto;
  padding: 16px;
  border: 1px solid color-mix(in srgb, var(--border-card) 86%, var(--orange-main) 14%);
  border-radius: 14px;
  background: color-mix(in srgb, var(--bg-page) 88%, var(--orange-main) 12%);
}

.settings-fill-note strong,
.settings-fill-note span {
  display: block;
}

.settings-fill-note strong {
  color: var(--text-title);
  font-size: 14px;
}

.settings-fill-note span {
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.compact-action-panel {
  justify-content: flex-start;
}

.onboarding-intro-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.onboarding-intro-card {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--border-card);
  border-radius: 12px;
  background: var(--bg-page);
}

.onboarding-intro-card strong,
.onboarding-intro-card span {
  display: block;
}

.onboarding-intro-card strong {
  color: var(--text-title);
  font-size: 15px;
}

.onboarding-intro-card span {
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.65;
}

.onboarding-action-note {
  margin-top: auto;
}

.membership-note {
  margin-top: 0;
}

/* 平板竖屏 / 小屏笔记本：导航从左侧改为顶部横向滚动 */
@media (max-width: 900px) {
  .settings-view {
    min-height: 0;
    gap: 16px;
  }

  .settings-layout {
    grid-template-columns: 1fr;
    align-items: start;
    gap: 16px;
  }

  .settings-nav {
    position: sticky;
    top: var(--header-height, 64px);
    z-index: 10;
    min-height: 0;
    overflow-x: auto;
    overflow-y: visible;
    flex-direction: row;
    flex-wrap: nowrap;
    padding: 8px;
    border-radius: 14px;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }

  .settings-nav::-webkit-scrollbar {
    display: none;
  }

  .settings-panel {
    min-height: 0;
  }

  .settings-nav-item {
    flex: 0 0 auto;
    width: auto;
    min-height: 44px;
    padding: 8px 14px;
    font-size: 13px;
    white-space: nowrap;
    gap: 8px;
  }

  .settings-nav-item :deep(.feature-icon),
  .settings-nav-item :deep(.feature-icon.size-md) {
    width: 32px;
    height: 32px;
  }

  .settings-nav-item::after {
    right: auto;
    left: 50%;
    top: auto;
    bottom: 4px;
    width: 18px;
    height: 3px;
    border-radius: 2px;
    transform: translateX(-50%) scaleX(0.6);
  }

  .settings-nav-item.active::after {
    transform: translateX(-50%) scaleX(1);
  }

  .appearance-options {
    grid-template-columns: 1fr;
  }

  .cai-type-slots {
    grid-template-columns: 1fr 1fr 1fr;
  }

  .cai-form-row-2col {
    grid-template-columns: 1fr;
  }

  .account-delete-zone {
    max-width: none;
  }

  .profile-overview-card {
    flex-direction: column;
    align-items: flex-start;
    text-align: left;
  }
}

/* 手机横屏及以下 */
@media (max-width: 640px) {
  .settings-header h1 {
    font-size: 20px;
  }

  .settings-panel {
    padding: 16px;
    border-radius: 14px;
  }

  .panel-heading {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    margin-bottom: 14px;
    padding-bottom: 14px;
  }

  .panel-heading h2 {
    font-size: 17px;
  }

  .panel-heading-icon :deep(.feature-icon),
  .panel-heading-icon:deep(.feature-icon.size-lg) {
    width: 40px;
    height: 40px;
  }

  .profile-overview-card {
    padding: 14px;
    gap: 12px;
  }

  .profile-overview-card :deep(.profile-avatar) {
    width: 52px;
    height: 52px;
  }

  .profile-name {
    font-size: 16px;
  }

  .profile-name-row {
    flex-wrap: wrap;
    gap: 6px;
  }

  .info-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .info-item {
    padding: 12px;
  }

  .info-item strong {
    font-size: 14px;
  }

  .profile-support-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .profile-support-card {
    padding: 12px;
  }

  .notification-type-select {
    width: 100%;
  }

  .preference-select,
  .preference-slider {
    width: 100%;
  }

  .preference-row {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding: 14px;
  }

  .preference-row.stacked {
    gap: 10px;
  }

  .voice-preference-main {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .voice-control {
    width: 100%;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 52px;
    align-items: center;
  }

  .onboarding-intro-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  /* 导航图标缩小 */
  .settings-nav-item :deep(.feature-icon),
  .settings-nav-item :deep(.feature-icon.size-md) {
    width: 28px;
    height: 28px;
  }

  .voice-preview-button {
    width: 48px;
    min-width: 48px;
    height: 44px;
  }

  .browser-voice-select {
    width: 100%;
  }

  .cai-type-slots {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .cai-usage-bar {
    flex-wrap: wrap;
    gap: 8px;
  }

  .cai-form-row-2col {
    grid-template-columns: 1fr;
  }

  .cai-form-actions {
    flex-direction: column;
  }

  .cai-form-actions .n-button {
    width: 100%;
  }

  .cai-slot {
    padding: 12px;
  }

  .appearance-status {
    align-items: flex-start;
  }

  .appearance-preview {
    height: 56px;
  }

  .account-delete-form .el-button--danger {
    width: 100%;
  }

  .settings-form {
    max-width: none;
  }

  .security-mode-tabs,
  .sub-nav-tabs {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }

  .security-mode-tabs::-webkit-scrollbar,
  .sub-nav-tabs::-webkit-scrollbar {
    display: none;
  }

  .security-mode-tab,
  .sub-nav-tab {
    padding: 8px 14px;
    font-size: 13px;
    white-space: nowrap;
  }

  .data-overview-grid {
    gap: 8px;
  }

  .quota-grid .info-item {
    padding: 10px;
  }
}

/* 超小屏手机 */
@media (max-width: 380px) {
  .settings-panel {
    padding: 12px;
    border-radius: 12px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .settings-nav-item {
    padding: 6px 10px;
    min-height: 38px;
    font-size: 12px;
    gap: 6px;
  }

  .settings-nav-item :deep(.feature-icon),
  .settings-nav-item :deep(.feature-icon.size-md) {
    width: 24px;
    height: 24px;
  }

  .settings-nav {
    padding: 6px;
    gap: 2px;
  }

  .profile-overview-card {
    padding: 10px;
    gap: 10px;
  }

  .preference-row {
    padding: 10px;
    gap: 8px;
  }

  .panel-heading {
    margin-bottom: 10px;
    padding-bottom: 10px;
  }

  .cai-form-card {
    padding: 12px;
  }

  .settings-fill-note {
    padding: 12px;
  }
}

@media (max-width: 420px) {
  :global(.browser-voice-select-popper) {
    width: calc(100vw - 24px) !important;
  }
}

@media (prefers-reduced-motion: reduce) {
  .voice-preview-button,
  .voice-preview-icon,
  .settings-nav-icon,
  .settings-refresh-icon,
  .settings-nav-item,
  .settings-nav-item::after,
  .info-item,
  .info-item::before,
  .appearance-option,
  .preference-row,
  .sub-nav-tab,
  .security-mode-tab,
  .settings-panel,
  .security-panel-enter-active,
  .security-panel-leave-active,
  .section-fade-enter-active,
  .section-fade-leave-active {
    transition-duration: 0.01ms;
  }

  .section-fade-enter-from,
  .section-fade-leave-to,
  .security-panel-enter-from,
  .security-panel-leave-to {
    opacity: 1;
    transform: none;
  }
}
</style>

<!-- 注销弹窗样式（unscoped，因 el-dialog teleport 到 body） -->
<style>
.account-delete-dialog {
  max-width: calc(100vw - 32px);
}

.account-delete-dialog .el-dialog__header {
  padding: 16px 20px;
  margin-right: 0;
  border-bottom: 1px solid var(--border-card);
}

.account-delete-dialog .el-dialog__title {
  font-weight: 600;
  font-size: 16px;
  color: var(--text-title);
}

.account-delete-dialog .el-dialog__headerbtn .el-dialog__close {
  color: var(--text-muted);
}

.account-delete-dialog .el-dialog__body {
  padding: 20px;
}

.delete-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.delete-warning-box {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e53935;
  background: #ffebee;
}

.delete-warning-box .delete-warning-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(229, 57, 53, 0.12);
  margin-bottom: 10px;
  color: #d32f2f;
}

.delete-warning-box .delete-warning-icon :deep(.feature-icon) {
  width: 32px;
  height: 32px;
}

.delete-warning-text strong {
  display: block;
  font-size: 14px;
  color: #b71c1c;
  margin-bottom: 6px;
}

.delete-warning-text p {
  margin: 0;
  font-size: 13px;
  color: #c62828;
  line-height: 1.6;
}

.delete-warning-text p strong {
  display: inline;
  font-size: 13px;
  color: #b71c1c;
}

.delete-confirm-hint {
  margin: 0 0 10px;
  font-size: 13px;
  color: #c62828;
  line-height: 1.5;
}

.delete-dialog-code {
  display: block;
  padding: 8px 12px;
  margin-bottom: 12px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #ffcdd2;
  font-family: monospace;
  font-size: 15px;
  font-weight: 600;
  color: #b71c1c;
  user-select: all;
  word-break: break-all;
}

.delete-confirm-input {
  margin-top: 0;
}

/* 暗色模式 */
[data-theme="dark"] .delete-warning-box {
  background: rgba(229, 57, 53, 0.12);
  border-color: rgba(229, 57, 53, 0.4);
}

[data-theme="dark"] .delete-warning-box .delete-warning-icon {
  background: rgba(229, 57, 53, 0.2);
}

[data-theme="dark"] .delete-warning-text strong,
[data-theme="dark"] .delete-warning-text p,
[data-theme="dark"] .delete-confirm-hint {
  color: #ef9a9a;
}

[data-theme="dark"] .delete-dialog-code {
  background: rgba(0, 0, 0, 0.2);
  border-color: rgba(229, 57, 53, 0.3);
  color: #ef9a9a;
}

/* 移动端响应式 */
@media (max-width: 520px) {
  .account-delete-dialog {
    width: 92% !important;
  }
  .account-delete-dialog .el-dialog__header {
    padding: 14px 16px;
  }
  .account-delete-dialog .el-dialog__body {
    padding: 14px;
  }
  .account-delete-dialog .el-dialog__footer {
    padding: 10px 14px;
  }
  .delete-warning-box {
    padding: 12px;
  }
  .delete-warning-box .delete-warning-icon {
    width: 40px;
    height: 40px;
    margin-bottom: 8px;
  }
  .delete-warning-text strong {
    font-size: 13px;
  }
  .delete-warning-text p {
    font-size: 12px;
  }
  .delete-confirm-hint {
    font-size: 12px;
    margin-bottom: 8px;
  }
  .delete-dialog-code {
    font-size: 13px;
    padding: 6px 10px;
    margin-bottom: 10px;
  }
}

@media (max-width: 380px) {
  .account-delete-dialog {
    width: 96% !important;
  }
  .account-delete-dialog .el-dialog__body {
    padding: 12px;
  }
  .delete-warning-box {
    padding: 10px;
  }
}

/* 自定义 AI 表单：el-select 高度统一（非 scoped，穿透 Element Plus 组件） */
.cai-form .el-select .el-input__wrapper,
.cai-form .el-select .el-select__wrapper {
  height: var(--cai-form-control-height, 40px) !important;
  min-height: var(--cai-form-control-height, 40px) !important;
  padding: 4px 12px !important;
  box-sizing: border-box;
}

.cai-form .el-select .el-input__inner,
.cai-form .el-select .el-select__input {
  height: var(--cai-form-control-inner-height, 32px) !important;
  line-height: var(--cai-form-control-inner-height, 32px) !important;
  font-size: 14px !important;
}
</style>
