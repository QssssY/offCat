# Community Auto Moderation Routing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-stage community auto-moderation router so severe text is blocked, suspicious or image content stays in manual review, and low-risk text-only posts/comments can be auto-approved.

**Architecture:** Keep the current "approved content only becomes public" boundary. Extract moderation rules from `CommunityService` into a focused backend service, reuse existing `review_status` and `review_reason` fields, and return the creation review status to the frontend so UI messages and optimistic comment insertion stay correct.

**Tech Stack:** Spring Boot, MyBatis-Plus, JUnit 5, Mockito, Vue 3, Vitest, Element Plus.

---

## Scope

This plan intentionally does not add cloud moderation, image AI review, reporting flows, batch review, or a sensitive-word configuration page. Image content remains manual-review-only in this phase.

## File Structure

- Create: `server/src/main/java/com/airesume/server/service/CommunityModerationDecision.java`
  - Small immutable decision object used by community creation paths.
- Create: `server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
  - Owns text normalization, severe-word blocking, suspicious-word routing, and image/manual-review routing.
- Create: `server/src/main/java/com/airesume/server/dto/community/CreateCommunityContentResponse.java`
  - API response for create post/comment: content ID plus final review status.
- Modify: `server/src/main/java/com/airesume/server/service/CommunityService.java`
  - Replace inline `validateCommunityTextOrThrow()` with the new moderation service and set `review_status` from the decision.
- Modify: `server/src/main/java/com/airesume/server/controller/CommunityController.java`
  - Return `Result<CreateCommunityContentResponse>` for post/comment creation.
- Create: `server/src/test/java/com/airesume/server/service/CommunityTextModerationServiceTest.java`
  - Direct unit tests for moderation routing.
- Modify: `server/src/test/java/com/airesume/server/service/CommunityServiceModerationTest.java`
  - Cover integration behavior for auto-approved, pending, and rejected content.
- Modify: `frontend/app/src/api/community.js`
  - Document the new create response shape; existing request wrapper can stay unchanged.
- Modify: `frontend/app/src/components/community/PostEditor.vue`
  - Show "发布成功，已公开展示" for auto-approved posts and "已提交审核，通过后将在社区展示" for pending posts.
- Modify: `frontend/app/src/components/community/ShareReportDialog.vue`
  - Show accurate share status based on backend `reviewStatus`.
- Modify: `frontend/app/src/components/community/CommentSection.vue`
  - Only insert comments/replies optimistically when the backend returns `approved`; pending comments should clear input and show review message without increasing local counts.
- Modify tests under `frontend/app/src/__tests__/components/community/`
  - Extend existing post editor and comment section tests for the new status-aware behavior.
- Modify docs:
  - `tasks/TASK_62_COMMUNITY_AUTO_MODERATION_ROUTING_BACKEND.md`
  - `frontend/tasks/TASK_62_COMMUNITY_AUTO_MODERATION_ROUTING_FRONTEND.md`
  - `tasks/stage.md`
  - `frontend/tasks/stage.md`

---

### Task 1: Add Backend Moderation Decision Service

**Files:**
- Create: `server/src/main/java/com/airesume/server/service/CommunityModerationDecision.java`
- Create: `server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
- Test: `server/src/test/java/com/airesume/server/service/CommunityTextModerationServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Create `server/src/test/java/com/airesume/server/service/CommunityTextModerationServiceTest.java`:

```java
package com.airesume.server.service;

import com.airesume.server.common.constants.CommunityConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 社区文本自动审核服务测试。
 * 作用：锁定严重违规自动拒绝、疑似风险进入人工复核、低风险纯文本自动通过的分流边界。
 */
class CommunityTextModerationServiceTest {

    private final CommunityTextModerationService moderationService = new CommunityTextModerationService();

    @Test
    void shouldRejectSeverePostTextBeforePersist() {
        CommunityModerationDecision decision = moderationService.reviewPost(
                "成人视频资源",
                "这里发布色情成人视频和约炮引流内容。",
                false
        );

        assertTrue(decision.isRejected());
        assertEquals("内容包含违规信息，请修改后再发布", decision.getRejectMessage());
    }

