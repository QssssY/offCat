import { describe, expect, it } from 'vitest'
import {
  MAX_RESUME_PHOTO_FILE_SIZE_BYTES,
  RESUME_PHOTO_SIZE_LIMIT_TEXT,
  isResumePhotoFileTooLarge,
} from '@/utils/resumePhoto'

describe('resumePhoto', () => {
  it('uses the configured resume photo upload limit', () => {
    expect(MAX_RESUME_PHOTO_FILE_SIZE_BYTES).toBe(5 * 1024 * 1024)
    expect(RESUME_PHOTO_SIZE_LIMIT_TEXT).toBe('5MB')
  })

  it('rejects files larger than the supported resume photo limit', () => {
    expect(isResumePhotoFileTooLarge(5 * 1024 * 1024)).toBe(false)
    expect(isResumePhotoFileTooLarge(5 * 1024 * 1024 + 1)).toBe(true)
  })
})
