<template>
  <div
    :class="[
      'inline-rich-editor',
      {
        'inline-rich-editor--multiline': multiline,
        'is-focused': isFocused,
        'is-empty': isEmpty,
      },
    ]"
    :style="editorInlineStyle"
  >
    <EditorContent v-if="editor" :editor="editor" class="inline-rich-content" />
    <span v-if="showPlaceholder" class="inline-rich-placeholder">{{ placeholder }}</span>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import HardBreak from '@tiptap/extension-hard-break'
import { TextStyle } from '@tiptap/extension-text-style'
import { FontSize, FONT_SIZE_MAX, FONT_SIZE_MIN } from './extensions/fontSize'
import { sanitizeRichTextHtml } from './resumeSanitizer'

const props = defineProps({
  field: {
    type: Object,
    required: true,
  },
  mode: {
    type: String,
    default: 'preview',
  },
  multiline: {
    type: Boolean,
    default: false,
  },
  placeholder: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update-html', 'focus', 'request-remove-empty'])

const isEditable = computed(() => props.mode === 'preview')
const isFocused = ref(false)
const isEmpty = ref(true)

const syncEmptyState = (instance) => {
  isEmpty.value = instance.isEmpty || !instance.getText().trim()
}

const createEditor = () => {
  return new Editor({
    editable: isEditable.value,
    content: sanitizeRichTextHtml(props.field.html) || '<p></p>',
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
        class: 'inline-rich-prosemirror',
      },
      handleKeyDown(view, event) {
        if (event.ctrlKey && event.key === 'Enter') {
          event.preventDefault()
          editor.commands.setHardBreak()
          return true
        }

        if (!props.multiline && !event.ctrlKey && event.key === 'Enter') {
          event.preventDefault()
          return true
        }

        if ((event.key === 'Backspace' || event.key === 'Delete') && isEditorEmpty()) {
          emit('request-remove-empty', props.field.id)
        }

        return false
      },
    },
    onFocus() {
      isFocused.value = true
      emit('focus', props.field.id)
    },
    onBlur() {
      isFocused.value = false
    },
    onCreate({ editor }) {
      syncEmptyState(editor)
    },
    onUpdate({ editor }) {
      syncEmptyState(editor)
      emit('update-html', {
        id: props.field.id,
        html: editor.getHTML(),
      })
    },
  })
}

const editor = createEditor()

const editorInlineStyle = computed(() => {
  return {
    fontSize: props.field.style?.fontSize ? `${props.field.style.fontSize}px` : undefined,
    fontWeight: props.field.style?.fontWeight || undefined,
  }
})

const showPlaceholder = computed(() => {
  return isEditable.value && !isFocused.value && isEmpty.value && !!props.placeholder
})

const isEditorEmpty = () => {
  return editor.isEmpty || !editor.getText().trim()
}

const hasTextSelection = () => {
  const selection = editor.state.selection
  return selection.from !== selection.to
}

/**
 * 头部字段沿用正文编辑器的行内样式逻辑：
 * 有选区时优先改选区，没有选区时再交给父组件改整字段样式。
 */
const toggleBoldSelection = () => {
  if (!hasTextSelection()) {
    return false
  }
  editor.chain().focus().toggleBold().run()
  return true
}

const adjustSelectionFontSize = (delta) => {
  if (!hasTextSelection()) {
    return false
  }

  const currentSize = Number.parseFloat(editor.getAttributes('textStyle').fontSize || '14') || 14
  const nextSize = Math.min(FONT_SIZE_MAX, Math.max(FONT_SIZE_MIN, currentSize + delta))
  editor.chain().focus().setFontSize(String(nextSize)).run()
  return true
}

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
  () => props.field.html,
  (nextHtml) => {
    if (!editor || editor.isDestroyed) {
      return
    }
    if (nextHtml === editor.getHTML()) {
      return
    }
    editor.commands.setContent(sanitizeRichTextHtml(nextHtml) || '<p></p>', false)
    syncEmptyState(editor)
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
.inline-rich-editor {
  position: relative;
  min-width: 0;
}

.inline-rich-content :deep(.inline-rich-prosemirror) {
  outline: none;
  min-height: 1em;
  line-height: inherit;
  color: inherit;
  white-space: normal;
  word-break: break-word;
}

.inline-rich-content :deep(.inline-rich-prosemirror p) {
  margin: 0;
}

.inline-rich-content :deep(.inline-rich-prosemirror p + p) {
  margin-top: 6px;
}

.inline-rich-content :deep(.inline-rich-prosemirror .ProseMirror-trailingBreak) {
  display: none;
}

.inline-rich-editor--multiline .inline-rich-content :deep(.inline-rich-prosemirror) {
  min-height: 44px;
}

.inline-rich-placeholder {
  position: absolute;
  left: 0;
  top: 0;
  color: rgba(82, 96, 109, 0.72);
  pointer-events: none;
  user-select: none;
}
</style>
