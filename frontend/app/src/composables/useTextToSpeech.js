import { computed, onUnmounted, ref } from 'vue'
import { getBrowserTtsPresetParameters } from '@/utils/settingsPreferences'

const SENTENCE_END_REGEXP = /[。！？.!?]/
const FEEDBACK_BLOCK_REGEXP = /<FEEDBACK>[\s\S]*?<\/FEEDBACK>/gi
const CHROME_KEEPALIVE_INTERVAL_MS = 10000
const STUCK_TIMEOUT_MS = 180000
const UTTERANCE_START_TIMEOUT_MS = 5000
const MIN_UTTERANCE_TIMEOUT_MS = 12000
const STARTED_UTTERANCE_MIN_TIMEOUT_MS = 60000
const MAX_UTTERANCE_TIMEOUT_MS = 180000
const UTTERANCE_TIMEOUT_PER_CHAR_MS = 450
const MAX_UTTERANCE_START_RETRIES = 1
const SPECIFIC_PRESET_MATCHERS = Object.freeze({
  gentle_female: Object.freeze([/xiaoxiao/, /yaoyao/]),
  pro_female: Object.freeze([/xiaoyi/, /jenny/]),
  lively_female: Object.freeze([/xiaoxuan/, /aria/]),
  warm_female: Object.freeze([/xiaobei/, /samantha/]),
  magnetic_male: Object.freeze([/yunxi/, /mark/]),
  pro_male: Object.freeze([/yunyang/, /david/]),
  calm_male: Object.freeze([/yunjian/, /daniel/]),
  energetic_male: Object.freeze([/kangkang/, /george/])
})
const SPECIFIC_PRESET_TYPES = new Set(Object.keys(SPECIFIC_PRESET_MATCHERS))
const QUALITY_CHINESE_PRESET_TYPES = new Set(['news_anchor', 'slow_clear'])

/**
 * 浏览器 TTS 语音合成封装。
 * 用于流式播报 AI 回复；keep-alive 定期 resume 防止 Chrome 15 秒自动暂停；
 * 全局卡死保护在 3 分钟无进展时强制 stop。
 */
