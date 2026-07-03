package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.auth.CaptchaResponse;
import com.airesume.server.service.CaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务实现。
 * 基于 JDK BufferedImage + Graphics2D 生成，无需额外依赖。
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL_MINUTES = 5;
    private static final int IMAGE_WIDTH = 130;
    private static final int IMAGE_HEIGHT = 50;
    private static final int CODE_LENGTH = 4;
    /** 排除易混淆的 0/O/1/I/l */
    private static final String CHAR_POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public CaptchaResponse generate() {
        String captchaId = UUID.randomUUID().toString();
        String code = randomCode();
        String imageBase64 = renderImage(code);

        // 存入 Redis，5 分钟过期
        storeCode(captchaId, code);

        log.info("Captcha generated, id: {}", captchaId);

        return CaptchaResponse.builder()
                .captchaId(captchaId)
                .captchaImage(imageBase64)
                .build();
    }

    @Override
    public void verify(String captchaId, String captchaCode) {
        if (stringRedisTemplate == null) {
            log.warn("Redis unavailable, captcha verification not possible, id: {}", captchaId);
            throw new BusinessException("验证码服务暂不可用，请稍后重试");
        }

        String key = CAPTCHA_KEY_PREFIX + captchaId;
        String storedCode = null;
        try {
            // 使用 Redis 原子 getAndDelete 消费验证码，防止并发请求复用同一个 captchaId。
            storedCode = stringRedisTemplate.opsForValue().getAndDelete(key);
        } catch (Exception e) {
            log.error("Failed to read captcha from Redis, id: {}", captchaId, e);
            throw new BusinessException("验证码服务暂不可用，请稍后重试");
        }

        if (storedCode == null) {
            throw new BusinessException("验证码错误或已过期");
        }

        if (!storedCode.equalsIgnoreCase(captchaCode)) {
            log.info("Captcha code mismatch, id: {}", captchaId);
            throw new BusinessException("验证码错误或已过期");
        }

        log.info("Captcha verified successfully, id: {}", captchaId);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    private void storeCode(String captchaId, String code) {
        if (stringRedisTemplate == null) {
            log.warn("Redis unavailable, captcha code not stored, id: {}", captchaId);
            throw new BusinessException("验证码服务暂不可用，请稍后重试");
        }
        try {
            String key = CAPTCHA_KEY_PREFIX + captchaId;
            stringRedisTemplate.opsForValue().set(key, code.toUpperCase(), CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to store captcha code in Redis, id: {}", captchaId, e);
            throw new BusinessException("验证码服务暂不可用，请稍后重试");
        }
    }

    /**
     * 使用 JDK Graphics2D 渲染验证码图片。
     */
    private String renderImage(String code) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            // 背景
            g.setColor(new Color(245, 245, 245));
            g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

            // 干扰线（3 条）
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 3; i++) {
                g.setColor(randomColor(150, 200));
                int x1 = RANDOM.nextInt(IMAGE_WIDTH / 3);
                int y1 = RANDOM.nextInt(IMAGE_HEIGHT);
                int x2 = IMAGE_WIDTH - RANDOM.nextInt(IMAGE_WIDTH / 3);
                int y2 = RANDOM.nextInt(IMAGE_HEIGHT);
                g.drawLine(x1, y1, x2, y2);
            }

            // 绘制字符
            int charWidth = IMAGE_WIDTH / (CODE_LENGTH + 1);
            for (int i = 0; i < code.length(); i++) {
                String ch = String.valueOf(code.charAt(i));
                g.setColor(randomColor(20, 120));
                // 随机旋转
                double angle = (RANDOM.nextDouble() - 0.5) * 0.6;
                double x = charWidth * (i + 0.7);
                double y = IMAGE_HEIGHT * 0.7;

                AffineTransform originalTransform = g.getTransform();
                g.rotate(angle, x, y);

                Font font = new Font("Arial", Font.BOLD, 28 + RANDOM.nextInt(6));
                g.setFont(font);
                g.drawString(ch, (int) x, (int) y);

                g.setTransform(originalTransform);
            }

            // 噪点（20 个）
            for (int i = 0; i < 20; i++) {
                g.setColor(randomColor(100, 180));
                int x = RANDOM.nextInt(IMAGE_WIDTH);
                int y = RANDOM.nextInt(IMAGE_HEIGHT);
                g.fillOval(x, y, 2, 2);
            }
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        }

        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private Color randomColor(int min, int max) {
        int r = min + RANDOM.nextInt(max - min);
        int g = min + RANDOM.nextInt(max - min);
        int b = min + RANDOM.nextInt(max - min);
        return new Color(r, g, b);
    }
}
