import request from '../utils/request'

export function getScreenshotList(params) {
  return request.get('/screenshot/list', { params })
}

export function getPendingReview() {
  return request.get('/screenshot/pendingReview')
}

export function reviewScreenshot(id, amount) {
  return request.put(`/screenshot/review/${id}`, null, { params: { amount } })
}

export function reprocessScreenshot(id) {
  return request.post(`/screenshot/reprocess/${id}`)
}