    @Test
    void shouldRouteSuspiciousPostToPendingReview() {
        CommunityModerationDecision decision = moderationService.reviewPost(
                "兼职内推",
                "想了解更多可以加薇详聊。",
                false
        );

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_PENDING, decision.getReviewStatus());
        assertEquals("内容需人工复核", decision.getReviewReason());
    }

    @Test
    void shouldRoutePostWithImagesToPendingReview() {
        CommunityModerationDecision decision = moderationService.reviewPost(
                "面试复盘",
                "这次主要考察了 JVM 和索引。",
                true
        );

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_PENDING, decision.getReviewStatus());
        assertEquals("包含图片，需人工复核", decision.getReviewReason());
    }

    @Test
    void shouldApproveLowRiskTextOnlyPost() {
        CommunityModerationDecision decision = moderationService.reviewPost(
                "Java 面试复盘",
                "这次主要聊了线程池参数、慢 SQL 排查和项目难点。",
                false
        );

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_APPROVED, decision.getReviewStatus());
        assertEquals(null, decision.getReviewReason());
    }

    @Test
    void shouldApproveBlankTextImageFreeCommentAsPendingOnlyWhenImageExists() {
        CommunityModerationDecision decision = moderationService.reviewComment("", true);

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_PENDING, decision.getReviewStatus());
        assertEquals("包含图片，需人工复核", decision.getReviewReason());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd F:\Code\ai-resume\server
mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest" test
```

Expected: FAIL because `CommunityTextModerationService` and `CommunityModerationDecision` do not exist.

- [ ] **Step 3: Create the decision object**

Create `server/src/main/java/com/airesume/server/service/CommunityModerationDecision.java`:

```java
package com.airesume.server.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 社区自动审核决策。
 * 作用：统一表达内容创建时的自动拒绝、自动通过或进入人工复核状态。
 */
@Getter
@RequiredArgsConstructor
public class CommunityModerationDecision {

    private final boolean rejected;
    private final String reviewStatus;
    private final String reviewReason;
    private final String rejectMessage;

    public static CommunityModerationDecision rejected(String message) {
        return new CommunityModerationDecision(true, null, null, message);
    }

    public static CommunityModerationDecision accepted(String reviewStatus, String reviewReason) {
        return new CommunityModerationDecision(false, reviewStatus, reviewReason, null);
    }
}
```

- [ ] **Step 4: Create the moderation service**

Create `server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`:

```java
package com.airesume.server.service;

import com.airesume.server.common.constants.CommunityConstants;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 社区文本自动审核服务。
 * 作用：先用本地规则完成低成本分流，降低管理员处理明显正常纯文本内容的工作量。
 */
@Service
public class CommunityTextModerationService {

    private static final String REJECT_MESSAGE = "内容包含违规信息，请修改后再发布";
    private static final String MANUAL_REVIEW_REASON = "内容需人工复核";
    private static final String IMAGE_REVIEW_REASON = "包含图片，需人工复核";

    private static final List<String> SEVERE_WORDS = List.of(
            "色情", "成人视频", "约炮", "裸聊", "成人视频资源",
            "政治敏感", "反动", "颠覆", "台独", "港独",
            "傻逼", "去死", "诈骗", "博彩"
    );

    private static final List<String> SUSPICIOUS_WORDS = List.of(
            "加微信", "加薇", "私聊", "联系方式", "兼职", "返利", "代办", "资源"
    );

    /**
     * 审核帖子标题和正文。
     */
    public CommunityModerationDecision reviewPost(String title, String content, boolean hasImages) {
        return reviewText(title + "\n" + content, hasImages);
    }

    /**
     * 审核评论正文。
     */
    public CommunityModerationDecision reviewComment(String content, boolean hasImages) {
        return reviewText(content, hasImages);
    }

    private CommunityModerationDecision reviewText(String text, boolean hasImages) {
        String normalized = normalizeModerationText(text);
        if (SEVERE_WORDS.stream().anyMatch(normalized::contains)) {
            return CommunityModerationDecision.rejected(REJECT_MESSAGE);
        }
        if (hasImages) {
            return CommunityModerationDecision.accepted(CommunityConstants.REVIEW_STATUS_PENDING, IMAGE_REVIEW_REASON);
        }
        if (SUSPICIOUS_WORDS.stream().anyMatch(normalized::contains)) {
            return CommunityModerationDecision.accepted(CommunityConstants.REVIEW_STATUS_PENDING, MANUAL_REVIEW_REASON);
        }
        return CommunityModerationDecision.accepted(CommunityConstants.REVIEW_STATUS_APPROVED, null);
    }

