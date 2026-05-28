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
import { ElMessage, ElMessageBox } from 'element-plus'
import { exportPdfFromHtml, downloadPdfFile, serializeElementToHtml, stripScopedSelectors } from '@/api/resumePdf'
import { checkTemplateQuota } from '@/api/template.js'
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
  const html2canvas = (await import('html2canvas')).default
  return await html2canvas(props.targetRef, {
    scale: 2,
    useCORS: true,
    backgroundColor: '#ffffff'
  })
}

async function canvasToPdf(canvas, fileName) {
  const { default: jsPDF } = await import('jspdf')
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
    await checkTemplateQuota()
    const canvas = await captureCanvas()
    if (!canvas) return
    await canvasToPdf(canvas, props.fileName)
    ElMessage.success('PDF 已导出（图片型）')
  } catch (err) {
    console.error('[PDF导出-图片型] 失败:', err)
    ElMessage.error('PDF 导出失败，请稍后重试')
  } finally {
    imagePdfExporting.value = false
  }
}

/**
 * 文字型 PDF 导出（两步式：生成 → 确认 → 下载）
 *
 * 步骤一：调用后端生成 PDF，拿到 fileId
 * 步骤二：弹窗询问用户"PDF 制作完成，是否下载到本地？"
 *   - 用户点击"是" → 调用下载接口，通过临时 <a> 标签触发浏览器保存
 *   - 用户点击"否" → 仅提示"已保存"，后续可在其他地方下载（预留扩展）
 *
 * 与旧实现的区别：
 * - 旧：后端直接返回 Blob 文件流，前端创建 Blob URL 下载
 * - 新：后端返回 JSON（含 fileId），前端二次请求下载接口
 *
 * 优势：
 * - 生成失败时前端能收到明确的错误 JSON，而非空白 Blob
 * - 用户可以在下载前确认，提升体验
 * - fileId 可用于后续的分享、重新下载等扩展功能
 */
async function exportTextPdf() {
  if (!props.targetRef) {
    ElMessage.warning('预览内容未就绪，请稍后重试')
    return
  }
  textPdfExporting.value = true

  try {
    // 1. 准备 HTML：合并模板 CSS + 简历导出样式的 scoped 选择器已被清除
    await checkTemplateQuota()
    const extraCss = props.templateCss ? stripScopedSelectors(props.templateCss) : ''
    const html = serializeElementToHtml(props.targetRef, resumeExportCss + '\n' + extraCss)

    // 2. 调用后端生成 PDF（返回 JSON，不再是 Blob）
    const result = await exportPdfFromHtml(html)
    // result 结构: { fileId: "uuid", fileName: "uuid.pdf", fileSize: 12345 }

    // 3. 弹窗确认：询问用户是否要下载到本地
    //    ElMessageBox.confirm 返回 Promise：
    //    - 点击"确认" → resolve → 执行下载
    //    - 点击"取消" → reject → 进入 catch 提示已保存
    await ElMessageBox.confirm(
      `PDF 制作完成！（文件大小：${(result.fileSize / 1024).toFixed(1)} KB）<br/>是否下载到本地？`,
      '导出成功',
      {
        dangerouslyUseHTMLString: true,             // 允许消息中嵌入 HTML（用于换行）
        confirmButtonText: '下载',
        cancelButtonText: '暂不下载',
        type: 'success',
      }
    )

    // 4. 用户确认后使用认证头下载，避免登录 token 出现在 URL、浏览器历史或代理日志中。
    await downloadPdfFile(result.fileId, result.fileName)
    ElMessage.success('PDF 已下载（文字型，可选中复制）')

  } catch (err) {
    // ElMessageBox 取消时会抛出 'cancel'，这不是错误，给个提示即可
    if (err === 'cancel' || err === 'close') {
      ElMessage.info('PDF 已生成并保存在服务器，需要时可重新导出')
      return
    }

    console.error('[PDF导出-文字型] 失败:', err)
    // 如果错误消息是字符串则直接展示，否则用通用提示
    ElMessage.error(typeof err === 'string' ? err : (err?.message || 'PDF 导出失败，请稍后重试'))
  } finally {
    textPdfExporting.value = false
  }
}

async function exportImage() {
  imageExporting.value = true
  try {
    await checkTemplateQuota()
    const canvas = await captureCanvas()
    if (!canvas) return

    canvas.toBlob((blob) => {
      if (!blob) {
        ElMessage.error('图片导出失败，请重试')
        return
      }
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
