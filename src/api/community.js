import request from '@/utils/request'

/**
 * 获取帖子列表
 * @param {Object} params
 * @param {string} params.category - 板块: 'interview_exp' | 'referral'，空为全部
 * @param {string} params.sort - 排序: 'latest' | 'hot'
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页条数
 */
export function getPostList(params) {
  return request({
    url: '/api/community/posts',
    method: 'get',
    params
  })
}

/**
 * 获取帖子详情
 * @param {number|string} postId
 */
export function getPostDetail(postId) {
  return request({
    url: `/api/community/posts/${postId}`,
    method: 'get'
  })
}

/**
 * 发布帖子
 * @param {Object} data
 * @param {string} data.category - 板块
 * @param {string} data.title - 帖子标题
 * @param {string} data.content - 内容
 * @param {string[]} data.images - 图片URL数组
 * @param {string} [data.sharedInterviewSessionId] - 分享面试报告时关联的会话ID
 */
export function createPost(data) {
  return request({
    url: '/api/community/posts',
    method: 'post',
    data
  })
}

/**
 * 上传帖子图片
 * @param {File} file
 */
export function uploadPostImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/community/images/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 点赞/取消点赞帖子
 * @param {number|string} postId
 */
export function togglePostLike(postId) {
  return request({
    url: `/api/community/posts/${postId}/like`,
    method: 'post'
  })
}

/**
 * 收藏/取消收藏帖子
 * @param {number|string} postId
 */
export function togglePostFavorite(postId) {
  return request({
    url: `/api/community/posts/${postId}/favorite`,
    method: 'post'
  })
}

/**
 * 获取帖子评论列表
 * @param {number|string} postId
 * @param {Object} params
 * @param {number} params.pageNum
 * @param {number} params.pageSize
 */
export function getComments(postId, params) {
  return request({
    url: `/api/community/posts/${postId}/comments`,
    method: 'get',
    params
  })
}

/**
 * 发表评论
 * @param {number|string} postId
 * @param {Object} data
 * @param {string} data.content
 * @param {number|string} [data.parentCommentId] - 父评论ID（回复时传入）
 */
export function createComment(postId, data) {
  return request({
    url: `/api/community/posts/${postId}/comments`,
    method: 'post',
    data
  })
}

export function getCommentDetail(postId, commentId) {
  return request({
    url: `/api/community/posts/${postId}/comments/${commentId}/detail`,
    method: 'get'
  })
}

/**
 * 获取评论的回复列表
 * @param {number|string} postId
 * @param {number|string} commentId
 * @param {Object} params
 * @param {number} params.pageNum
 * @param {number} params.pageSize
 */
export function getReplies(postId, commentId, params) {
  return request({
    url: `/api/community/posts/${postId}/comments/${commentId}/replies`,
    method: 'get',
    params
  })
}

/**
 * 删除评论
 * @param {number|string} postId
 * @param {number|string} commentId
 */
export function deleteComment(postId, commentId) {
  return request({
    url: `/api/community/posts/${postId}/comments/${commentId}`,
    method: 'delete'
  })
}

/**
 * 获取当前用户的帖子列表
 * @param {Object} params
 * @param {number} params.pageNum
 * @param {number} params.pageSize
 */
export function getMyPosts(params) {
  return request({
    url: '/api/community/posts',
    method: 'get',
    params: { ...params, mine: true }
  })
}

/**
 * 获取当前用户点赞过的帖子列表
 * @param {Object} params
 * @param {number} params.pageNum
 * @param {number} params.pageSize
 */
export function getLikedPosts(params) {
  return request({
    url: '/api/community/posts',
    method: 'get',
    params: { ...params, filter: 'liked' }
  })
}

/**
 * 获取当前用户收藏的帖子列表
 * @param {Object} params
 * @param {number} params.pageNum
 * @param {number} params.pageSize
 */
export function getFavoritedPosts(params) {
  return request({
    url: '/api/community/posts',
    method: 'get',
    params: { ...params, filter: 'favorited' }
  })
}

/**
 * 获取当前用户评论过的帖子列表
 * @param {Object} params
 * @param {number} params.pageNum
 * @param {number} params.pageSize
 */
export function getCommentedPosts(params) {
  return request({
    url: '/api/community/posts',
    method: 'get',
    params: { ...params, filter: 'commented' }
  })
}

/**
 * 获取当前用户发布的评论列表（附带所属帖子信息）
 * @param {Object} params
 * @param {number} params.pageNum
 * @param {number} params.pageSize
 */
export function getMyComments(params) {
  return request({
    url: '/api/community/my/comments',
    method: 'get',
    params
  })
}

/**
 * 删除帖子
 * @param {number|string} postId
 */
export function deletePost(postId) {
  return request({
    url: `/api/community/posts/${postId}`,
    method: 'delete'
  })
}

/**
 * 获取当前用户收到的互动信息（别人对我帖子的点赞和评论）
 * @param {Object} params
 * @param {number} params.pageNum
 * @param {number} params.pageSize
 */
export function getMyInteractions(params) {
  return request({
    url: '/api/community/my/interactions',
    method: 'get',
    params
  })
}

/**
 * 获取未读互动数量
 * @param {string} since - ISO格式的时间戳，上次查看时间
 */
export function getInteractionUnreadCount(since) {
  return request({
    url: '/api/community/my/interactions/unread-count',
    method: 'get',
    params: { since }
  })
}