export function useTextToSpeech(options = {}) {
  const isSupported = ref(typeof window !== 'undefined' && 'speechSynthesis' in window)
  const isSpeaking = ref(false)
  const isPaused = ref(false)
  const voices = ref([])
  const selectedVoice = ref(null)
  const engineStatus = computed(() => (isSupported.value ? 'system-tts' : 'unsupported'))
  const enhancedVoiceReady = ref(false)
  const activeUtteranceCount = ref(0)
  const initialPresetParameters = getBrowserTtsPresetParameters(options.voicePreference?.type || 'natural_zh')
  const rate = ref(Number(options.rate ?? initialPresetParameters?.rate ?? 0.92))
  const pitch = ref(Number(options.pitch ?? initialPresetParameters?.pitch ?? 1.06))
  const volume = ref(Number(options.volume ?? 1))
  const voicePreference = ref(options.voicePreference || { type: 'natural_zh' })
  const genderPreferenceTypes = new Set(['female', 'male'])

  let buffer = ''
  let pendingCount = 0
  let speechRunId = 0
  let voiceReadyResolvers = []
  let endedUtterances = new WeakSet()
  let startedUtterances = new WeakSet()
  let utteranceMetadata = new WeakMap()
  let utteranceWatchdogs = new WeakMap()
  let utteranceStartWatchdogs = new WeakMap()
  let utteranceWatchdogTimers = new Set()
  let activeUtterances = new Set()
  let speechQueue = []
  let isPreparingQueuedUtterance = false
  let keepAliveInterval = null
  let lastPendingChangeAt = 0
  let userGesturePrepared = false

  const speechSynthesisRef = computed(() => (
    typeof window !== 'undefined' ? window.speechSynthesis : null
  ))

  const normalizeTextForSpeech = (text) => {
    if (!text) return ''
    return text
      .replace(FEEDBACK_BLOCK_REGEXP, '')
      .replace(/```[\s\S]*?```/g, '')
      .replace(/[#*_>`|]/g, '')
      .replace(/\[(.*?)\]\(.*?\)/g, '$1')
      .replace(/\s+/g, ' ')
      .trim()
  }

  const isFemaleVoiceName = (name) => /xiaoxiao|xiaoyi|xiaobei|xiaoxuan|huihui|yaoyao|hanhan|tingting|meijia|female|woman|girl|zira|aria|jenny|susan|samantha|victoria/.test(name)
  const isMaleVoiceName = (name) => /yunxi|yunyang|yunjian|kangkang|male|man|boy|david|mark|george|daniel/.test(name)
  const isLegacySystemVoiceName = (name) => /desktop|huihui|zira|david|heami|hanhan|ichiro|haruka|hazel|hedda/.test(name)
  const isLegacySystemVoice = (voice) => isLegacySystemVoiceName((voice?.name || '').toLowerCase())
  const isChineseVoice = (voice) => (voice?.lang || '').toLowerCase().startsWith('zh')
  const isHighQualityChineseVoiceName = (name) => /xiaoxiao|xiaoyi|xiaobei|yunxi|xiaoxuan|natural|neural|premium/.test(name)
  const matchesSpecificPresetVoice = (voice, presetType) => {
    const matchers = SPECIFIC_PRESET_MATCHERS[presetType]
    if (!matchers) return false
    if (!isChineseVoice(voice)) return false
    const name = (voice?.name || '').toLowerCase()
    return matchers.some((matcher) => matcher.test(name))
  }
  const getSpecificPresetMatchScore = (voice, presetType) => {
    const matchers = SPECIFIC_PRESET_MATCHERS[presetType]
    if (!matchers) return 0
    if (!isChineseVoice(voice)) return 0
    const name = (voice?.name || '').toLowerCase()
    const matchedIndex = matchers.findIndex((matcher) => matcher.test(name))
    return matchedIndex === -1 ? 0 : 80 - matchedIndex
  }
  const getVoiceGender = (voice) => {
    const name = (voice?.name || '').toLowerCase()
    if (isMaleVoiceName(name)) return 'male'
    if (isFemaleVoiceName(name)) return 'female'
    return 'unknown'
  }

  const getVoiceScore = (voice, preferredType = 'natural_zh') => {
    const lang = voice.lang?.toLowerCase() || ''
    const name = voice.name?.toLowerCase() || ''
    let score = 0
    if (lang.startsWith('zh')) score += 20
    if (lang === 'zh-cn' || lang === 'zh-hans') score += 8
    if (isHighQualityChineseVoiceName(name)) score += 18
    if (/google/.test(name)) score += 4
    if (/microsoft/.test(name)) score += 4
    // Chrome 在部分环境会暴露远程 Google 中文 voice，但网络或服务不可用时 speak 会被接受却没有声音。
    // 默认中文播报优先保证能出声：本地中文 voice 的可靠性高于普通远程 voice，真正的 Natural/Neural 仍保留高分。
    if (voice.localService === true) score += isLegacySystemVoiceName(name) ? 8 : 14
    if (voice.localService === false) score += /natural|neural|premium/.test(name) ? 2 : -16
    // Chrome 常把 Windows 旧式本地语音也标为可用；这些 voice 稳定但机械，默认自然音色不应优先选择。
    if (isLegacySystemVoiceName(name)) score -= 8
    if (preferredType === 'female' && isFemaleVoiceName(name)) score += 16
    if (preferredType === 'male' && isMaleVoiceName(name)) score += 16
    score += getSpecificPresetMatchScore(voice, preferredType)
    return score
  }

  const matchCustomVoice = (availableVoices, preference) => {
    if (!preference?.voiceURI && !preference?.name) return null
    return availableVoices.find((voice) => (
      (preference.voiceURI && voice.voiceURI === preference.voiceURI) ||
      (
        preference.name &&
        voice.name === preference.name &&
        (!preference.lang || voice.lang === preference.lang)
      )
    )) || null
  }

  const pickPreferredVoice = (availableVoices, preference = voicePreference.value, options = {}) => {
    if (preference?.type === 'system') return null
    if (preference?.type === 'custom') {
      const customVoice = matchCustomVoice(availableVoices, preference)
      if (customVoice) return customVoice
    }
    const pickHighestScoredVoice = (voiceList) => [...voiceList]
      .sort((left, right) => getVoiceScore(right, preference?.type) - getVoiceScore(left, preference?.type))[0]
      || null
    if (SPECIFIC_PRESET_TYPES.has(preference?.type)) {
      // 具体预设只绑定明确匹配的系统 voice；找不到时不强行套用错误音色。
      const matchedPresetVoices = availableVoices.filter((voice) => matchesSpecificPresetVoice(voice, preference.type))
      return matchedPresetVoices.length > 0 ? pickHighestScoredVoice(matchedPresetVoices) : null
    }
    if (QUALITY_CHINESE_PRESET_TYPES.has(preference?.type)) {
      const chineseVoices = availableVoices.filter(isChineseVoice)
      return chineseVoices.length > 0 ? pickHighestScoredVoice(chineseVoices) : null
    }
    if (genderPreferenceTypes.has(preference?.type)) {
      const matchedGenderVoices = availableVoices.filter((voice) => getVoiceGender(voice) === preference.type)
      if (matchedGenderVoices.length > 0) return pickHighestScoredVoice(matchedGenderVoices)
      const neutralChineseVoices = availableVoices.filter((voice) => (
        isChineseVoice(voice) && getVoiceGender(voice) === 'unknown'
      ))
      if (neutralChineseVoices.length > 0) return pickHighestScoredVoice(neutralChineseVoices)
      // Chrome 只暴露相反性别 voice 时，不强行绑定错误性别；交回浏览器默认 voice，避免“男声/女声/默认”都被同一个显式 voice 覆盖。
      return null
    }
    if (!preference?.type || preference.type === 'natural_zh') {
      const chineseVoices = availableVoices.filter(isChineseVoice)
      const highQualityChineseVoices = chineseVoices.filter((voice) => (
        isHighQualityChineseVoiceName((voice?.name || '').toLowerCase())
      ))
      if (highQualityChineseVoices.length > 0) return pickHighestScoredVoice(highQualityChineseVoices)
      const localChineseVoices = chineseVoices.filter((voice) => voice.localService === true)
      if (options.allowGenderedDefaultFallback && localChineseVoices.length > 0) {
        return pickHighestScoredVoice(chineseVoices)
      }
      if (localChineseVoices.length > 0 && chineseVoices.length > 1) {
        return pickHighestScoredVoice(chineseVoices)
      }
      const neutralChineseVoices = chineseVoices.filter((voice) => getVoiceGender(voice) === 'unknown')
      if (neutralChineseVoices.length > 0) return pickHighestScoredVoice(neutralChineseVoices)
      if (chineseVoices.length === 1 && getVoiceGender(chineseVoices[0]) !== 'unknown') {
        // 只有一个明确性别的老式本地 voice 时，默认自然音色不主动指定它，保留用户原本的浏览器默认发声策略。
        return null
      }
    }
    return pickHighestScoredVoice(availableVoices)
  }

  const isPresetAvailable = (presetKey, availableVoices = voices.value) => {
    if (presetKey === 'system') return true
    if (presetKey === 'custom') return availableVoices.length > 0
    if (SPECIFIC_PRESET_TYPES.has(presetKey)) {
      return availableVoices.some((voice) => matchesSpecificPresetVoice(voice, presetKey))
    }
    if (presetKey === 'female') {
      return availableVoices.some((voice) => isChineseVoice(voice) && getVoiceGender(voice) === 'female')
    }
    if (presetKey === 'male') {
      return availableVoices.some((voice) => isChineseVoice(voice) && getVoiceGender(voice) === 'male')
    }
    if (presetKey === 'natural_zh' || QUALITY_CHINESE_PRESET_TYPES.has(presetKey)) {
      return availableVoices.some(isChineseVoice)
    }
    return false
  }

  const voicePreferenceStatus = computed(() => {
    const requestedType = voicePreference.value?.type || 'natural_zh'
    const selected = selectedVoice.value
    const selectedGender = getVoiceGender(selected)
    const isGenderPreference = genderPreferenceTypes.has(requestedType)
    const hasRequestedGenderVoice = isGenderPreference && voices.value.some((voice) => (
      isChineseVoice(voice) && getVoiceGender(voice) === requestedType
    ))
    return {
      requestedType,
      selectedVoiceName: selected?.name || '',
      selectedVoiceURI: selected?.voiceURI || '',
      selectedVoiceLang: selected?.lang || '',
      selectedGender,
      usesBrowserDefaultVoice: !selected,
      hasRequestedGenderVoice,
      isRequestedPresetAvailable: isPresetAvailable(requestedType),
      isDegraded: Boolean(isGenderPreference && selectedGender !== requestedType),
    }
  })

  const hasPreferredNonLegacyVoice = () => {
    const preferredVoice = pickPreferredVoice(voices.value)
    const preferredName = (preferredVoice?.name || '').toLowerCase()
    return Boolean(
      preferredVoice &&
      !isLegacySystemVoice(preferredVoice) &&
      isHighQualityChineseVoiceName(preferredName)
    )
  }

  const hasDeferredSingleGenderedDefaultVoice = () => {
    if (voicePreference.value?.type && voicePreference.value.type !== 'natural_zh') return false
    const chineseVoices = voices.value.filter(isChineseVoice)
    return chineseVoices.length === 1 && getVoiceGender(chineseVoices[0]) !== 'unknown'
  }

  const refreshVoices = (options = {}) => {
    if (!isSupported.value || !speechSynthesisRef.value) return
    voices.value = speechSynthesisRef.value.getVoices()
    selectedVoice.value = pickPreferredVoice(voices.value, voicePreference.value, options)
  }

  const resolveVoiceReadyWaiters = () => {
    const pendingResolvers = []
    voiceReadyResolvers.forEach((waiter) => {
      if (waiter.isReady()) {
        clearTimeout(waiter.timer)
        waiter.resolve()
        return
      }
      pendingResolvers.push(waiter)
    })
    voiceReadyResolvers = pendingResolvers
  }

  const handleVoicesChanged = () => {
    refreshVoices()
    resolveVoiceReadyWaiters()
  }

  const waitForVoicesReady = (isReady = () => voices.value.length > 0) => {
    refreshVoices()
    if (!isSupported.value || !speechSynthesisRef.value || isReady()) {
      return Promise.resolve(true)
    }
    return new Promise((resolve) => {
      const waiter = {
        isReady,
        timer: null,
        resolve: () => {
          voiceReadyResolvers = voiceReadyResolvers.filter((item) => item !== waiter)
          resolve(true)
        },
      }
      waiter.timer = setTimeout(() => {
        voiceReadyResolvers = voiceReadyResolvers.filter((item) => item !== waiter)
        resolve(false)
      }, 800)
      voiceReadyResolvers.push(waiter)
    })
  }

  const stopKeepAlive = () => {
    if (keepAliveInterval) {
      clearInterval(keepAliveInterval)
      keepAliveInterval = null
    }
  }

  // keep-alive：每 10 秒 resume 一次防止 Chrome 15 秒自动暂停；同时检测卡死。
  const startKeepAlive = () => {
    stopKeepAlive()
    lastPendingChangeAt = Date.now()
    keepAliveInterval = setInterval(() => {
      if (isSpeaking.value && speechSynthesisRef.value) {
        speechSynthesisRef.value.resume()
        // 全局卡死保护：3 分钟内没有任何 utterance 完成，强制停止
        if (Date.now() - lastPendingChangeAt > STUCK_TIMEOUT_MS) {
          stop()
        }
      } else {
        stopKeepAlive()
      }
    }, CHROME_KEEPALIVE_INTERVAL_MS)
  }

  const clearUtteranceStartWatchdog = (utterance) => {
    const startWatchdog = utteranceStartWatchdogs.get(utterance)
    if (!startWatchdog) return
    clearTimeout(startWatchdog)
    utteranceStartWatchdogs.delete(utterance)
    utteranceWatchdogTimers.delete(startWatchdog)
  }

  const getUtteranceEvent = (utterance, reason) => {
    const metadata = utteranceMetadata.get(utterance) || {}
    return {
      reason,
      started: startedUtterances.has(utterance),
      text: metadata.text || utterance.text || '',
      utterance,
    }
  }

  const clearUtteranceRuntimeState = (utterance) => {
    activeUtterances.delete(utterance)
    activeUtteranceCount.value = activeUtterances.size
    clearUtteranceStartWatchdog(utterance)
    const watchdog = utteranceWatchdogs.get(utterance)
    if (watchdog) {
      clearTimeout(watchdog)
      utteranceWatchdogs.delete(utterance)
      utteranceWatchdogTimers.delete(watchdog)
    }
  }

  const markUtteranceStart = (utterance) => {
    const metadata = utteranceMetadata.get(utterance)
    if (!metadata || metadata.runId !== speechRunId) return
    if (startedUtterances.has(utterance)) return
    startedUtterances.add(utterance)
    metadata.startedAt = Date.now()
    clearUtteranceStartWatchdog(utterance)
    metadata.onStart?.(getUtteranceEvent(utterance, 'start'))
  }

  const shouldWaitForVoiceBeforeSpeech = (speechOptions = {}) => {
    refreshVoices()
    const shouldWaitForHigherQualityVoice = Boolean(
      (userGesturePrepared || speechOptions.allowDefaultVoice) &&
      voicePreference.value?.type !== 'system' &&
      (
        (selectedVoice.value && isLegacySystemVoice(selectedVoice.value)) ||
        hasDeferredSingleGenderedDefaultVoice()
      )
    )
    if (voices.value.length > 0 && !shouldWaitForHigherQualityVoice) {
      userGesturePrepared = false
      return false
    }
    userGesturePrepared = false
    return shouldWaitForHigherQualityVoice ? hasPreferredNonLegacyVoice : undefined
  }

  const playNextQueuedUtterance = () => {
    if (isPreparingQueuedUtterance || activeUtterances.size > 0 || !speechQueue.length) return
    const nextItem = speechQueue[0]
    if (nextItem.runId !== speechRunId) {
      speechQueue.shift()
      pendingCount = Math.max(0, pendingCount - 1)
      playNextQueuedUtterance()
      return
    }

    const voiceReadyPredicate = shouldWaitForVoiceBeforeSpeech(nextItem.speechOptions)
    if (voiceReadyPredicate !== false) {
      isPreparingQueuedUtterance = true
      // Chrome 可能先返回 Huihui Desktop 等旧式机械音色，再异步补齐 Google/自然音色；点击手势内短暂等待更优 voice。
      void waitForVoicesReady(voiceReadyPredicate).then((ready) => {
        isPreparingQueuedUtterance = false
        const voiceOverride = ready
          ? null
          : pickPreferredVoice(voices.value, voicePreference.value, { allowGenderedDefaultFallback: true })
        if (!ready) selectedVoice.value = voiceOverride
        if (nextItem.runId !== speechRunId || activeUtterances.size > 0) return
        speechQueue.shift()
        // 浏览器 speechSynthesis 自身的多 utterance 队列在 Chrome 中不稳定；这里改为应用层串行，只在上一句结束后再交给浏览器下一句。
        enqueueNow(nextItem.text, { ...nextItem.speechOptions, voiceOverride })
      })
      return
    }

    speechQueue.shift()
    // 浏览器 speechSynthesis 自身的多 utterance 队列在 Chrome 中不稳定；这里改为应用层串行，只在上一句结束后再交给浏览器下一句。
    enqueueNow(nextItem.text, nextItem.speechOptions)
  }

  // pendingCount 表示应用层待播队列 + 当前浏览器 utterance 的总数，统一结束入口扣减；WeakSet 防止重复释放。
  const markUtteranceEnd = (utterance, reason = 'end') => {
    const metadata = utteranceMetadata.get(utterance)
    if (!metadata || metadata.runId !== speechRunId) return
    if (endedUtterances.has(utterance)) return
    const endEvent = getUtteranceEvent(utterance, reason)
    endedUtterances.add(utterance)
    // Chrome 对 SpeechSynthesisUtterance 的队列引用不稳定，播放期间必须由业务层持有强引用，结束后再释放。
    clearUtteranceRuntimeState(utterance)
    pendingCount = Math.max(0, pendingCount - 1)
    lastPendingChangeAt = Date.now()
    metadata.onEnd?.(endEvent)
    utteranceMetadata.delete(utterance)
    if (speechQueue.length > 0) {
      playNextQueuedUtterance()
    }
    if (pendingCount === 0) {
      stopKeepAlive()
      isSpeaking.value = false
      options.onEnd?.(endEvent)
    }
  }

  const getUtteranceTimeout = (text) => Math.min(
    MAX_UTTERANCE_TIMEOUT_MS,
    Math.max(MIN_UTTERANCE_TIMEOUT_MS, text.length * UTTERANCE_TIMEOUT_PER_CHAR_MS)
  )

  const getStartedUtteranceTimeout = (text) => Math.min(
    MAX_UTTERANCE_TIMEOUT_MS,
    Math.max(STARTED_UTTERANCE_MIN_TIMEOUT_MS, getUtteranceTimeout(text) * 3)
  )

  const retryUtteranceStart = (utterance) => {
    const metadata = utteranceMetadata.get(utterance)
    if (!metadata || metadata.runId !== speechRunId) return false
    if ((metadata.startRetryCount || 0) >= MAX_UTTERANCE_START_RETRIES) return false

    endedUtterances.add(utterance)
    clearUtteranceRuntimeState(utterance)
    utteranceMetadata.delete(utterance)
    // Chrome/Edge 偶发接受 speak() 但不触发 onstart；先清空浏览器队列，再用系统默认 voice 重试同一句。
    speechSynthesisRef.value?.cancel()
    enqueueNow(metadata.text, {
      ...metadata.speechOptions,
      allowDefaultVoice: true,
      requireStartEvent: true,
      startRetryCount: (metadata.startRetryCount || 0) + 1,
      useBrowserDefaultVoice: true,
      voiceOverride: null,
    })
    return true
  }

  const startUtteranceStartWatchdog = (utterance) => {
    const timer = setTimeout(() => {
      utteranceWatchdogTimers.delete(timer)
      utteranceStartWatchdogs.delete(utterance)
      if (endedUtterances.has(utterance) || startedUtterances.has(utterance)) return
      if (retryUtteranceStart(utterance)) return
      // Chrome 可能接受 speak 调用但既不真正开始播报也不触发回调，此时主动释放 AI 回复状态。
      speechSynthesisRef.value?.cancel()
      markUtteranceEnd(utterance, 'start-timeout')
    }, UTTERANCE_START_TIMEOUT_MS)
    utteranceStartWatchdogs.set(utterance, timer)
    utteranceWatchdogTimers.add(timer)
  }

  const startUtteranceWatchdog = (utterance, text, delay = getUtteranceTimeout(text)) => {
    const timer = setTimeout(() => {
      utteranceWatchdogTimers.delete(timer)
      utteranceWatchdogs.delete(utterance)
      if (endedUtterances.has(utterance)) return
      const metadata = utteranceMetadata.get(utterance)
      const browserStillSpeaking = Boolean(speechSynthesisRef.value?.speaking || speechSynthesisRef.value?.pending)
      if (metadata && startedUtterances.has(utterance) && browserStillSpeaking) {
        const hardTimeout = getStartedUtteranceTimeout(text)
        const elapsedAfterStart = Date.now() - (metadata.startedAt ?? metadata.createdAt ?? Date.now())
        if (elapsedAfterStart < hardTimeout) {
          // 已触发 onstart 的 Chrome 播报不能按短句估算直接 cancel，否则慢音色会被误判为卡死而中途截断。
          startUtteranceWatchdog(utterance, text, Math.min(getUtteranceTimeout(text), hardTimeout - elapsedAfterStart))
          return
        }
      }
      // 部分浏览器偶发不触发 onend/onerror，主动 cancel 并释放播报状态，避免面试卡住。
      speechSynthesisRef.value?.cancel()
      markUtteranceEnd(utterance, 'timeout')
    }, delay)
    utteranceWatchdogs.set(utterance, timer)
    utteranceWatchdogTimers.add(timer)
  }

  const enqueueNow = (text, speechOptions = {}) => {
    const normalizedText = normalizeTextForSpeech(text)
    if (!normalizedText || !isSupported.value || !speechSynthesisRef.value) return

    refreshVoices()
    const shouldWatchStart = activeUtterances.size === 0 && (
      speechOptions.requireStartEvent || (!speechSynthesisRef.value?.speaking && !speechSynthesisRef.value?.pending)
    )
    const utterance = new SpeechSynthesisUtterance(normalizedText)
    utteranceMetadata.set(utterance, {
      text: normalizedText,
      runId: speechRunId,
      createdAt: Date.now(),
      onStart: speechOptions.onStart,
      onEnd: speechOptions.onEnd,
      speechOptions,
      startRetryCount: Number(speechOptions.startRetryCount || 0),
    })
    const utteranceVoice = speechOptions.useBrowserDefaultVoice
      ? null
      : (speechOptions.voiceOverride || selectedVoice.value)
    utterance.lang = utteranceVoice?.lang || 'zh-CN'
    utterance.rate = rate.value
    utterance.pitch = pitch.value
    utterance.volume = volume.value
    if (utteranceVoice) utterance.voice = utteranceVoice
    utterance.onstart = () => markUtteranceStart(utterance)
    utterance.onend = () => markUtteranceEnd(utterance)
    utterance.onerror = (event) => markUtteranceEnd(utterance, event?.error || 'error')

    activeUtterances.add(utterance)
    activeUtteranceCount.value = activeUtterances.size
    lastPendingChangeAt = Date.now()
    isSpeaking.value = true
    speechSynthesisRef.value.resume?.()
    speechSynthesisRef.value.speak(utterance)
    if (shouldWatchStart) startUtteranceStartWatchdog(utterance)
    startUtteranceWatchdog(utterance, normalizedText)
  }

  const enqueue = (text, runId = speechRunId, speechOptions = {}) => {
    const normalizedText = normalizeTextForSpeech(text)
    if (!normalizedText || !isSupported.value || !speechSynthesisRef.value) return
    if (pendingCount === 0 && activeUtterances.size === 0 && speechQueue.length === 0) {
      // Chrome 的 speechSynthesis 是页面级单例；上一轮卡死后即使本 composable 已空闲，
      // 浏览器内部队列仍可能残留无声任务。新一轮首句入队前先清队列，保证面试播报和设置页试听都能重新发声。
      speechSynthesisRef.value.cancel?.()
    }
    const shouldAllowDefaultVoiceAfterWait = userGesturePrepared || speechOptions.allowDefaultVoice
    speechQueue.push({
      text: normalizedText,
      runId,
      speechOptions: {
        ...speechOptions,
        allowDefaultVoice: speechOptions.allowDefaultVoice || shouldAllowDefaultVoiceAfterWait,
      },
    })
    pendingCount += 1
    lastPendingChangeAt = Date.now()
    isSpeaking.value = true
    // Chrome 在持续合成约 15 秒后会自动暂停，keep-alive 定期 resume 防止此问题。
    if (pendingCount === 1) startKeepAlive()
    playNextQueuedUtterance()
  }

  const hasActiveSpeech = () => Boolean(
    buffer.trim()
    || pendingCount > 0
    || isSpeaking.value
    || speechSynthesisRef.value?.speaking
    || speechSynthesisRef.value?.pending
  )

  const clearSpeechState = (shouldCancelBrowserSpeech = true) => {
    speechRunId += 1
    buffer = ''
    pendingCount = 0
    stopKeepAlive()
    utteranceWatchdogTimers.forEach((timer) => clearTimeout(timer))
    utteranceWatchdogTimers = new Set()
    utteranceWatchdogs = new WeakMap()
    utteranceStartWatchdogs = new WeakMap()
    utteranceMetadata = new WeakMap()
    speechQueue = []
    isPreparingQueuedUtterance = false
    activeUtterances = new Set()
    activeUtteranceCount.value = 0
    endedUtterances = new WeakSet()
    startedUtterances = new WeakSet()
    isSpeaking.value = false
    isPaused.value = false
    if (shouldCancelBrowserSpeech) {
      speechSynthesisRef.value?.cancel()
    }
  }

  const speak = (text, speechOptions = {}) => {
    clearSpeechState(false)
    enqueue(text, speechRunId, speechOptions)
  }

  const prepareForUserGesture = () => {
    if (!isSupported.value || !speechSynthesisRef.value) return
    // Chrome 首次点击后 voices 可能仍为空或只有旧式机械音色；先唤醒合成器，后续播报会短暂等待更优 voice。
    userGesturePrepared = true
    refreshVoices()
    speechSynthesisRef.value.resume?.()
  }

  const speakStreaming = (chunk, speechOptions = {}) => {
    if (!chunk) return
    buffer += String(chunk).replace(FEEDBACK_BLOCK_REGEXP, '')
    if (!buffer.trim()) return
    while (true) {
      const endIndex = Array.from(buffer).findIndex((char) => SENTENCE_END_REGEXP.test(char))
      if (endIndex === -1) break
      const sentence = buffer.slice(0, endIndex + 1).trim()
      buffer = buffer.slice(endIndex + 1)
      // 流式播报同样透传 Chrome 启动检测参数，避免后续追问被 speak 接收但没有真正出声。
      enqueue(sentence, speechRunId, speechOptions)
    }
  }

  const flushRemaining = (speechOptions = {}) => {
    const remaining = buffer.trim()
    buffer = ''
    if (remaining) {
      // 无标点结尾的最后一段也必须沿用同一兜底参数，否则 done 后 flush 的内容仍可能无声。
      enqueue(remaining, speechRunId, speechOptions)
    } else if (pendingCount === 0) {
      options.onEnd?.()
    }
  }

  const stop = () => {
    clearSpeechState(true)
  }

  const pause = () => {
    if (!isSupported.value || !speechSynthesisRef.value) return
    speechSynthesisRef.value.pause()
    isPaused.value = true
  }

  const resume = () => {
    if (!isSupported.value || !speechSynthesisRef.value) return
    speechSynthesisRef.value.resume()
    isPaused.value = false
  }

  const setVoice = (voice) => {
    selectedVoice.value = voice
  }

  const setVoicePreference = (preference) => {
    voicePreference.value = preference || { type: 'natural_zh' }
    selectedVoice.value = pickPreferredVoice(voices.value)
  }

  refreshVoices()
  if (isSupported.value && speechSynthesisRef.value) {
    speechSynthesisRef.value.onvoiceschanged = handleVoicesChanged
  }

  onUnmounted(() => {
    stop()
    if (speechSynthesisRef.value) {
      speechSynthesisRef.value.onvoiceschanged = null
    }
  })

  return {
    isSupported,
    isSpeaking,
    isPaused,
    engineStatus,
    enhancedVoiceReady,
    voicePreferenceStatus,
    activeUtteranceCount,
    voices,
    voice: selectedVoice,
    rate,
    pitch,
    volume,
    setVoice,
    setVoicePreference,
    getPresetParameters: getBrowserTtsPresetParameters,
    isPresetAvailable,
    speak,
    speakStreaming,
    flushRemaining,
    stop,
    pause,
    resume,
    prepareForUserGesture,
    normalizeTextForSpeech,
  }
}
