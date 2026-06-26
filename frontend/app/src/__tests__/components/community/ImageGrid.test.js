import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import ImageGrid from '@/components/community/ImageGrid.vue'

/**
 * BDD-style tests for Feature 7: 图片上传校验 + 样式 + 重构
 * Covers issue #11 (CSS namespace), #12, #14.
 *
 * The ImageGrid component uses Teleport to render the viewer overlay
 * on document.body, which means its viewer CSS classes are NOT scoped.
 * After the fix, all non-scoped viewer classes should use a
 * 'community-viewer-' prefix to avoid global CSS collisions.
 */
describe('ImageGrid', () => {

  const singleImage = ['http://example.com/photo1.jpg']
  const multipleImages = [
    'http://example.com/photo1.jpg',
    'http://example.com/photo2.jpg',
    'http://example.com/photo3.jpg'
  ]
  const mountedWrappers = []

  /**
   * Helper: mount ImageGrid with attachTo document body
   * so that Teleport content is rendered in the DOM.
   */
  const mountGrid = (images = [], props = {}) => {
    const wrapper = mount(ImageGrid, {
      props: { images, ...props },
      attachTo: document.body,
      global: {
        stubs: {
          // Do NOT stub Transition so we can observe viewer rendering
          Transition: false
        }
      }
    })
    mountedWrappers.push(wrapper)
    return wrapper
  }

  afterEach(() => {
    // 先卸载仍挂载的组件，再清理可能残留的预览层，避免 Vue 后续 patch 已被手工删除的节点。
    mountedWrappers.splice(0).forEach((wrapper) => {
      if (wrapper.exists()) wrapper.unmount()
    })
    document.body.querySelectorAll('.custom-viewer, [class*="community-viewer"]').forEach(el => el.remove())
  })

  // ==========================================================
  //  Feature 7.4: CSS namespace for viewer overlay (#11)
  // ==========================================================

  describe('Feature 7.4: CSS namespace for viewer overlay (#11)', () => {

    it('should use namespaced CSS class names for viewer container', async () => {
      // ====== Given ======
      const wrapper = mountGrid(multipleImages)

      // ====== When ======
      // Click the first grid item to open the viewer
      const gridItems = wrapper.findAll('.grid-item')
      expect(gridItems.length).toBe(3)
      await gridItems[0].trigger('click')

      // ====== Then ======
      // After fix: the viewer container should use 'community-viewer' class prefix
      const viewer = document.body.querySelector('.custom-viewer')
        || document.body.querySelector('.community-viewer')

      expect(viewer).not.toBeNull()
      // Document expected behavior: after fix, the class should be 'community-viewer'
      // Before fix: it is 'custom-viewer' (no namespace)
      // This assertion documents the target state
      expect(viewer.classList.contains('community-viewer')).toBe(true)
    })

    it('should use namespaced CSS class for viewer close button', async () => {
      // ====== Given ======
      const wrapper = mountGrid(singleImage)

      // ====== When ======
      const gridItem = wrapper.find('.grid-item')
      await gridItem.trigger('click')

      // ====== Then ======
      // After fix: close button should use 'community-viewer-close' class
      const closeBtn = document.body.querySelector('.viewer-close')
        || document.body.querySelector('.community-viewer-close')

      expect(closeBtn).not.toBeNull()
      expect(closeBtn.classList.contains('community-viewer-close')).toBe(true)
    })

    it('should use namespaced CSS classes for viewer arrows', async () => {
      // ====== Given ======
      const wrapper = mountGrid(multipleImages)

      // ====== When ======
      const gridItem = wrapper.find('.grid-item')
      await gridItem.trigger('click')

      // ====== Then ======
      // After fix: arrow buttons should use 'community-viewer-arrow' class
      const arrows = document.body.querySelectorAll('.viewer-arrow')
        || document.body.querySelectorAll('.community-viewer-arrow')

      expect(arrows.length).toBeGreaterThanOrEqual(2)
      for (const arrow of arrows) {
        expect(arrow.classList.contains('community-viewer-arrow')).toBe(true)
      }

      wrapper.unmount()
    })

    it('should use namespaced CSS class for viewer canvas', async () => {
      // ====== Given ======
      const wrapper = mountGrid(singleImage)

      // ====== When ======
      const gridItem = wrapper.find('.grid-item')
      await gridItem.trigger('click')

      // ====== Then ======
      const canvas = document.body.querySelector('.viewer-canvas')
        || document.body.querySelector('.community-viewer-canvas')

      expect(canvas).not.toBeNull()
      expect(canvas.classList.contains('community-viewer-canvas')).toBe(true)

      wrapper.unmount()
    })

    it('should use namespaced CSS class for viewer counter', async () => {
      // ====== Given ======
      const wrapper = mountGrid(multipleImages)

      // ====== When ======
      const gridItem = wrapper.find('.grid-item')
      await gridItem.trigger('click')

      // ====== Then ======
      const counter = document.body.querySelector('.viewer-counter')
        || document.body.querySelector('.community-viewer-counter')

      expect(counter).not.toBeNull()
      expect(counter.classList.contains('community-viewer-counter')).toBe(true)

      wrapper.unmount()
    })

    it('should use namespaced CSS class for viewer image', async () => {
      // ====== Given ======
      const wrapper = mountGrid(singleImage)

      // ====== When ======
      const gridItem = wrapper.find('.grid-item')
      await gridItem.trigger('click')

      // ====== Then ======
      const img = document.body.querySelector('.viewer-img')
        || document.body.querySelector('.community-viewer-img')

      expect(img).not.toBeNull()
      expect(img.classList.contains('community-viewer-img')).toBe(true)

      wrapper.unmount()
    })

    it('should use namespaced CSS class for prev/next direction indicators', async () => {
      // ====== Given ======
      const wrapper = mountGrid(multipleImages)

      // ====== When ======
      const gridItem = wrapper.find('.grid-item')
      await gridItem.trigger('click')

      // ====== Then ======
      const prev = document.body.querySelector('.viewer-prev')
        || document.body.querySelector('.community-viewer-prev')
      const next = document.body.querySelector('.viewer-next')
        || document.body.querySelector('.community-viewer-next')

      expect(prev).not.toBeNull()
      expect(next).not.toBeNull()
      expect(prev.classList.contains('community-viewer-prev')).toBe(true)
      expect(next.classList.contains('community-viewer-next')).toBe(true)

      wrapper.unmount()
    })
  })

  // ==========================================================
  //  ImageGrid core behavior (non-namespaced, functional tests)
  // ==========================================================

  describe('ImageGrid core behavior', () => {

    it('applies parent class to the image grid root element', () => {
      const wrapper = mount(ImageGrid, {
        props: { images: singleImage },
        attrs: { class: 'card-images' },
        attachTo: document.body
      })
      mountedWrappers.push(wrapper)

      const grid = wrapper.find('.image-grid')
      expect(grid.classes()).toContain('card-images')
    })

    it('renders correct number of grid items (max 9)', () => {
      const images = Array.from({ length: 12 }, (_, i) => `http://example.com/img${i}.jpg`)
      const wrapper = mountGrid(images)

      const gridItems = wrapper.findAll('.grid-item')
      expect(gridItems.length).toBe(9) // displayImages slices to 9

      wrapper.unmount()
    })

    it('shows more-overlay when images exceed 9', () => {
      const images = Array.from({ length: 12 }, (_, i) => `http://example.com/img${i}.jpg`)
      const wrapper = mountGrid(images)

      // The 9th item (index 8) should have the "+3" overlay
      const lastItem = wrapper.findAll('.grid-item')[8]
      const overlay = lastItem.find('.more-overlay')
      expect(overlay.exists()).toBe(true)
      expect(overlay.text()).toBe('+3')

      wrapper.unmount()
    })

    it('does not show more-overlay when images are exactly 9 or fewer', () => {
      const wrapper = mountGrid(multipleImages) // 3 images

      const overlays = wrapper.findAll('.more-overlay')
      expect(overlays.length).toBe(0)

      wrapper.unmount()
    })

    it('opens viewer on grid item click', async () => {
      const wrapper = mountGrid(multipleImages)

      // Click first image
      await wrapper.findAll('.grid-item')[0].trigger('click')

      // Viewer should be visible in DOM via Teleport
      const viewer = document.body.querySelector('.custom-viewer')
        || document.body.querySelector('.community-viewer')
      expect(viewer).not.toBeNull()

      wrapper.unmount()
    })

    it('closes viewer on close button click', async () => {
      const wrapper = mountGrid(multipleImages)

      // Open viewer
      await wrapper.findAll('.grid-item')[0].trigger('click')

      // Find and click close button
      const closeBtn = document.body.querySelector('.viewer-close')
        || document.body.querySelector('.community-viewer-close')
      expect(closeBtn).not.toBeNull()
      closeBtn.click()
      await wrapper.vm.$nextTick()

      // Viewer should be removed
      const viewer = document.body.querySelector('.custom-viewer')
        || document.body.querySelector('.community-viewer')
      expect(viewer).toBeNull()

      wrapper.unmount()
    })

    it('does not render arrows for single image', async () => {
      const wrapper = mountGrid(singleImage)

      await wrapper.find('.grid-item').trigger('click')

      const arrows = document.body.querySelectorAll('.viewer-arrow')
      expect(arrows.length).toBe(0)

      wrapper.unmount()
    })

    it('renders correct counter text', async () => {
      const wrapper = mountGrid(multipleImages)

      await wrapper.findAll('.grid-item')[1].trigger('click') // Click 2nd image

      const counter = document.body.querySelector('.viewer-counter')
        || document.body.querySelector('.community-viewer-counter')
      // The viewerIndex should be 1 after clicking the 2nd image
      expect(counter).not.toBeNull()
      expect(counter.textContent.trim()).toBe('2 / 3')

      wrapper.unmount()
    })

    it('responds to Escape key to close viewer', async () => {
      const wrapper = mountGrid(multipleImages)

      await wrapper.findAll('.grid-item')[0].trigger('click')

      // Simulate Escape key
      const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' })
      document.dispatchEvent(escapeEvent)
      await wrapper.vm.$nextTick()

      const viewer = document.body.querySelector('.custom-viewer')
        || document.body.querySelector('.community-viewer')
      expect(viewer).toBeNull()

      wrapper.unmount()
    })

    it('navigates to next image on ArrowRight key', async () => {
      const wrapper = mountGrid(multipleImages)

      await wrapper.findAll('.grid-item')[0].trigger('click') // viewerIndex = 0

      // Press ArrowRight
      const rightEvent = new KeyboardEvent('keydown', { key: 'ArrowRight' })
      document.dispatchEvent(rightEvent)
      await wrapper.vm.$nextTick()

      // viewerIndex should now be 1
      expect(wrapper.vm.viewerIndex).toBe(1)

      wrapper.unmount()
    })

    it('navigates to previous image on ArrowLeft key (with wrap-around)', async () => {
      const wrapper = mountGrid(multipleImages)

      await wrapper.findAll('.grid-item')[0].trigger('click') // viewerIndex = 0

      // Press ArrowLeft (should wrap to last image)
      const leftEvent = new KeyboardEvent('keydown', { key: 'ArrowLeft' })
      document.dispatchEvent(leftEvent)
      await wrapper.vm.$nextTick()

      // viewerIndex should wrap to 2 (last image)
      expect(wrapper.vm.viewerIndex).toBe(2)

      wrapper.unmount()
    })
  })
})
