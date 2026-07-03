import { ref, computed } from 'vue'

const MAX_HISTORY = 80
const QUEUE_DELAY_MS = 280

/**
 * 管理模板编辑器的撤销/重做历史栈。
 * @param {Object} options
 * @param {() => Object} options.buildSnapshot — 返回当前文档快照的纯数据对象
 * @param {(snapshot: Object) => Promise<void>} options.applySnapshot — 将快照恢复到编辑器
 * @param {import('vue').Ref<boolean>} options.isEditable — 是否处于可编辑模式
 */
export function useResumeHistory({ buildSnapshot, applySnapshot, isEditable }) {
  const historyState = ref({ past: [], future: [] })
  const suspendHistory = ref(false)
  const historyTimer = ref(null)

  const canUndo = computed(() => historyState.value.past.length > 1)
  const canRedo = computed(() => historyState.value.future.length > 0)

  function createRecord() {
    const snapshot = buildSnapshot()
    const prev = historyState.value.past[historyState.value.past.length - 1]?.snapshot
    if (prev && snapshot.photoGeneration === prev.photoGeneration) {
      snapshot.photoDataUrl = null
    }
    return {
      signature: JSON.stringify({ ...snapshot, photoDataUrl: undefined }),
      snapshot,
    }
  }

  function initialize() {
    historyState.value = { past: [createRecord()], future: [] }
  }

  function recordNow() {
    if (!isEditable.value || suspendHistory.value) return

    if (historyTimer.value) {
      clearTimeout(historyTimer.value)
      historyTimer.value = null
    }

    const record = createRecord()
    const last = historyState.value.past[historyState.value.past.length - 1]
    if (last?.signature === record.signature) return

    historyState.value.past.push(record)
    if (historyState.value.past.length > MAX_HISTORY) {
      historyState.value.past.shift()
    }
    historyState.value.future = []
  }

  function queueSnapshot() {
    if (!isEditable.value || suspendHistory.value) return

    if (historyTimer.value) {
      clearTimeout(historyTimer.value)
    }

    historyTimer.value = setTimeout(() => {
      historyTimer.value = null
      recordNow()
    }, QUEUE_DELAY_MS)
  }

  async function undo() {
    if (!isEditable.value) return

    recordNow()
    if (historyState.value.past.length <= 1) return

    const current = historyState.value.past.pop()
    historyState.value.future.push(current)
    await applySnapshot(historyState.value.past[historyState.value.past.length - 1].snapshot)
  }

  async function redo() {
    if (!isEditable.value || !historyState.value.future.length) return

    recordNow()
    const nextRecord = historyState.value.future.pop()
    historyState.value.past.push(nextRecord)
    await applySnapshot(nextRecord.snapshot)
  }

  function getSignature() {
    return createRecord().signature
  }

  return {
    historyState,
    suspendHistory,
    historyTimer,
    canUndo,
    canRedo,
    initialize,
    recordNow,
    queueSnapshot,
    undo,
    redo,
    getSignature,
  }
}
