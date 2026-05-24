<template>
  <div class="settings-view">
    <header class="settings-header">
      <div>
        <h1>设置中心</h1>
        <p>管理账号、安全、面试偏好、隐私数据和本机显示通知偏好。</p>
      </div>
    </header>

    <div class="settings-layout">
      <aside class="settings-nav" aria-label="设置分组">
        <button
          v-for="section in sections"
          :key="section.key"
          type="button"
          class="settings-nav-item"
          :class="{ active: activeSection === section.key }"
          @click="activeSection = section.key"
        >
          <FeatureIcon :name="section.icon" size="md" class="settings-nav-icon" />
          <span>{{ section.label }}</span>
        </button>
      </aside>

      <main class="settings-content">
        <section v-show="activeSection === 'profile'" class="settings-panel" aria-labelledby="profile-title">
          <div class="panel-heading">
            <div>
              <h2 id="profile-title">账号资料</h2>
              <p>查看当前账号信息、订阅状态和注册时间。</p>
            </div>
          </div>

          <div class="profile-summary">
            <img src="@/assets/user.png" alt="用户头像" class="profile-avatar" />
            <div class="profile-main">
              <div class="profile-name-row">
                <div class="profile-name">{{ displayName }}</div>
              </div>
              <div class="profile-meta">{{ userInfo?.username || '--' }}</div>
            </div>
            <el-tag :type="roleTagType" effect="plain">{{ roleText }}</el-tag>
          </div>

          <div class="info-grid">
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
        </section>

        <section v-show="activeSection === 'interview'" class="settings-panel" aria-labelledby="interview-title">
          <div class="panel-heading">
            <div>
              <h2 id="interview-title">面试偏好</h2>
              <p>设置进入模拟面试时优先带入的默认配置，偏好仅保存在当前浏览器。</p>
            </div>
          </div>

          <div class="preference-list">
            <div class="preference-row stacked">
              <div>
                <strong>默认面试岗位</strong>
                <span>只在岗位仍处于启用状态时自动回填，避免旧岗位配置污染新会话。</span>
              </div>
              <el-select
                v-model="interviewPreferenceForm.defaultInterviewJobRoleCode"
                class="preference-select"
                filterable
                @change="handleDefaultJobChange"
              >
                <el-option label="不设默认岗位" value="" />
                <el-option
                  v-for="job in interviewJobOptions"
                  :key="job.value"
                  :label="job.label"
                  :value="job.value"
                />
              </el-select>
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>默认面试级别</strong>
                <span>进入面试入口页时默认选中的难度级别。</span>
              </div>
              <el-select
                v-model="interviewPreferenceForm.defaultInterviewDifficulty"
                class="preference-select"
                @change="handleInterviewPreferenceSave"
              >
                <el-option
                  v-for="item in difficultyPreferenceOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>默认面试模式</strong>
                <span>进入面试入口页时默认选中的面试官模式。</span>
              </div>
              <el-select
                v-model="interviewPreferenceForm.defaultInterviewMode"
                class="preference-select"
                @change="handleInterviewPreferenceSave"
              >
                <el-option
                  v-for="item in interviewModeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>默认反馈模式</strong>
                <span>进入面试入口页时默认选中的反馈节奏。</span>
              </div>
              <el-select
                v-model="interviewPreferenceForm.defaultFeedbackMode"
                class="preference-select"
                @change="handleInterviewPreferenceSave"
              >
                <el-option
                  v-for="item in feedbackModeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>默认交互方式</strong>
                <span>进入模拟面试入口页时默认选择文字面试或语音面试；浏览器不支持语音能力时会保持文字面试。</span>
              </div>
              <el-select
                v-model="interviewPreferenceForm.defaultInterviewInteractionType"
                class="preference-select"
                @change="handleInterviewPreferenceSave"
              >
                <el-option
                  v-for="item in interactionModeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
          </div>

          <div class="voice-preference-block">
            <div class="voice-preference-heading">
              <h3>语音通话偏好</h3>
              <p>这些设置只影响当前浏览器里的语音面试收音、自动提交和 AI 播报。</p>
            </div>

            <div class="preference-list">
              <div class="preference-row stacked">
                <div>
                  <strong>AI 播报声音</strong>
                  <span>使用当前浏览器可用的系统语音，音色偏好只保存在本机。</span>
                </div>
                <div class="voice-control">
                  <el-select
                    v-model="interviewPreferenceForm.voicePreferredType"
                    class="preference-select"
                    @change="handleVoicePreferredTypeChange"
                  >
                    <el-option
                      v-for="item in voicePreferredTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                  <el-button
                    type="primary"
                    plain
                    class="voice-preview-button"
                    title="试听"
                    aria-label="试听当前 AI 播报声音"
                    :disabled="!previewTextToSpeech.isSupported.value"
                    @click="handleVoicePreview"
                  >
                  <FeatureIcon name="voice-interview" size="md" class="voice-preview-icon" />
                  </el-button>
                </div>
              </div>
              <div
                v-if="interviewPreferenceForm.voicePreferredType === 'custom'"
                class="preference-row stacked"
              >
                <div>
                  <strong>浏览器 voice 列表</strong>
                  <span>不同浏览器和系统安装的语音包不同，找不到已选音色时会回到默认中文自然音色。</span>
                </div>
                <el-select
                  v-model="selectedBrowserVoiceKey"
                  class="preference-select browser-voice-select"
                  filterable
                  fit-input-width
                  popper-class="browser-voice-select-popper"
                  :disabled="browserVoiceOptions.length === 0"
                  placeholder="当前浏览器暂无可用 voice"
                >
                  <el-option
                    v-for="item in browserVoiceOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>AI 播报语速</strong>
                  <span>默认略慢，便于听清面试官问题。</span>
                </div>
                <el-slider
                  v-model="interviewPreferenceForm.voiceSpeakingRate"
                  class="preference-slider"
                  :min="0.7"
                  :max="1.2"
                  :step="0.01"
                  :format-tooltip="formatSpeechRate"
                  @change="handleInterviewPreferenceSave"
                />
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>AI 播报音调</strong>
                  <span>控制浏览器语音合成的音调高低。</span>
                </div>
                <el-slider
                  v-model="interviewPreferenceForm.voicePitch"
                  class="preference-slider"
                  :min="0.8"
                  :max="1.3"
                  :step="0.01"
                  :format-tooltip="formatSpeechPitch"
                  @change="handleInterviewPreferenceSave"
                />
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>AI 播报音量</strong>
                  <span>只影响浏览器 TTS 播报音量，不改变系统音量。</span>
                </div>
                <el-slider
                  v-model="interviewPreferenceForm.voiceVolume"
                  class="preference-slider"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :format-tooltip="formatSpeechVolume"
                  @change="handleInterviewPreferenceSave"
                />
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>取消静音后恢复</strong>
                  <span>选择取消静音后是否立即继续收音。</span>
                </div>
                <el-select
                  v-model="interviewPreferenceForm.voiceMuteResumeMode"
                  class="preference-select"
                  @change="handleInterviewPreferenceSave"
                >
                  <el-option label="自动继续识别" value="auto" />
                  <el-option label="再次点击麦克风后继续" value="manual" />
                </el-select>
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>自动提交等待时间</strong>
                  <span>用户停止说话后等待多久自动发送本轮回答。</span>
                </div>
                <el-select
                  v-model="interviewPreferenceForm.voiceAutoSubmitDelayMs"
                  class="preference-select"
                  @change="handleInterviewPreferenceSave"
                >
                  <el-option label="不自动提交" :value="0" />
                  <el-option label="等待 2 秒" :value="2000" />
                  <el-option label="等待 3 秒" :value="3000" />
                  <el-option label="等待 5 秒" :value="5000" />
                </el-select>
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>语音识别语言</strong>
                  <span>自动模式会在外企面试官使用英文，其它面试模式使用中文普通话。</span>
                </div>
                <el-select
                  v-model="interviewPreferenceForm.voiceRecognitionLanguage"
                  class="preference-select"
                  @change="handleInterviewPreferenceSave"
                >
                  <el-option label="自动匹配面试模式" value="auto" />
                  <el-option label="中文普通话" value="zh-CN" />
                  <el-option label="英文" value="en-US" />
                </el-select>
              </div>
              <div class="preference-row stacked">
                <div>
                  <strong>重置语音偏好</strong>
                  <span>恢复 AI 播报声音、语速、音调、音量、静音恢复、自动提交和识别语言的默认值。</span>
                </div>
                <el-button plain class="preference-action" @click="handleVoicePreferenceReset">
                  重置偏好
                </el-button>
              </div>
            </div>
          </div>
        </section>

        <section v-show="activeSection === 'security'" class="settings-panel" aria-labelledby="security-title">
          <div class="panel-heading">
            <div>
              <h2 id="security-title">账号安全</h2>
              <p>修改登录密码、安全问题，并管理不可恢复的账号注销操作。</p>
            </div>
          </div>

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

        </section>

        <section v-show="activeSection === 'privacy'" class="settings-panel" aria-labelledby="privacy-title">
          <div class="panel-heading">
            <div>
              <h2 id="privacy-title">隐私与数据</h2>
              <p>查看账号数据概览，管理当前浏览器保存的本机设置缓存。</p>
            </div>
            <el-tooltip content="刷新数据" placement="top" :show-after="400">
              <el-button
                plain
                circle
                class="data-overview-refresh-btn"
                :class="{ 'is-refreshing': growthOverviewLoading }"
                :disabled="growthOverviewLoading"
                @click="fetchGrowthOverview"
              >
              <FeatureIcon name="growth-radar" size="md" class="settings-refresh-icon" />
              </el-button>
            </el-tooltip>
          </div>

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
              <el-button type="warning" plain @click="handleClearLocalCacheConfirm">
                清空本地缓存
              </el-button>
            </div>
            <div class="preference-row data-retention-row">
              <div>
                <strong>数据保留说明</strong>
                <span>账号数据由服务端按当前策略保留；面试记录和简历诊断记录可在数据管理中设置自动清理天数，手动清理仍需二次确认。</span>
              </div>
            </div>
          </div>
        </section>

        <section v-show="activeSection === 'dataManagement'" class="settings-panel" aria-labelledby="data-management-title">
          <div class="panel-heading">
            <div>
              <h2 id="data-management-title">数据管理</h2>
              <p>管理历史记录手动清理与自动清理偏好；自动清理只在保存后按服务端低峰任务执行。</p>
            </div>
          </div>

          <div class="preference-list">
            <div class="preference-row danger-row">
              <div>
                <strong>面试记录清理</strong>
                <span>批量清理当前账号下的历史面试会话、聊天记录和岗位定向上下文。</span>
              </div>
              <el-button type="danger" plain :loading="interviewHistoryClearing" @click="handleInterviewHistoryClearConfirm">
                清理记录
              </el-button>
            </div>
            <div class="preference-row danger-row">
              <div>
                <strong>简历诊断清理</strong>
                <span>批量清理当前账号下的简历诊断、JD 匹配、AI 润色记录和上传文件。</span>
              </div>
              <el-button type="danger" plain :loading="resumeHistoryClearing" @click="handleResumeHistoryClearConfirm">
                清理记录
              </el-button>
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>面试记录保留天数</strong>
                <span>{{ retentionPreferenceText }}</span>
              </div>
              <el-select
                v-model="interviewPreferenceForm.interviewRetentionDays"
                class="preference-select"
              >
                <el-option
                  v-for="item in retentionDayOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>简历诊断保留天数</strong>
                <span>{{ resumeRetentionPreferenceText }}</span>
              </div>
              <el-select
                v-model="interviewPreferenceForm.resumeRetentionDays"
                class="preference-select"
                :loading="userSettingsSaving"
              >
                <el-option
                  v-for="item in retentionDayOptions"
                  :key="`resume-${item.value}`"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
            <div class="preference-row data-management-save-row">
              <div>
                <strong>保存数据管理设置</strong>
                <span>保留天数只在点击保存后同步到服务端，避免修改面试偏好时触发后端写入。</span>
              </div>
              <el-button type="primary" :loading="userSettingsSaving" @click="handleDataManagementSettingsSave">
                保存设置
              </el-button>
            </div>
          </div>
        </section>

        <section v-show="activeSection === 'feedback'" class="settings-panel" aria-labelledby="feedback-title">
          <div class="panel-heading">
            <div>
              <h2 id="feedback-title">问题反馈</h2>
              <p>提交使用过程中遇到的问题或功能建议，管理员会在后台集中跟进。</p>
            </div>
          </div>

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
        </section>

        <section v-show="activeSection === 'appearance'" class="settings-panel" aria-labelledby="appearance-title">
          <div class="panel-heading">
            <div>
              <h2 id="appearance-title">外观偏好</h2>
              <p>选择当前浏览器使用的显示模式，偏好会自动保存在本机。</p>
            </div>
            <div class="appearance-status">
              <el-tag effect="plain">当前：{{ resolvedThemeText }}</el-tag>
              <span>已保存到当前浏览器</span>
            </div>
          </div>

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
        </section>

        <section v-show="activeSection === 'notification'" class="settings-panel" aria-labelledby="notification-title">
          <div class="panel-heading">
            <div>
              <h2 id="notification-title">通知偏好</h2>
              <p>仅影响当前浏览器的显示偏好。</p>
            </div>
          </div>

          <div class="preference-list">
            <div class="preference-row">
              <div>
                <strong>顶部实时通知提醒</strong>
                <span>关闭后不建立实时通知连接，也不显示顶部通知铃铛。</span>
              </div>
              <el-switch
                v-model="notificationForm.notificationRealtimeEnabled"
                aria-label="顶部实时通知提醒"
                @change="handleNotificationPreferenceSave"
              />
            </div>
            <div class="preference-row">
              <div>
                <strong>进入通知中心默认只看未读</strong>
                <span>打开通知中心时自动带入未读筛选。</span>
              </div>
              <el-switch
                v-model="notificationForm.notificationDefaultUnreadOnly"
                aria-label="进入通知中心默认只看未读"
                @change="handleNotificationPreferenceSave"
              />
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>通知中心默认类型</strong>
                <span>进入通知中心时自动选择对应类型。</span>
              </div>
              <el-select
                v-model="notificationForm.notificationDefaultType"
                class="notification-type-select"
                @change="handleNotificationPreferenceSave"
              >
                <el-option label="全部类型" value="" />
                <el-option label="简历诊断" value="resume" />
                <el-option label="AI 润色" value="polish" />
                <el-option label="模拟面试" value="interview" />
                <el-option label="额度提醒" value="quota" />
                <el-option label="系统通知" value="system" />
                <el-option label="活动公告" value="activity" />
                <el-option label="版本公告" value="update" />
                <el-option label="维护公告" value="maintenance" />
              </el-select>
            </div>
          </div>
        </section>

        <section v-show="activeSection === 'onboarding'" class="settings-panel" aria-labelledby="onboarding-title">
          <div class="panel-heading">
            <div>
              <h2 id="onboarding-title">新手引导</h2>
              <p>需要重新熟悉功能入口时，可以再次查看引导。</p>
            </div>
          </div>

          <el-button type="primary" plain @click="showOnboarding = true">
            重新查看新手引导
          </el-button>
        </section>

        <section v-show="activeSection === 'membership'" class="settings-panel" aria-labelledby="membership-title">
          <div class="panel-heading">
            <div>
              <h2 id="membership-title">会员与额度</h2>
              <p>查看当前身份、到期时间和可用额度。</p>
            </div>
            <el-button type="primary" plain @click="router.push('/membership')">
              查看会员中心
            </el-button>
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
        </section>
      </main>
    </div>

    <OnboardingGuide v-model:visible="showOnboarding" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAccount, getCurrentAccountSecurityQuestion, updatePassword, updateSecurityQuestion } from '@/api/auth'
