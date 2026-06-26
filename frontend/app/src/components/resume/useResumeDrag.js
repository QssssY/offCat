/**
 * 简历模板拖拽逻辑
 * 管理正文块的跨章节拖拽和头部 meta 项排序
 */
import { ref } from 'vue'

export function useResumeDrag({
  sections,
  header,
  findBlockLocation,
  focusBlock,
  focusHeaderField,
  recordHistoryNow,
  activateBlock,
  activateHeaderField,
}) {
  function createEmptyDragState() {
    return { sectionId: '', blockId: '', position: '' }
  }

  const draggingBlockId = ref('')
  const metaDraggingId = ref('')
  const dragOverState = ref(createEmptyDragState())

  function resetDragState() {
    draggingBlockId.value = ''
    dragOverState.value = createEmptyDragState()
  }

  function resetMetaDragState() {
    metaDraggingId.value = ''
  }

  function handleBlockDragStart(blockId, event) {
    draggingBlockId.value = blockId
    activateBlock(blockId)
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/plain', blockId)
    }
  }

  function setDragOver(sectionId, blockId, position, event) {
    if (event?.dataTransfer) {
      event.dataTransfer.dropEffect = 'move'
    }
    dragOverState.value = { sectionId, blockId: blockId || '', position }
  }

  function clearDragOver() {
    dragOverState.value = createEmptyDragState()
  }

  function isDragOver(sectionId, blockId, position) {
    return (
      dragOverState.value.sectionId === sectionId &&
      dragOverState.value.blockId === (blockId || '') &&
      dragOverState.value.position === position
    )
  }

  async function handleBlockDrop(sectionId, blockId, position) {
    const draggedId = draggingBlockId.value
    if (!draggedId) return

    if (blockId && draggedId === blockId) {
      resetDragState()
      return
    }

    const fromLocation = findBlockLocation(draggedId)
    const toSectionIndex = sections.value.findIndex((section) => section.id === sectionId)
    if (!fromLocation || toSectionIndex === -1) {
      resetDragState()
      return
    }

    const draggedBlock = fromLocation.section.blocks.splice(fromLocation.blockIndex, 1)[0]
    const targetSection = sections.value[toSectionIndex]
    let insertIndex = targetSection.blocks.length

    if (blockId) {
      const targetIndex = targetSection.blocks.findIndex((item) => item.id === blockId)
      insertIndex = position === 'before' ? targetIndex : targetIndex + 1
    }

    if (fromLocation.section.id === sectionId && fromLocation.blockIndex < insertIndex) {
      insertIndex -= 1
    }

    targetSection.blocks.splice(insertIndex, 0, draggedBlock)
    resetDragState()
    await focusBlock(draggedBlock.id)
    recordHistoryNow()
  }

  function handleMetaDragStart(itemId, event) {
    metaDraggingId.value = itemId
    activateHeaderField(itemId, 'meta')
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/plain', itemId)
    }
  }

  function handleMetaDragOver(event) {
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move'
    }
  }

  async function handleMetaDrop(targetId) {
    const fromIndex = header.value.metaItems.findIndex((item) => item.id === metaDraggingId.value)
    const toIndex = header.value.metaItems.findIndex((item) => item.id === targetId)
    if (fromIndex === -1 || toIndex === -1 || fromIndex === toIndex) {
      resetMetaDragState()
      return
    }

    const [moved] = header.value.metaItems.splice(fromIndex, 1)
    header.value.metaItems.splice(toIndex, 0, moved)
    resetMetaDragState()
    await focusHeaderField(moved.id, 'meta')
    recordHistoryNow()
  }

  return {
    draggingBlockId,
    metaDraggingId,
    dragOverState,
    resetDragState,
    resetMetaDragState,
    handleBlockDragStart,
    setDragOver,
    clearDragOver,
    isDragOver,
    handleBlockDrop,
    handleMetaDragStart,
    handleMetaDragOver,
    handleMetaDrop,
  }
}
