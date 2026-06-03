export const SPEECH_RECOGNITION_CAPABILITY_STATUS = Object.freeze({
  WEBSPEECH_READY: 'webspeech-ready',
  TEMPORARILY_UNAVAILABLE: 'temporarily-unavailable',
  PERMISSION_BLOCKED: 'permission-blocked',
  UNSUPPORTED: 'unsupported',
})

export const getSpeechRecognitionConstructor = () => {
  if (typeof window === 'undefined') return null
  return window.SpeechRecognition || window.webkitSpeechRecognition || null
}

const queryMicrophonePermission = async () => {
  if (typeof navigator === 'undefined' || typeof navigator.permissions?.query !== 'function') {
    return ''
  }

  try {
    const permission = await navigator.permissions.query({ name: 'microphone' })
    return permission?.state || ''
  } catch {
    return ''
  }
}

/**
 * 检测当前浏览器语音识别能力。
 * 项目只使用普通 Web Speech 主链路，不再接入浏览器实验性的本地语言包安装能力。
 */
export async function detectSpeechRecognitionCapability() {
  const SpeechRecognition = getSpeechRecognitionConstructor()

  if (!SpeechRecognition) {
    return {
      status: SPEECH_RECOGNITION_CAPABILITY_STATUS.UNSUPPORTED,
      SpeechRecognition: null,
      permissionState: '',
    }
  }

  const permissionState = await queryMicrophonePermission()
  if (permissionState === 'denied') {
    return {
      status: SPEECH_RECOGNITION_CAPABILITY_STATUS.PERMISSION_BLOCKED,
      SpeechRecognition,
      permissionState,
    }
  }

  return {
    status: SPEECH_RECOGNITION_CAPABILITY_STATUS.WEBSPEECH_READY,
    SpeechRecognition,
    permissionState,
  }
}