import { createUserFeedback } from '@/api/feedback'
import { getGrowthOverview } from '@/api/growth'
import { clearInterviewHistory, getInterviewJobRoles } from '@/api/interview'
import { getMembershipPlans } from '@/api/membership'
import { clearResumeHistory } from '@/api/resume'
import { getUserSettings, saveUserSettings } from '@/api/userSettings'
import OnboardingGuide from '@/components/OnboardingGuide.vue'
import { useTextToSpeech } from '@/composables/useTextToSpeech'
import { FEEDBACK_MODE_OPTIONS, INTERACTION_MODE_OPTIONS, INTERVIEW_MODE_OPTIONS } from '@/constants/interview'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { removeToken } from '@/utils/auth'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import {
  clearLocalSettingsCache,
  DEFAULT_SETTINGS_PREFERENCES,
  getSettingsPreferences,
  saveSettingsPreferences
} from '@/utils/settingsPreferences'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

const activeSection = ref('profile')
const showOnboarding = ref(false)
const securityMode = ref('password')
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
  { key: 'security', label: '账号安全', icon: 'account-security' },
  { key: 'privacy', label: '隐私与数据', icon: 'data-cleanup' },
  { key: 'dataManagement', label: '数据管理', icon: 'data-management' },
  { key: 'feedback', label: '问题反馈', icon: 'feedback-center' },
  { key: 'appearance', label: '外观偏好', icon: 'settings' },
  { key: 'notification', label: '通知偏好', icon: 'notification-center' },
  { key: 'onboarding', label: '新手引导', icon: 'onboarding-task' },
  { key: 'membership', label: '会员与额度', icon: 'membership-credits' }
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
const voicePreferredTypeOptions = [
  { label: '默认中文自然音色', value: 'natural_zh' },
  { label: '女声优先', value: 'female' },
  { label: '男声优先', value: 'male' },
  { label: '系统默认', value: 'system' },
  { label: '指定浏览器音色', value: 'custom' }
]
const retentionDayOptions = [
  { label: '不自动清理', value: 0 },
  { label: '保留 30 天', value: 30 },
  { label: '保留 90 天', value: 90 },
  { label: '保留 180 天', value: 180 },
  { label: '保留 365 天', value: 365 }
]

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
const buildVoiceKey = (voice) => `${voice.voiceURI || ''}|||${voice.name || ''}|||${voice.lang || ''}`

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

