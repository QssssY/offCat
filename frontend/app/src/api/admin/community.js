import adminRequest from '@/utils/adminRequest'

export function getAdminCommunityPosts(params = {}) {
  return adminRequest({
    url: '/api/admin/community/posts',
    method: 'get',
    params: { page: 1, size: 20, ...params }
  })
}

export function getAdminCommunityComments(params = {}) {
  return adminRequest({
    url: '/api/admin/community/comments',
    method: 'get',
    params: { page: 1, size: 20, ...params }
  })
}

export function reviewAdminCommunityPost(id, data) {
  return adminRequest({
    url: `/api/admin/community/posts/${encodeURIComponent(id)}/review`,
    method: 'put',
    data
  })
}

export function reviewAdminCommunityComment(id, data) {
  return adminRequest({
    url: `/api/admin/community/comments/${encodeURIComponent(id)}/review`,
    method: 'put',
    data
  })
}
