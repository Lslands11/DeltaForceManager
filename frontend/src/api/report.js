import request from '../utils/request'

export function getDailyTrend(params) {
  return request.get('/report/dailyTrend', { params })
}

export function getMultiAccountSummary() {
  return request.get('/report/multiAccountSummary')
}

export function getAccountTrend(params) {
  return request.get('/report/accountTrend', { params })
}

export function getProfitSummary(params) {
  return request.get('/report/profitSummary', { params })
}