const syncPreferenceForms = (preferences) => {
  const nextPreferences = { ...preferences }
  notificationForm.value = nextPreferences
  interviewPreferenceForm.value = { ...nextPreferences }
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
  if (interviewPreferenceForm.value.voicePreferredType !== 'custom') {
    interviewPreferenceForm.value.voiceName = ''
    interviewPreferenceForm.value.voiceURI = ''
    interviewPreferenceForm.value.voiceLang = ''
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

const handleVoicePreview = () => {
  previewTextToSpeech.rate.value = Number(interviewPreferenceForm.value.voiceSpeakingRate)
  previewTextToSpeech.pitch.value = Number(interviewPreferenceForm.value.voicePitch)
  previewTextToSpeech.volume.value = Number(interviewPreferenceForm.value.voiceVolume)
  previewTextToSpeech.setVoicePreference(buildVoicePreferenceFromForm())
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
  // 清理范围只覆盖本机设置缓存，不能触碰登录 token，避免“清缓存”变成隐式退出登录。
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
  tasks.push(
    getMembershipPlans().then((res) => {
      membershipPlans.value = Array.isArray(res?.data) ? res.data : []
    }).catch(() => {
      // 套餐列表失败时不回退显示内部编码，只保留用户可理解的兜底文案。
      membershipPlans.value = []
    })
  )
  await Promise.all(tasks)
})

onBeforeUnmount(() => {
  clearAccountDeleteTimer()
})
</script>

<style scoped>
.settings-view {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
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
  align-items: flex-start;
}

.settings-nav {
  position: sticky;
  top: 84px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px;
  border: 1px solid var(--border-card);
  border-radius: 12px;
  background: var(--bg-card);
}

.settings-nav-item {
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
}

.settings-nav-item:hover,
.settings-nav-item.active {
  background: var(--orange-light-bg);
  color: var(--orange-deep);
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
}

.settings-panel {
  padding: 24px;
  border: 1px solid var(--border-card);
  border-radius: 12px;
  background: var(--bg-card);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
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

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.info-item {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: var(--bg-page);
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

.security-mode-tabs {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--border-divider);
  overflow-x: auto;
}

.security-mode-tab {
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
  transition: color 0.2s, border-color 0.2s, background-color 0.2s;
}

.security-mode-tab:hover,
.security-mode-tab.active {
  color: var(--orange-main);
}

.security-mode-tab.danger {
  color: #b42318;
}

.security-mode-tab.active {
  border-bottom-color: var(--orange-main);
  font-weight: 600;
}

.security-mode-tab.danger.active {
  border-bottom-color: #f56c6c;
  color: #b42318;
}

.security-mode-tab:focus-visible {
  outline: 2px solid var(--orange-main);
  outline-offset: 2px;
}

.security-panel-enter-active,
.security-panel-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.security-panel-enter-from,
.security-panel-leave-to {
  opacity: 0;
  transform: translateY(6px);
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
  transition: border-color 0.2s, background-color 0.2s, box-shadow 0.2s;
}

.appearance-option:hover,
.appearance-option.active {
  border-color: var(--orange-main);
  background: var(--bg-card);
}

.appearance-option.active {
  box-shadow: 0 0 0 2px rgba(255, 140, 66, 0.14);
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
  border: 1px solid var(--border-card);
  border-radius: 10px;
  overflow: hidden;
}

.preference-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-card);
}

