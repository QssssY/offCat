import { describe, expect, it } from 'vitest'
import { createResumePdfImagePages } from '@/utils/resumePdfPagination'

describe('resumePdfPagination', () => {
  it('should keep a short resume on one full-width page', () => {
    const pages = createResumePdfImagePages({
      canvasWidth: 2100,
      canvasHeight: 2500,
    })

    expect(pages).toHaveLength(1)
    expect(pages[0]).toMatchObject({
      x: 0,
      y: 0,
      width: 210,
      addPage: false,
    })
    expect(pages[0].height).toBeCloseTo(250, 6)
  })

  it('should shrink a near-one-page resume instead of cutting a small tail to page two', () => {
    const pages = createResumePdfImagePages({
      canvasWidth: 1900,
      canvasHeight: 2800,
    })

    expect(pages).toHaveLength(1)
    expect(pages[0]).toMatchObject({
      y: 0,
      height: 297,
      addPage: false,
    })
    expect(pages[0].x).toBeCloseTo(4.232142, 5)
    expect(pages[0].width).toBeCloseTo(201.535714, 5)
  })

  it('should keep a moderately over-one-page polished resume on one readable page', () => {
    const pages = createResumePdfImagePages({
      canvasWidth: 1900,
      canvasHeight: 3100,
    })

    expect(pages).toHaveLength(1)
    expect(pages[0]).toMatchObject({
      y: 0,
      height: 297,
      addPage: false,
    })
    expect(pages[0].x).toBeCloseTo(13.983871, 5)
    expect(pages[0].width).toBeCloseTo(182.032258, 5)
  })

  it('should paginate a long resume without shrinking width by height', () => {
    const pages = createResumePdfImagePages({
      canvasWidth: 2100,
      canvasHeight: 5000,
    })

    expect(pages).toHaveLength(2)
    expect(pages.every((page) => page.width === 210)).toBe(true)
    expect(pages[0]).toMatchObject({ x: 0, y: 0, addPage: false })
    expect(pages[1]).toMatchObject({ x: 0, y: -297, addPage: true })
    expect(pages[0].height).toBeCloseTo(500, 6)
    expect(pages[1].height).toBeCloseTo(500, 6)
  })

  it('should honor custom page margin without adding hidden extra whitespace', () => {
    const pages = createResumePdfImagePages({
      canvasWidth: 1900,
      canvasHeight: 3800,
      pageWidth: 210,
      pageHeight: 297,
      margin: 10,
    })

    expect(pages).toHaveLength(2)
    expect(pages[0]).toMatchObject({
      x: 10,
      y: 10,
      width: 190,
      height: 380,
    })
    expect(pages[1]).toMatchObject({
      x: 10,
      y: -267,
      width: 190,
      height: 380,
      addPage: true,
    })
  })
})
