package com.airesume.server.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 图片文件校验工具类
 * 通过魔术字节（magic bytes）校验上传文件的实际格式是否与扩展名一致
 */
public class ImageValidator {

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "gif", new byte[]{0x47, 0x49, 0x46},
            "webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    /**
     * 校验上传文件的魔术字节是否与声明的扩展名匹配
     *
     * @param file      上传的文件
     * @param extension 文件扩展名（不含点号，如 "jpg"、"png"）
     * @return true 如果格式匹配或扩展名不在已知列表中；false 如果格式不匹配或文件太小
     * @throws IOException 读取文件时出错
     */
    public static boolean validateMagicBytes(MultipartFile file, String extension) throws IOException {
        String ext = extension.toLowerCase();
        byte[] expected = MAGIC_BYTES.get(ext);
        if (expected == null) {
            return true;
        }

        boolean isWebp = "webp".equals(ext);
        int readLen = isWebp ? 12 : expected.length;

        byte[] header = new byte[readLen];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < readLen) {
                return false;
            }
        }

        for (int i = 0; i < expected.length; i++) {
            if (header[i] != expected[i]) {
                return false;
            }
        }

        if (isWebp) {
            return header[8] == 0x57 && header[9] == 0x45
                    && header[10] == 0x42 && header[11] == 0x50;
        }
        return true;
    }
}
