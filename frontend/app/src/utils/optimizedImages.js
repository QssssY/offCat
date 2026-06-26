import backgroundPng from '@/assets/background.png'
import logoPng from '@/assets/logo.png'
import assistantPng from '@/assets/assistant.png'
import userPng from '@/assets/user.png'
import backgroundDesktopWebp from '@/assets/optimized/background-desktop.webp'
import backgroundMobileWebp from '@/assets/optimized/background-mobile.webp'
import logoWebp from '@/assets/optimized/logo.webp'
import assistantWebp from '@/assets/optimized/assistant.webp'
import userWebp from '@/assets/optimized/user.webp'

export const optimizedImages = {
  homeBackground: {
    desktopWebp: backgroundDesktopWebp,
    mobileWebp: backgroundMobileWebp,
    png: backgroundPng
  },
  logo: {
    webp: logoWebp,
    png: logoPng
  },
  assistantAvatar: {
    webp: assistantWebp,
    png: assistantPng
  },
  userAvatar: {
    webp: userWebp,
    png: userPng
  }
}

export function toCssImageSet(webp, png) {
  return `image-set(url("${webp}") type("image/webp"), url("${png}") type("image/png"))`
}
