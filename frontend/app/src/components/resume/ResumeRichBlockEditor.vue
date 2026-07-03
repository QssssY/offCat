<template>
  <div class="rich-block-editor" :style="editorInlineStyle">
    <EditorContent v-if="editor" :editor="editor" class="rich-block-content" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, watch } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import HardBreak from '@tiptap/extension-hard-break'
import { TextStyle } from '@tiptap/extension-text-style'
import { FontSize, FONT_SIZE_MAX, FONT_SIZE_MIN } from './extensions/fontSize'
import { sanitizeRichTextHtml } from './resumeSanitizer'

const props = defineProps({
  block: {
    type: Object,
    required: true,
  },
  mode: {
    type: String,
    default: 'preview',
  },
})

const emit = defineEmits([
  'update-html',
  'focus',
  'request-insert-after',
  'request-remove-empty',
])

const createEditor = () => {
  return new Editor({
    editable: props.mode === 'preview',
    content: sanitizeRichTextHtml(props.block.html) || '<p></p>',
    extensions: [
      StarterKit.configure({
        heading: false,
        blockquote: false,
        code: false,
        codeBlock: false,
        horizontalRule: false,
        orderedList: false,
        bulletList: false,
        listItem: false,
        strike: false,
        italic: false,
        hardBreak: false,
      }),
      TextStyle,
      FontSize,
      HardBreak.configure({
        keepMarks: true,
      }),
    ],
    editorProps: {
      transformPastedHTML(html) {
        return sanitizeRichTextHtml(html)
      },
      attributes: {
        class: 'rich-block-prosemirror',
      },
      handleKeyDown(view, event) {
        if (event.ctrlKey && event.key === 'Enter') {
          event.preventDefault()
          editor.commands.setHardBreak()
          return true
        }

        if (!event.ctrlKey && event.key === 'Enter') {
          event.preventDefault()
          emit('request-insert-after', props.block.id)
          return true
        }

        if ((event.key === 'Backspace' || event.key === 'Delete') && isEditorEmpty()) {
          event.preventDefault()
          emit('request-remove-empty', props.block.id)
          return true
        }

        return false
      },
    },
    onFocus() {
      emit('focus', props.block.id)
    },
    onUpdate({ editor }) {
      emit('update-html', {
        id: props.block.id,
        html: editor.getHTML(),
      })
    },
  })
}

const editor = createEditor()

const editorInlineStyle = computed(() => {
  return {
    fontSize: props.block.style?.fontSize ? `${props.block.style.fontSize}px` : undefined,
    fontWeight: props.block.style?.fontWeight || undefined,
  }
})

const isEditorEmpty = () => {
  return editor.isEmpty || !editor.getText().trim()
}

const hasTextSelection = () => {
  const selection = editor.state.selection
  return selection.from !== selection.to
}

/**
 * 工具栏优先修改当前选区；如果当前块没有选区，则交给父组件处理整段样式。
 */
const toggleBoldSelection = () => {
  if (!hasTextSelection()) {
    return false
  }
  editor.chain().focus().toggleBold().run()
  return true
}

/**
 * 字号调整只在选区存在时落到行内样式；没有选区时由父组件调整整块字号。
 */
const adjustSelectionFontSize = (delta) => {
  if (!hasTextSelection()) {
    return false
  }

  const currentSize = Number.parseFloat(editor.getAttributes('textStyle').fontSize || '14') || 14
  const nextSize = Math.min(FONT_SIZE_MAX, Math.max(FONT_SIZE_MIN, currentSize + delta))
  editor.chain().focus().setFontSize(String(nextSize)).run()
  return true
}

/**
 * 重置当前选区的加粗与字号；整段样式重置仍由父组件负责。
 */
const resetSelectionStyle = () => {
  if (!hasTextSelection()) {
    return false
  }

  editor.chain().focus().unsetBold().unsetFontSize().run()
  return true
}

const focusEditor = () => {
  editor.commands.focus('end')
}

const blurEditor = () => {
  editor.commands.blur()
}

watch(
  () => props.block.html,
  (nextHtml) => {
    if (!editor || editor.isDestroyed) {
      return
    }
    if (nextHtml === editor.getHTML()) {
      return
    }
    editor.commands.setContent(sanitizeRichTextHtml(nextHtml) || '<p></p>', false)
  },
)

watch(
  () => props.mode,
  (nextMode) => {
    editor.setEditable(nextMode === 'preview')
  },
)

onBeforeUnmount(() => {
  editor?.destroy()
})

defineExpose({
  focusEditor,
  blurEditor,
  toggleBoldSelection,
  adjustSelectionFontSize,
  resetSelectionStyle,
})
</script>

<style scoped>
.rich-block-editor {
  min-width: 0;
}

.rich-block-content :deep(.rich-block-prosemirror) {
  outline: none;
  min-height: 24px;
  line-height: inherit;
  color: inherit;
  white-space: normal;
  word-break: break-word;
}

.rich-block-content :deep(.rich-block-prosemirror p) {
  margin: 0;
}

.rich-block-content :deep(.rich-block-prosemirror p + p) {
  margin-top: 8px;
}

.rich-block-content :deep(.rich-block-prosemirror .ProseMirror-trailingBreak) {
  display: none;
}
</style>