    /**
     * 审核文本归一化，减少空格、大小写和简单符号绕过。
     */
    private String normalizeModerationText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[\\p{Punct}，。！？、；：“”‘’（）【】《》]", "");
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd F:\Code\ai-resume\server
mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest" test
```

Expected: PASS.

---

### Task 2: Integrate Moderation Routing Into Community Creation

**Files:**
- Modify: `server/src/main/java/com/airesume/server/service/CommunityService.java`
- Modify: `server/src/test/java/com/airesume/server/service/CommunityServiceModerationTest.java`

- [ ] **Step 1: Write failing integration tests**

Modify `CommunityServiceModerationTest` setup to pass the new service:

```java
communityService = new CommunityService(
        postMapper,
        commentMapper,
        likeMapper,
        favoriteMapper,
        userMapper,
        interviewSessionMapper,
        new ObjectMapper(),
        new CommunityTextModerationService()
);
```

Add these tests:

```java
@Test
void shouldAutoApproveLowRiskTextOnlyPost() {
    CreatePostRequest request = new CreatePostRequest();
    request.setCategory("interview_exp");
    request.setTitle("一次 Java 面试复盘");
    request.setContent("这次面试主要考察了并发和数据库索引。");

    communityService.createPost(1001L, request);

    ArgumentCaptor<CommunityPost> captor = ArgumentCaptor.forClass(CommunityPost.class);
    verify(postMapper).insert(captor.capture());
    assertEquals("approved", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
    assertEquals(null, ReflectionTestUtils.getField(captor.getValue(), "reviewReason"));
}

@Test
void shouldKeepImagePostPendingReview() {
    CreatePostRequest request = new CreatePostRequest();
    request.setCategory("interview_exp");
    request.setTitle("面试题截图");
    request.setContent("这是一张面试复盘截图。");
    request.setImages(List.of("https://example.com/review.png"));

    communityService.createPost(1001L, request);

    ArgumentCaptor<CommunityPost> captor = ArgumentCaptor.forClass(CommunityPost.class);
    verify(postMapper).insert(captor.capture());
    assertEquals("pending", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
    assertEquals("包含图片，需人工复核", ReflectionTestUtils.getField(captor.getValue(), "reviewReason"));
}

@Test
void shouldKeepSuspiciousTextPostPendingReview() {
    CreatePostRequest request = new CreatePostRequest();
    request.setCategory("referral");
    request.setTitle("内推沟通");
    request.setContent("可以私聊我联系方式。");

    communityService.createPost(1001L, request);

    ArgumentCaptor<CommunityPost> captor = ArgumentCaptor.forClass(CommunityPost.class);
    verify(postMapper).insert(captor.capture());
    assertEquals("pending", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
    assertEquals("内容需人工复核", ReflectionTestUtils.getField(captor.getValue(), "reviewReason"));
}

@Test
void shouldAutoApproveLowRiskTextOnlyCommentAndIncreaseCount() {
    CommunityPost approvedPost = buildPost(2001L, 1002L, "approved");
    approvedPost.setCommentCount(0);
    when(postMapper.selectById(2001L)).thenReturn(approvedPost);

    CreateCommentRequest request = new CreateCommentRequest();
    request.setContent("我也遇到过类似的面试追问。");

    communityService.createComment(1001L, 2001L, request);

    ArgumentCaptor<CommunityComment> captor = ArgumentCaptor.forClass(CommunityComment.class);
    verify(commentMapper).insert(captor.capture());
    assertEquals("approved", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
    verify(postMapper).updateById(approvedPost);
    assertEquals(1, approvedPost.getCommentCount());
}

@Test
void shouldKeepImageCommentPendingAndNotIncreaseCount() {
    CommunityPost approvedPost = buildPost(2001L, 1002L, "approved");
    approvedPost.setCommentCount(0);
    when(postMapper.selectById(2001L)).thenReturn(approvedPost);

    CreateCommentRequest request = new CreateCommentRequest();
    request.setContent("");
    request.setImages(List.of("https://example.com/comment.png"));

    communityService.createComment(1001L, 2001L, request);

    ArgumentCaptor<CommunityComment> captor = ArgumentCaptor.forClass(CommunityComment.class);
    verify(commentMapper).insert(captor.capture());
    assertEquals("pending", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
    verify(postMapper, never()).updateById(approvedPost);
}
```

Remove or update the old expectation that normal posts/comments are always `pending`.

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd F:\Code\ai-resume\server
mvn.cmd -q "-Dtest=CommunityServiceModerationTest" test
```

Expected: FAIL because `CommunityService` still always sets new content to `pending`.

- [ ] **Step 3: Inject the new moderation service**

Modify `CommunityService` constructor fields:

```java
private final ObjectMapper objectMapper;
private final CommunityTextModerationService moderationService;
```

- [ ] **Step 4: Apply moderation decision in `createPost`**

Replace the inline `validateCommunityTextOrThrow(title + "\n" + request.getContent());` call and fixed pending assignment with:

```java
boolean hasImages = request.getImages() != null && !request.getImages().isEmpty();
CommunityModerationDecision moderationDecision = moderationService.reviewPost(title, request.getContent(), hasImages);
if (moderationDecision.isRejected()) {
    throw new BusinessException(moderationDecision.getRejectMessage());
}
```

Set fields after creating the entity:

```java
// 自动审核分流：低风险纯文本可直接公开，图片或疑似风险内容继续进入人工审核池。
post.setReviewStatus(moderationDecision.getReviewStatus());
post.setReviewReason(moderationDecision.getReviewReason());
if (CommunityConstants.REVIEW_STATUS_APPROVED.equals(moderationDecision.getReviewStatus())) {
    post.setReviewedTime(LocalDateTime.now());
}
```

- [ ] **Step 5: Apply moderation decision in `createComment`**

Before creating `CommunityComment`, add:

```java
boolean hasImages = request.getImages() != null && !request.getImages().isEmpty();
CommunityModerationDecision moderationDecision = moderationService.reviewComment(request.getContent(), hasImages);
if (moderationDecision.isRejected()) {
    throw new BusinessException(moderationDecision.getRejectMessage());
}
```

Replace fixed pending assignment with:

```java
// 评论自动审核分流：通过后才计入评论数，待审评论不影响公开计数。
comment.setReviewStatus(moderationDecision.getReviewStatus());
comment.setReviewReason(moderationDecision.getReviewReason());
if (CommunityConstants.REVIEW_STATUS_APPROVED.equals(moderationDecision.getReviewStatus())) {
    comment.setReviewedTime(LocalDateTime.now());
}
```

After `commentMapper.insert(comment);`, only increment count for approved comments:

```java
if (CommunityConstants.REVIEW_STATUS_APPROVED.equals(moderationDecision.getReviewStatus())) {
    post.setCommentCount(post.getCommentCount() + 1);
    postMapper.updateById(post);
}
```

- [ ] **Step 6: Remove old private rule methods**

Delete `validateCommunityTextOrThrow()` and `normalizeModerationText()` from `CommunityService` after all callers are removed.

- [ ] **Step 7: Run backend moderation tests**

Run:

```bash
cd F:\Code\ai-resume\server
mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest,AdminCommunityModerationServiceTest" test
```

Expected: PASS.

---

### Task 3: Return Create Review Status From Backend API

**Files:**
- Create: `server/src/main/java/com/airesume/server/dto/community/CreateCommunityContentResponse.java`
- Modify: `server/src/main/java/com/airesume/server/service/CommunityService.java`
- Modify: `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- Test: `server/src/test/java/com/airesume/server/controller/CommunityControllerTest.java` if it exists; otherwise extend the closest existing controller test for community creation.

- [ ] **Step 1: Create response DTO**

Create `server/src/main/java/com/airesume/server/dto/community/CreateCommunityContentResponse.java`:

```java
package com.airesume.server.dto.community;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社区内容创建响应。
 * 作用：让前端知道本次创建的帖子或评论是已公开还是仍待审核。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommunityContentResponse {

    /** 新创建的帖子或评论 ID */
    private Long id;

    /** 审核状态：approved / pending */
    private String reviewStatus;
}
```

- [ ] **Step 2: Change service return type**

In `CommunityService`, change:

```java
public Long createPost(Long userId, CreatePostRequest request)
```

to:

```java
public CreateCommunityContentResponse createPost(Long userId, CreatePostRequest request)
```

Return:

```java
return new CreateCommunityContentResponse(post.getId(), post.getReviewStatus());
```

Change:

```java
public Long createComment(Long userId, Long postId, CreateCommentRequest request)
```

to:

```java
public CreateCommunityContentResponse createComment(Long userId, Long postId, CreateCommentRequest request)
```

Return:

```java
return new CreateCommunityContentResponse(comment.getId(), comment.getReviewStatus());
```

- [ ] **Step 3: Change controller response type**

In `CommunityController`, replace create endpoints:

```java
public Result<CreateCommunityContentResponse> createPost(
        Authentication authentication,
        @Valid @RequestBody CreatePostRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("[社区] 创建帖子, userId: {}, category: {}", userId, request.getCategory());
    CreateCommunityContentResponse response = communityService.createPost(userId, request);
    return Result.success(response);
}
```

```java
public Result<CreateCommunityContentResponse> createComment(
        Authentication authentication,
        @PathVariable Long postId,
        @Valid @RequestBody CreateCommentRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("[社区] 创建评论, userId: {}, postId: {}", userId, postId);
    CreateCommunityContentResponse response = communityService.createComment(userId, postId, request);
    return Result.success(response);
}
```

- [ ] **Step 4: Update Java tests for new return type**

Where tests call `communityService.createPost(...)` or `createComment(...)`, either ignore the return value or assert:

```java
CreateCommunityContentResponse response = communityService.createPost(1001L, request);
assertEquals("approved", response.getReviewStatus());
```

- [ ] **Step 5: Run backend compile and focused tests**

Run:

```bash
cd F:\Code\ai-resume\server
mvn.cmd -q "-Dtest=CommunityService*Test,AdminCommunityModerationServiceTest" test
mvn.cmd -q -DskipTests compile
```

Expected: PASS.

---

### Task 4: Update Frontend Creation UX for Status-Aware Responses

**Files:**
- Modify: `frontend/app/src/api/community.js`
- Modify: `frontend/app/src/components/community/PostEditor.vue`
- Modify: `frontend/app/src/components/community/ShareReportDialog.vue`
- Modify: `frontend/app/src/components/community/CommentSection.vue`
- Modify tests:
  - `frontend/app/src/__tests__/components/community/PostEditor.test.js`
  - Existing or new `frontend/app/src/__tests__/components/community/CommentSection.test.js`

- [ ] **Step 1: Document response shape in API wrapper**

Modify `frontend/app/src/api/community.js` JSDoc for `createPost`:

```js
/**
 * 发布帖子
 * @param {Object} data
 * @returns {Promise<{data: {id: number|string, reviewStatus: 'approved'|'pending'}}>}
 */
```

Add equivalent JSDoc for `createComment` where it is defined:

```js
/**
 * 发布评论
 * @param {number|string} postId
 * @param {Object} data
 * @returns {Promise<{data: {id: number|string, reviewStatus: 'approved'|'pending'}}>}
 */
```

- [ ] **Step 2: Update post editor message**

In `PostEditor.vue`, replace:

```js
await createPost({
  category: form.value.category,
  title: form.value.title.trim(),
  content: form.value.content.trim(),
  images: form.value.images
})
ElMessage.success('已提交审核，通过后将在社区展示')
```

with:

```js
const res = await createPost({
  category: form.value.category,
  title: form.value.title.trim(),
  content: form.value.content.trim(),
  images: form.value.images
})
const reviewStatus = res?.data?.reviewStatus
ElMessage.success(reviewStatus === 'approved' ? '发布成功，已公开展示' : '已提交审核，通过后将在社区展示')
```

- [ ] **Step 3: Update share report dialog message**

In `ShareReportDialog.vue`, replace fixed success text:

```js
ElMessage.success('分享成功')
```

with:

```js
const reviewStatus = res?.data?.reviewStatus
ElMessage.success(reviewStatus === 'approved' ? '分享成功，已公开展示' : '已提交审核，通过后将在社区展示')
```

The `createPost(...)` call must be assigned to `const res = await createPost(...)`.

- [ ] **Step 4: Update top-level comment optimistic insertion**

In `CommentSection.vue`, after `const res = await createComment(...)`, add:

```js
const reviewStatus = res?.data?.reviewStatus
if (reviewStatus !== 'approved') {
  ElMessage.success('评论已提交审核，通过后将在评论区展示')
  commentText.value = ''
  commentImages.value = []
  return
}
ElMessage.success('评论发布成功')
```

For approved comments, change the local ID from:

```js
id: res.data,
```

to:

```js
id: res.data.id,
```

- [ ] **Step 5: Update reply optimistic behavior**

In `CommentSection.vue`, replace:

```js
await createComment(props.postId, data)
ElMessage.success('回复成功')
```

with:

```js
const res = await createComment(props.postId, data)
const reviewStatus = res?.data?.reviewStatus
if (reviewStatus !== 'approved') {
  ElMessage.success('回复已提交审核，通过后将在评论区展示')
  replyText.value = ''
  replyImages.value = []
  replyTarget.value = null
  return
}
ElMessage.success('回复成功')
```

Only update `replyCount`, `total`, and fetch replies after the approved branch.

- [ ] **Step 6: Write/update frontend tests**

For `PostEditor.test.js`, add a case:

```js
it('shows public success when backend auto approves the post', async () => {
  createPost.mockResolvedValueOnce({ data: { id: 1001, reviewStatus: 'approved' } })
  // fill required form fields using the existing helper pattern in this test file
  await submitPost()
  expect(ElMessage.success).toHaveBeenCalledWith('发布成功，已公开展示')
})
```

Add a pending case:

```js
it('shows review success when backend keeps the post pending', async () => {
  createPost.mockResolvedValueOnce({ data: { id: 1002, reviewStatus: 'pending' } })
  await submitPost()
  expect(ElMessage.success).toHaveBeenCalledWith('已提交审核，通过后将在社区展示')
})
```

For `CommentSection` tests, add:

```js
it('does not optimistically insert pending comments', async () => {
  createComment.mockResolvedValueOnce({ data: { id: 3001, reviewStatus: 'pending' } })
  await submitTopLevelComment('我有一张截图')
  expect(ElMessage.success).toHaveBeenCalledWith('评论已提交审核，通过后将在评论区展示')
  expect(wrapper.findAll('.comment-card')).toHaveLength(0)
})
```

Add approved case:

```js
it('optimistically inserts auto approved comments', async () => {
  createComment.mockResolvedValueOnce({ data: { id: 3002, reviewStatus: 'approved' } })
  await submitTopLevelComment('我也遇到过类似问题')
  expect(ElMessage.success).toHaveBeenCalledWith('评论发布成功')
  expect(wrapper.text()).toContain('我也遇到过类似问题')
})
```

- [ ] **Step 7: Run frontend tests**

Run:

```bash
cd F:\Code\ai-resume\frontend\app
npm.cmd test -- --run src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/CommentSection.test.js
```

Expected: PASS.

---

### Task 5: Documentation and Stage Updates

**Files:**
- Create: `tasks/TASK_62_COMMUNITY_AUTO_MODERATION_ROUTING_BACKEND.md`
- Create: `frontend/tasks/TASK_62_COMMUNITY_AUTO_MODERATION_ROUTING_FRONTEND.md`
- Modify: `tasks/stage.md`
- Modify: `frontend/tasks/stage.md`

- [ ] **Step 1: Add backend task file**

Create `tasks/TASK_62_COMMUNITY_AUTO_MODERATION_ROUTING_BACKEND.md` with:

```markdown
# TASK_62 社区自动审核分流后端治理

## 当前任务所属模块

社区内容安全治理。目标是在现有先审后发基础上增加自动分流：严重违规直接拒绝，疑似风险或图片内容进入人工审核，低风险纯文本自动通过，降低管理员处理明显正常文本内容的压力。

## 前端文件定位

- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/components/community/ShareReportDialog.vue`
- `frontend/app/src/components/community/CommentSection.vue`
- `frontend/app/src/api/community.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
- `server/src/main/java/com/airesume/server/service/CommunityModerationDecision.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/dto/community/CreateCommunityContentResponse.java`

## 本轮修改文件清单

- 新增：`server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
- 新增：`server/src/main/java/com/airesume/server/service/CommunityModerationDecision.java`
- 新增：`server/src/main/java/com/airesume/server/dto/community/CreateCommunityContentResponse.java`
- 修改：`server/src/main/java/com/airesume/server/service/CommunityService.java`
- 修改：`server/src/main/java/com/airesume/server/controller/CommunityController.java`
- 新增：`server/src/test/java/com/airesume/server/service/CommunityTextModerationServiceTest.java`
- 修改：`server/src/test/java/com/airesume/server/service/CommunityServiceModerationTest.java`

## 后端实现方案

- 将原先散落在 `CommunityService` 的文本规则抽到 `CommunityTextModerationService`。
- 严重违规文本继续在入库前拒绝，不进入公开区和审核队列。
- 疑似风险文本写入 `pending`，并写入通用复核原因。
- 带图片的帖子和评论继续写入 `pending`，避免未做图片识别时自动公开。
- 低风险纯文本写入 `approved`，直接公开展示。
- 评论只有在自动通过或人工通过时才计入帖子评论数。
- 创建帖子和评论接口返回 `{ id, reviewStatus }`，供前端展示准确提示。

## 数据存储方案

本轮复用 TASK_61 已新增的 `review_status` 和 `review_reason` 字段，不新增表、不新增字段、不新增迁移脚本。

## 编译结果

记录实际执行结果：

```bash
mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest,AdminCommunityModerationServiceTest" test
mvn.cmd -q "-Dtest=CommunityService*Test,AdminCommunityModerationServiceTest" test
mvn.cmd -q -DskipTests compile
```

## 当前功能验收说明

- 明显违规文本仍会被直接拒绝。
- 图片内容不会自动公开，继续进入人工审核。
- 疑似广告或引流文本进入人工审核。
- 普通低风险纯文本内容自动通过，减少管理员审核量。
- 前端可根据后端返回的审核状态显示准确提示。

## 停止，不继续下一个功能

本轮只完成社区自动审核分流一期，不接入云审核、图片 AI 鉴黄、举报系统、批量审核或敏感词后台配置。
```

- [ ] **Step 2: Add frontend task file**

Create `frontend/tasks/TASK_62_COMMUNITY_AUTO_MODERATION_ROUTING_FRONTEND.md` with:

```markdown
# TASK_62 社区自动审核分流前端适配

## 当前任务所属模块

社区内容安全治理前端适配。目标是让发帖、分享报告、评论和回复根据后端返回的 `reviewStatus` 显示准确提示，并避免待审评论被本地乐观插入到公开评论区。

## 前端文件定位

- `frontend/app/src/api/community.js`
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/components/community/ShareReportDialog.vue`
- `frontend/app/src/components/community/CommentSection.vue`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/dto/community/CreateCommunityContentResponse.java`

## 本轮修改文件清单

- 修改：`frontend/app/src/api/community.js`
- 修改：`frontend/app/src/components/community/PostEditor.vue`
- 修改：`frontend/app/src/components/community/ShareReportDialog.vue`
- 修改：`frontend/app/src/components/community/CommentSection.vue`
- 修改或新增：`frontend/app/src/__tests__/components/community/PostEditor.test.js`
- 修改或新增：`frontend/app/src/__tests__/components/community/CommentSection.test.js`

## 前端实现方案

- 发帖返回 `approved` 时提示“发布成功，已公开展示”。
- 发帖返回 `pending` 时继续提示“已提交审核，通过后将在社区展示”。
- 分享报告同样根据 `reviewStatus` 展示公开或待审提示。
- 评论或回复返回 `approved` 时保留本地乐观插入和计数更新。
- 评论或回复返回 `pending` 时只清空输入并提示待审，不把内容插入公开评论区。

## 后端实现方案

后端创建接口返回 `{ id, reviewStatus }`：

- `POST /api/community/posts`
- `POST /api/community/posts/{postId}/comments`

## 数据存储方案

前端不新增本地持久化。审核状态来自后端创建接口响应。

## 构建结果

记录实际执行结果：

```bash
npm.cmd test -- --run src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/CommentSection.test.js
npm.cmd run build
```

## 当前功能验收说明

- 用户能区分内容已公开还是等待审核。
- 待审评论不会短暂出现在当前用户本地评论区，避免和真实公开状态不一致。
- 已自动通过的低风险评论仍能即时出现在评论区。

## 停止，不继续下一个功能

本轮只完成自动审核分流的前端状态适配，不继续扩展举报中心、批量审核、图片预审、敏感词配置页或云审核接入。
```

- [ ] **Step 3: Update backend stage**

Prepend a new section to `tasks/stage.md`:

```markdown
## 社区自动审核分流后端治理（2026-05-31）

## 已完成且已验证的功能

- 社区文本审核规则已从 `CommunityService` 抽为 `CommunityTextModerationService`。
- 严重违规文本在入库前拒绝；疑似风险文本进入 `pending`；带图片内容继续进入 `pending`；低风险纯文本自动 `approved`。
- 创建帖子和评论接口已返回 `{ id, reviewStatus }`，前端可按真实状态提示用户。
- 评论计数仍只统计审核通过评论，待审评论不会计入公开评论数。

## 本轮完成状态

- 填写实际 Maven 测试和编译命令结果。

## 尚未开始的功能

- 未接入云审核、图片 AI 鉴黄、举报中心、敏感词配置后台、批量审核或多级审核流程。

## 停止，不继续下一个功能

当前仅完成社区自动审核分流一期，等待验收，不继续下一个功能。
```

- [ ] **Step 4: Update frontend stage**

Prepend a new section to `frontend/tasks/stage.md`:

```markdown
## 社区自动审核分流前端适配（2026-05-31）
- 当前阶段：本轮已完成发帖、分享报告、评论和回复对创建接口 `reviewStatus` 的状态化提示。
- 已完成内容：发帖和分享报告根据 `approved/pending` 展示公开或待审提示；评论和回复只有在 `approved` 时才本地插入并更新计数，`pending` 时只提示待审并清空输入。
- 后端联动：前端使用后端创建接口返回的 `{ id, reviewStatus }`；自动分流规则和后端验证见 `tasks/stage.md`。
- 前端验证：填写实际 Vitest 和构建命令结果。
- 关联任务文件：`frontend/tasks/TASK_62_COMMUNITY_AUTO_MODERATION_ROUTING_FRONTEND.md`、`tasks/TASK_62_COMMUNITY_AUTO_MODERATION_ROUTING_BACKEND.md`。
- 停止说明：本轮只完成自动审核分流状态适配，不继续推进举报系统、批量审核、图片 AI 审核、敏感词配置页或云审核服务。
```

---

### Task 6: Full Verification

**Files:**
- No new files; verification only.

- [ ] **Step 1: Run backend focused regression**

Run:

```bash
cd F:\Code\ai-resume\server
mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest,AdminCommunityModerationServiceTest" test
```

Expected: PASS.

- [ ] **Step 2: Run broader backend community regression**

Run:

```bash
cd F:\Code\ai-resume\server
mvn.cmd -q "-Dtest=CommunityService*Test,AdminCommunityModerationServiceTest,CriticalEndpointRateLimitFilterTest" test
```

Expected: PASS.

- [ ] **Step 3: Compile backend**

Run:

```bash
cd F:\Code\ai-resume\server
mvn.cmd -q -DskipTests compile
```

Expected: PASS.

- [ ] **Step 4: Run frontend focused regression**

Run:

```bash
cd F:\Code\ai-resume\frontend\app
npm.cmd test -- --run src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/CommentSection.test.js
```

Expected: PASS.

- [ ] **Step 5: Build frontend**

Run:

```bash
cd F:\Code\ai-resume\frontend\app
npm.cmd run build
```

Expected: PASS.

---

## Self-Review

Spec coverage:
- Reduces administrator workload: low-risk text-only posts/comments auto-approve.
- Keeps automatic protection: severe terms are rejected before persistence.
- Preserves safety for images: image content remains pending manual review.
- Keeps public visibility boundary: only `approved` content is public.
- Updates frontend messages so users do not see stale "pending" language for auto-approved content.

Placeholder scan:
- No implementation task contains `TBD`, `TODO`, or "fill in later".
- Documentation templates include explicit text and only require actual command results after execution.

Type consistency:
- `CreateCommunityContentResponse.id` is `Long`.
- `reviewStatus` consistently uses existing constants: `approved` and `pending`.
- Frontend reads `res.data.id` and `res.data.reviewStatus`.
