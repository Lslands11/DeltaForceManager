<template>
  <div class="page-container">
    <div class="page-header">
      <h2>总览面板</h2>
      <p>实时监控所有账号的余额状态</p>
    </div>

    <!-- Stats Cards -->
    <div class="grid-4 section-gap">
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
    <div class="grid-3 section-gap">
      <div
        v-for="account in summary.accounts"
        :key="account.accountId"
        class="card card-interactive account-card"
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
    <div class="card">
      <div class="chart-header">
        <h3 class="chart-title">余额走势 (近7天)</h3>
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
import { useCurrency } from '../../composables/useCurrency'

const { isRmb, formatMoney, toRmb } = useCurrency()

const summary = ref({ totalBalance: 0, totalDailyProfit: 0, accounts: [] })
const trendAccountId = ref(null)
const trendChartRef = ref(null)
const accountOptions = ref([])
let chart = null

const onlineCount = computed(() =>
  (summary.value.accounts || []).filter(a => a.status === 'online').length
)

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
    data: dates.map(d => dateMap[d] != null ? (isRmb.value ? toRmb(dateMap[d]) : Number(dateMap[d])) : null),
    symbolSize: 6
  }))

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: Object.keys(accountMap), top: 0 },
    grid: { top: 30, right: 20, bottom: 30, left: 60 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', axisLabel: { formatter: (v) => formatMoney(v) } },
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
  padding: var(--space-lg);
}

.account-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-md);
}

.account-name {
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-md);
  color: var(--color-text);
}

.account-device {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: 2px;
}

.account-balance {
  font-size: 1.5rem;
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--space-xs);
  color: var(--color-text);
}

.account-change {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  margin-bottom: var(--space-sm);
}

.account-change.positive { color: var(--color-success); }
.account-change.negative { color: var(--color-danger); }
.value.positive { color: var(--color-success); }
.value.negative { color: var(--color-danger); }

.account-update {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-md);
}

.chart-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
}
</style>