.preference-row:last-child {
  border-bottom: 0;
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

.preference-action {
  flex: 0 0 auto;
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

.voice-preview-button:hover:not(.is-disabled) {
  color: #fff;
  background: var(--orange-main);
  border-color: var(--orange-main);
  box-shadow: 0 8px 18px rgba(255, 140, 66, 0.22);
  transform: translateY(-1px);
}

.voice-preview-button:active:not(.is-disabled) {
  box-shadow: 0 4px 10px rgba(255, 140, 66, 0.18);
  transform: translateY(0) scale(0.96);
}

.voice-preview-button:focus-visible {
  outline: 2px solid var(--orange-main);
  outline-offset: 2px;
}

.voice-preview-button.is-disabled {
  box-shadow: none;
  transform: none;
}

.voice-preview-button:hover:not(.is-disabled) .voice-preview-icon {
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

.voice-preference-block {
  margin-top: 22px;
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

.data-overview-refresh-btn:hover .settings-refresh-icon {
  transform: rotate(60deg);
}

.data-overview-refresh-btn.is-refreshing .settings-refresh-icon {
  animation: data-overview-spin 0.7s cubic-bezier(0.34, 1.56, 0.64, 1) infinite;
}

@keyframes data-overview-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.data-overview-grid {
  margin-bottom: 18px;
}

.inline-warning {
  margin-bottom: 18px;
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

@media (max-width: 900px) {
  .settings-layout {
    grid-template-columns: 1fr;
  }

  .settings-nav {
    position: static;
    overflow-x: auto;
    flex-direction: row;
  }

  .settings-nav-item {
    flex: 0 0 auto;
    width: auto;
    white-space: nowrap;
  }

  .appearance-options {
    grid-template-columns: 1fr;
  }

  .account-delete-zone {
    max-width: none;
  }
}

@media (max-width: 640px) {
  .settings-panel {
    padding: 18px;
  }

  .panel-heading,
  .profile-summary,
  .profile-name-row,
  .preference-row,
  .preference-row.stacked {
    flex-direction: column;
    align-items: stretch;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .notification-type-select {
    width: 100%;
  }

  .preference-select,
  .preference-slider {
    width: 100%;
  }

  .voice-control {
    width: 100%;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 52px;
    align-items: center;
  }

  .voice-preview-button {
    width: 52px;
    min-width: 52px;
    height: 48px;
  }

  .browser-voice-select {
    width: 100%;
  }

  .appearance-status {
    align-items: flex-start;
  }

  .appearance-preview {
    height: 64px;
  }

  .account-delete-form .el-button--danger {
    width: 100%;
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
  .settings-refresh-icon {
    transition-duration: 0.01ms;
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
</style>
