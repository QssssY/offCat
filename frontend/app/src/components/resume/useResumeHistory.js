/**
 * 简历模板撤销/重做逻辑
 * 使用快照比对 + 节流写入，管理编辑操作的历史栈
 */
import { computed, ref } from 'vue'

export function useResumeHistory({ isPreview }) {
  const historyState = ref({ past: [], future: [] })
  const suspendHistory = ref(false)
  const historyTimer = ref(null)

  let _createSnapshot = null
  let _applySnapshot = null

  const canUndo = computed(() => historyState.value.past.length > 1)
  const canRedo = computed(() => historyState.value.future.length > 0)

  function setSnapshotFunctions(createFn, applyFn) {
    _createSnapshot = createFn
    _applySnapshot = applyFn
  }

  function initializeHistory() {
    if (!_createSnapshot) return
    const snapshot = _createSnapshot()
    historyState.value = {
      past: [{ signature: JSON.stringify(snapshot), snapshot }],
      future: [],
    }
  }

  function recordHistoryNow() {
    if (!isPreview.value || suspendHistory.value || !_createSnapshot) return

    if (historyTimer.value) {
      clearTimeout(historyTimer.value)
      historyTimer.value = null
    }

    const snapshot = _createSnapshot()
    const record = { signature: JSON.stringify(snapshot), snapshot }
    const lastRecord = historyState.value.past[historyState.value.past.length - 1]
    if (lastRecord?.signature === record.signature) return

    historyState.value.past.push(record)
    if (historyState.value.past.length > 80) {
      historyState.value.past.shift()
    }
    historyState.value.future = []
  }

  function queueHistorySnapshot() {
    if (!isPreview.value || suspendHistory.value) return

    if (historyTimer.value) {
      clearTimeout(historyTimer.value)
    }
    historyTimer.value = setTimeout(() => {
      historyTimer.value = null
      recordHistoryNow()
    }, 280)
  }

  async function undoTemplateChange() {
    if (!isPreview.value) return

    recordHistoryNow()
    if (historyState.value.past.length <= 1) return

    const current = historyState.value.past.pop()
    historyState.value.future.push(current)
    await _applySnapshot(historyState.value.past[historyState.value.past.length - 1].snapshot)
  }

  async function redoTemplateChange() {
    if (!isPreview.value || !historyState.value.future.length) return

    recordHistoryNow()
    const nextRecord = historyState.value.future.pop()
    historyState.value.past.push(nextRecord)
    await _applySnapshot(nextRecord.snapshot)
  }

  function cleanup() {
    if (historyTimer.value) {
      clearTimeout(historyTimer.value)
      historyTimer.value = null
    }
  }

  return {
    historyState,
    suspendHistory,
    canUndo,
    canRedo,
    setSnapshotFunctions,
    initializeHistory,
    recordHistoryNow,
    queueHistorySnapshot,
    undoTemplateChange,
    redoTemplateChange,
    cleanup,
  }
}
