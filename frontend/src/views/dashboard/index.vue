<template>
  <div class="page-container">
    <div class="page-header">
      <h2>总览面板</h2>
      <p>实时监控所有账号的余额状态</p>
    </div>

    <!-- Stats Cards -->
    <div class="grid-4" style="margin-bottom: 24px;">
      <div class="card stat-card">
        <span class="label">总余额</span>
        <span class="value">{{ formatMoney(summary.totalBalance) }}</span>
      </div>
      <div class="card stat-card">
        <span class="label">今日盈亏</span>
        <span class="value" :class="summary.totalDailyProfit >= 0 ? 'positive' : 'negative'">
          {{ summary.totalDailyProfit >= 0 ? '+' : '' }}{{ formatMoney(summary.totalDailyProfit) }}
        </span>
      </div>
      <div class="card stat-card">
        <span class="label">在线账号</span>
        <span class="value">{{ onlineCount }}</span>
      </div>
      <div class="card stat-card">
        <span class="label">总账号数</span>
        <span class="value">{{ summary.accounts?.length || 0 }}</span>
      </div>
    </div>

    <!-- Account Cards -->
    <div class="grid-3">
      <div
        v-for="account in summary.accounts"
        :key="account.accountId"
        class="card account-card"
      >
        <div class="account-header">
          <div>
            <div class="account-name">{{ account.accountName }}</div>
            <div class="account-device">{{ account.deviceModel || '-' }}</div>
          </div>
          <el-tag :type="account.status === 'online' ? 'success' : 'info'" size="small">
            {{ account.status === 'online' ? '在线' : '离线' }}
          </el-tag>
        </div>
        <div class="account-balance">
          {{ formatMoney(account.currentBalance) }}
        </div>
        <div class="account-change" :class="account.dailyChange >= 0 ? 'positive' : 'negative'">
          今日 {{ account.dailyChange >= 0 ? '+' : '' }}{{ formatMoney(account.dailyChange) }}
        </div>
        <div class="account-update">
          最后更新: {{ formatTime(account.lastUpdateTime) }}
        </div>
      </div>
    </div>

    <!-- Trend Chart -->
    <div class="card" style="margin-top: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <h3 style="font-size: 1rem; font-weight: 600;">余额走势 (近7天)</h3>
        <el-select v-model="trendAccountId" placeholder="全部账号" clearable size="small" style="width: 160px;" @change="loadTrend">
          <el-option
            v-for="a in accountOptions"
            :key="a.id"
            :label="a.accountName"
            :value="a.id"
          />
        </el-select>
      </div>
      <div ref="trendChartRef" style="height: 300px;"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getMultiAccountSummary } from '../../api/report'
import { getDailyTrend } from '../../api/report'
import { getAccountList } from '../../api/account'

const summary = ref({ totalBalance: 0, totalDailyProfit: 0, accounts: [] })
const trendAccountId = ref(null)
const trendChartRef = ref(null)
const accountOptions = ref([])
let chart = null

const onlineCount = computed(() =>
  (summary.value.accounts || []).filter(a => a.status === 'online').length
)

function formatMoney(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(date) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

async function loadSummary() {
  try {
    const res = await getMultiAccountSummary()
    summary.value = res.result
  } catch (e) {
    console.error(e)
  }
}

async function loadAccounts() {
  try {
    const res = await getAccountList({ pageNo: 1, pageSize: 100 })
    accountOptions.value = res.result.records || []
  } catch (e) {
    console.error(e)
  }
}

async function loadTrend() {
  try {
    const endDate = new Date()
    const startDate = new Date()
    startDate.setDate(endDate.getDate() - 7)

    const params = {
      startDate: formatDate(startDate),
      endDate: formatDate(endDate)
    }
    if (trendAccountId.value) {
      params.accountId = trendAccountId.value
    }

    const res = await getDailyTrend(params)
    renderChart(res.result || [])
  } catch (e) {
    console.error(e)
  }
}

function formatDate(d) {
  return d.toISOString().split('T')[0]
}

function renderChart(data) {
  if (!trendChartRef.value) return
  if (!chart) {
    chart = echarts.init(trendChartRef.value)
  }

  const dates = [...new Set(data.map(d => d.date))]
  const accountMap = {}
  data.forEach(d => {
    if (!accountMap[d.accountName]) accountMap[d.accountName] = {}
    accountMap[d.accountName][d.date] = d.closeBalance
  })

  const series = Object.entries(accountMap).map(([name, dateMap]) => ({
    name,
    type: 'line',
    smooth: true,
    data: dates.map(d => dateMap[d] != null ? Number(dateMap[d]) : null),
    symbolSize: 6
  }))

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: Object.keys(accountMap), top: 0 },
    grid: { top: 30, right: 20, bottom: 30, left: 60 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', axisLabel: { formatter: (v) => (v / 10000).toFixed(1) + 'w' } },
    series
  }, true)
}

onMounted(async () => {
  await Promise.all([loadSummary(), loadAccounts()])
  await loadTrend()
  window.addEventListener('resize', () => chart?.resize())
})

onBeforeUnmount(() => {
  chart?.dispose()
  window.removeEventListener('resize', () => chart?.resize())
})
</script>

<style scoped>
.account-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.account-card:hover {
  transform: translateY(-2px);
}

.account-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.account-name {
  font-weight: 600;
  font-size: 1rem;
}

.account-device {
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin-top: 2px;
}

.account-balance {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 4px;
}

.account-change {
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 8px;
}

.account-change.positive { color: var(--success); }
.account-change.negative { color: var(--danger); }
.value.positive { color: var(--success); }
.value.negative { color: var(--danger); }

.account-update {
  font-size: 0.75rem;
  color: var(--text-secondary);
}
</style>
