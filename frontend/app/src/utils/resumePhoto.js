export const MAX_RESUME_PHOTO_FILE_SIZE_BYTES = 5 * 1024 * 1024
export const RESUME_PHOTO_SIZE_LIMIT_TEXT = '5MB'

/**
 * 简历编辑器的头像仅用于当前页面预览与导出，不参与模板持久化。
 * 这里统一约束上传体积，避免前端长时间占用过多内存。
 */
export function isResumePhotoFileTooLarge(fileSize) {
  return fileSize > MAX_RESUME_PHOTO_FILE_SIZE_BYTES
}
