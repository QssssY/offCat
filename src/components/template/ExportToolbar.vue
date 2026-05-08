<template>
  <div class="export-toolbar">
    <el-button @click="exportImagePdf" :loading="imagePdfExporting">
      导出 PDF（图片型）
    </el-button>
    <el-button @click="exportTextPdf" :loading="textPdfExporting">
      导出 PDF（文字型）
    </el-button>
    <el-button @click="exportImage" :loading="imageExporting">
      导出图片
    </el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import { ElMessage } from 'element-plus'
import { exportPdfFromHtml, serializeElementToHtml, stripScopedSelectors } from '@/api/resumePdf'
import resumeExportCss from '@/components/resume/resumeExportStyles'

const props = defineProps({
  targetRef: { type: Object, default: null },
  fileName: { type: String, default: '简历' },
  templateCss: { type: String, default: '' },
})

const imagePdfExporting = ref(false)
const textPdfExporting = ref(false)
const imageExporting = ref(false)

async function captureCanvas() {
  if (!props.targetRef) {
    ElMessage.warning('预览内容未就绪，请稍后重试')
    return null
  }
  return await html2canvas(props.targetRef, {
    scale: 2,
    useCORS: true,
    backgroundColor: '#ffffff'
  })
}

function canvasToPdf(canvas, fileName) {
  const imgData = canvas.toDataURL('image/png')
  const pdf = new jsPDF('p', 'mm', 'a4')
  const pdfWidth = pdf.internal.pageSize.getWidth()
  const pdfHeight = pdf.internal.pageSize.getHeight()
  const imgWidth = pdfWidth
  const imgHeight = (canvas.height * imgWidth) / canvas.width

  let heightLeft = imgHeight
  let position = 0

  pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
  heightLeft -= pdfHeight

  while (heightLeft > 0) {
    position = heightLeft - imgHeight
    pdf.addPage()
    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
    heightLeft -= pdfHeight
  }

  pdf.save(`${fileName}.pdf`)
}

// 图片型 PDF：html2canvas 截图 + jsPDF
async function exportImagePdf() {
  if (!props.targetRef) {
    ElMessage.warning('预览内容未就绪，请稍后重试')
    return
  }
  imagePdfExporting.value = true
  try {
    const canvas = await captureCanvas()
    if (!canvas) return
    canvasToPdf(canvas, props.fileName)
    ElMessage.success('PDF 已导出（图片型）')
  } catch (err) {
    console.error('[PDF导出-图片型] 失败:', err)
    ElMessage.error('PDF 导出失败，请稍后重试')
  } finally {
    imagePdfExporting.value = false
  }
}

// 文字型 PDF：序列化 HTML → 后端 Chrome headless 渲染
async function exportTextPdf() {
  if (!props.targetRef) {
    ElMessage.warning('预览内容未就绪，请稍后重试')
    return
  }
  textPdfExporting.value = true
  try {
    const extraCss = props.templateCss ? stripScopedSelectors(props.templateCss) : ''
    const html = serializeElementToHtml(props.targetRef, resumeExportCss + '\n' + extraCss)
    const blob = await exportPdfFromHtml(html)

    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${props.fileName}.pdf`
    a.click()
    URL.revokeObjectURL(url)

    ElMessage.success('PDF 已导出（文字型，可选中复制）')
  } catch (err) {
    console.error('[PDF导出-文字型] 失败:', err)
    ElMessage.error('PDF 导出失败，请稍后重试')
  } finally {
    textPdfExporting.value = false
  }
}

async function exportImage() {
  imageExporting.value = true
  try {
    const canvas = await captureCanvas()
    if (!canvas) return

    canvas.toBlob((blob) => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${props.fileName}.png`
      a.click()
      URL.revokeObjectURL(url)
    }, 'image/png')
  } finally {
    imageExporting.value = false
  }
}
</script>

<style scoped>
.export-toolbar {
  display: flex;
  gap: 8px;
}
</style>
