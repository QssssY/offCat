package com.airesume.server.service;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.community.CreateCommentRequest;
import com.airesume.server.mapper.CommunityCommentMapper;
import com.airesume.server.mapper.CommunityPostFavoriteMapper;
import com.airesume.server.mapper.CommunityPostLikeMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BDD-style tests for Feature 6 and Feature 7.
 *
 * Feature 6: 评论内容校验 (#6) - CreateCommentRequest DTO validation
 * Feature 7: 图片上传校验 (#14) - CommunityService.uploadImage validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Feature 6 & 7: 评论内容校验 + 图片上传校验")
class CommunityServiceValidationTest {

    // ==================== Shared infrastructure ====================

    @Mock
    private CommunityPostMapper postMapper;

    @Mock
    private CommunityCommentMapper commentMapper;

    @Mock
    private CommunityPostLikeMapper likeMapper;

    @Mock
    private CommunityPostFavoriteMapper favoriteMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private ObjectMapper objectMapper;

    private CommunityService service;

    private static final Long USER_ID = 1001L;

    // ==========================================================
    //  Feature 6: 评论内容校验 - CreateCommentRequest DTO validation
    // ==========================================================

    @Nested
    @DisplayName("Feature 6: 评论内容校验 (#6)")
    class CreateCommentRequestValidationTest {

        private Validator validator;

        @BeforeEach
        void setUp() {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @Test
        @DisplayName("Scenario 6.1 [P2] - 纯空格评论被拒绝")
        void shouldRejectWhitespaceOnlyContent() {
            // ====== Given ======
            CreateCommentRequest request = new CreateCommentRequest();
            request.setContent("   ");

            // ====== When ======
            Set<jakarta.validation.ConstraintViolation<CreateCommentRequest>> violations =
                    validator.validate(request);

            // ====== Then ======
            // 修复后：添加 @NotBlank 注解后，纯空格评论应被拒绝
            // 当前状态：缺少 @NotBlank，此测试会 FAIL
            assertFalse(violations.isEmpty(),
                    "纯空格评论应被拒绝（添加 @NotBlank 后此测试通过）");
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("不能为空")),
                    "违规消息应包含'不能为空'");
        }

        @Test
        @DisplayName("Scenario 6.2 [P2] - null评论被拒绝")
        void shouldRejectNullContent() {
            // ====== Given ======
            CreateCommentRequest request = new CreateCommentRequest();
            request.setContent(null);

            // ====== When ======
            Set<jakarta.validation.ConstraintViolation<CreateCommentRequest>> violations =
                    validator.validate(request);

            // ====== Then ======
            // 修复后：添加 @NotBlank 注解后，null 评论应被拒绝
            // 当前状态：缺少 @NotBlank，此测试会 FAIL
            assertFalse(violations.isEmpty(),
                    "null评论应被拒绝（添加 @NotBlank 后此测试通过）");
        }

        @Test
        @DisplayName("Scenario 6.3 [P2] - 正常评论通过")
        void shouldAcceptNormalContent() {
            // ====== Given ======
            CreateCommentRequest request = new CreateCommentRequest();
            request.setContent("正常评论内容");

            // ====== When ======
            Set<jakarta.validation.ConstraintViolation<CreateCommentRequest>> violations =
                    validator.validate(request);

            // ====== Then ======
            assertTrue(violations.isEmpty(),
                    "正常评论内容应通过校验");
        }

        @Test
        @DisplayName("Scenario 6.4 [P2] - 超长评论被拒绝")
        void shouldRejectOverlongContent() {
            // ====== Given ======
            CreateCommentRequest request = new CreateCommentRequest();
            request.setContent("a".repeat(501));

            // ====== When ======
            Set<jakarta.validation.ConstraintViolation<CreateCommentRequest>> violations =
                    validator.validate(request);

            // ====== Then ======
            assertFalse(violations.isEmpty(),
                    "超过500字的评论应被拒绝");
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("500字")),
                    "违规消息应包含'500字'");
        }

        @Test
        @DisplayName("Scenario 6.5 [P2] - 恰好500字评论通过")
        void shouldAcceptExactlyMaxLengthContent() {
            // ====== Given ======
            CreateCommentRequest request = new CreateCommentRequest();
            request.setContent("a".repeat(500));

            // ====== When ======
            Set<jakarta.validation.ConstraintViolation<CreateCommentRequest>> violations =
                    validator.validate(request);

            // ====== Then ======
            assertTrue(violations.isEmpty(),
                    "恰好500字的评论应通过校验");
        }
    }

    // ==========================================================
    //  Feature 7: 图片上传校验 (#14)
    // ==========================================================

    @Nested
    @DisplayName("Feature 7: 图片上传校验 (#14)")
    class ImageUploadValidationTest {

        private Path tempDir;

        @BeforeEach
        void setUp() throws Exception {
            service = new CommunityService(
                    postMapper, commentMapper, likeMapper, favoriteMapper, userMapper, objectMapper
            );
            // Set @Value injected fields via reflection
            ReflectionTestUtils.setField(service, "maxFileSize", 5 * 1024 * 1024L);
            tempDir = Files.createTempDirectory("community-upload-test");
            ReflectionTestUtils.setField(service, "configuredUploadDir", tempDir.toString());
        }

        // ---------- File empty validation ----------

        @Test
        @DisplayName("Scenario 7.0a [P2] - 空文件被拒绝")
        void shouldRejectEmptyFile() {
            // ====== Given ======
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", new byte[0]
            );

            // ====== When / Then ======
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.uploadImage(emptyFile, USER_ID));
            assertEquals("上传文件不能为空", exception.getMessage());
        }

        // ---------- File size validation ----------

        @Test
        @DisplayName("Scenario 7.0b [P2] - 超大文件被拒绝")
        void shouldRejectOversizedFile() {
            // ====== Given ======
            byte[] largeContent = new byte[(int) (5 * 1024 * 1024 + 1)]; // 5MB + 1 byte
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", largeContent
            );

            // ====== When / Then ======
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.uploadImage(largeFile, USER_ID));
            assertTrue(exception.getMessage().contains("图片大小不能超过"),
                    "应提示文件大小限制");
        }

        // ---------- Extension validation ----------

        @Test
        @DisplayName("Scenario 7.0c [P2] - 无扩展名文件被拒绝")
        void shouldRejectFileWithNoExtension() {
            // ====== Given ======
            MockMultipartFile noExtFile = new MockMultipartFile(
                    "file", "filename_no_ext", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8}
            );

            // ====== When / Then ======
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.uploadImage(noExtFile, USER_ID));
            assertTrue(exception.getMessage().contains("仅支持"),
                    "应提示仅支持 JPG、PNG、GIF、WebP 格式");
        }

        @Test
        @DisplayName("Scenario 7.0d [P2] - null文件名被拒绝")
        void shouldRejectFileWithNullFilename() {
            // ====== Given ======
            MockMultipartFile nullNameFile = new MockMultipartFile(
                    "file", null, "image/jpeg", new byte[]{1, 2, 3}
            );

            // ====== When / Then ======
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.uploadImage(nullNameFile, USER_ID));
            assertEquals("文件名不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("Scenario 7.0e [P2] - 不支持的扩展名被拒绝")
        void shouldRejectUnsupportedExtension() {
            // ====== Given ======
            MockMultipartFile bmpFile = new MockMultipartFile(
                    "file", "image.bmp", "image/bmp", new byte[]{0x42, 0x4D}
            );

            // ====== When / Then ======
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.uploadImage(bmpFile, USER_ID));
            assertTrue(exception.getMessage().contains("仅支持"),
                    "BMP文件应被拒绝");
        }

        // ---------- Magic byte validation ----------

        @Test
        @DisplayName("Scenario 7.1 [P2] - 伪造后缀文件被拒绝（基于magic bytes检测）")
        void shouldRejectFileWithMismatchedMagicBytes() {
            // ====== Given ======
            // HTML magic bytes with .jpg extension - a disguised malicious file
            byte[] htmlContent = "<!DOCTYPE html><html><body>malware</body></html>".getBytes();
            MockMultipartFile fakeJpg = new MockMultipartFile(
                    "file", "malware.jpg", "image/jpeg", htmlContent
            );

            // ====== When / Then ======
            // 修复后：应基于 magic bytes 检测文件真实类型，拒绝伪装文件
            // 当前状态：仅校验扩展名，此文件会通过校验
            // 此测试记录期望行为 - 添加 magic bytes 校验后此测试通过
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.uploadImage(fakeJpg, USER_ID));
            assertTrue(exception.getMessage().contains("格式") || exception.getMessage().contains("图片"),
                    "应拒绝伪装文件，提示格式不正确");
        }

        @Test
        @DisplayName("Scenario 7.2 [P2] - 真实JPG通过校验并成功上传")
        void shouldAcceptRealJpgFile() {
            // ====== Given ======
            // JPEG magic bytes: FF D8 FF
            byte[] jpgContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                    0x00, 0x10, 0x4A, 0x46, 0x49, 0x46}; // JFIF header fragment
            MockMultipartFile jpgFile = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", jpgContent
            );

            // ====== When ======
            String result = service.uploadImage(jpgFile, USER_ID);

            // ====== Then ======
            assertNotNull(result, "上传成功应返回文件URL");
            assertTrue(result.startsWith("/uploads/community/"),
                    "返回的URL应以 /uploads/community/ 开头");
            assertTrue(result.endsWith(".jpg"),
                    "返回的URL应保留原始扩展名");
        }

        @Test
        @DisplayName("Scenario 7.3 [P2] - 真实PNG通过校验并成功上传")
        void shouldAcceptRealPngFile() {
            // ====== Given ======
            // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
            byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47,
                    0x0D, 0x0A, 0x1A, 0x0A};
            MockMultipartFile pngFile = new MockMultipartFile(
                    "file", "screenshot.png", "image/png", pngContent
            );

            // ====== When ======
            String result = service.uploadImage(pngFile, USER_ID);

            // ====== Then ======
            assertNotNull(result, "上传成功应返回文件URL");
            assertTrue(result.startsWith("/uploads/community/"),
                    "返回的URL应以 /uploads/community/ 开头");
            assertTrue(result.endsWith(".png"),
                    "返回的URL应保留原始扩展名");
        }

        @Test
        @DisplayName("Scenario 7.3b [P2] - 真实GIF通过校验并成功上传")
        void shouldAcceptRealGifFile() {
            // ====== Given ======
            // GIF magic bytes: "GIF89a"
            byte[] gifContent = "GIF89a".getBytes();
            MockMultipartFile gifFile = new MockMultipartFile(
                    "file", "animation.gif", "image/gif", gifContent
            );

            // ====== When ======
            String result = service.uploadImage(gifFile, USER_ID);

            // ====== Then ======
            assertNotNull(result, "上传成功应返回文件URL");
            assertTrue(result.startsWith("/uploads/community/"),
                    "返回的URL应以 /uploads/community/ 开头");
            assertTrue(result.endsWith(".gif"),
                    "返回的URL应保留原始扩展名");
        }

        @Test
        @DisplayName("Scenario 7.3c [P2] - 真实WebP通过校验并成功上传")
        void shouldAcceptRealWebpFile() {
            // ====== Given ======
            // WebP magic bytes: "RIFF" + 4 bytes size + "WEBP"
            byte[] webpContent = new byte[]{
                    0x52, 0x49, 0x46, 0x46, // "RIFF"
                    0x00, 0x00, 0x00, 0x00, // file size placeholder
                    0x57, 0x45, 0x42, 0x50  // "WEBP"
            };
            MockMultipartFile webpFile = new MockMultipartFile(
                    "file", "image.webp", "image/webp", webpContent
            );

            // ====== When ======
            String result = service.uploadImage(webpFile, USER_ID);

            // ====== Then ======
            assertNotNull(result, "上传成功应返回文件URL");
            assertTrue(result.startsWith("/uploads/community/"),
                    "返回的URL应以 /uploads/community/ 开头");
            assertTrue(result.endsWith(".webp"),
                    "返回的URL应保留原始扩展名");
        }

        @Test
        @DisplayName("Scenario 7.3d [P2] - JPEG扩展名为.jpeg也能通过校验")
        void shouldAcceptJpegExtension() {
            // ====== Given ======
            byte[] jpgContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
            MockMultipartFile jpegFile = new MockMultipartFile(
                    "file", "photo.jpeg", "image/jpeg", jpgContent
            );

            // ====== When ======
            String result = service.uploadImage(jpegFile, USER_ID);

            // ====== Then ======
            assertNotNull(result, ".jpeg扩展名应通过校验");
            assertTrue(result.endsWith(".jpeg"),
                    "返回的URL应保留 .jpeg 扩展名");
        }
    }
}
