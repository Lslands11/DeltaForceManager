<template>
  <div class="page-container">
    <div class="page-header">
      <h2>报表统计</h2>
      <p>查看余额趋势和利润分析</p>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- Daily Trend -->
      <el-tab-pane label="每日趋势" name="daily">
        <div class="card" style="margin-bottom: 16px;">
          <el-form inline>
            <el-form-item label="账号">
              <el-select v-model="dailyQuery.accountId" placeholder="全部账号" clearable style="width: 200px;">
                <el-option v-for="a in accountOptions" :key="a.id" :label="a.accountName" :value="a.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="日期范围">
              <el-date-picker
                v-model="dailyQuery.dateRange"
                type="daterange"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 280px;"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadDailyTrend">查询</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="card" style="margin-bottom: 16px;">
          <div ref="dailyChartRef" style="height: 350px;"></div>
        </div>

        <div class="card">
          <el-table :data="dailyData" stripe>
            <el-table-column prop="date" label="日期" width="120" />
            <el-table-column prop="accountName" label="账号" min-width="120" />
            <el-table-column label="开盘余额" min-width="120">
              <template #default="{ row }">{{ formatMoney(row.openBalance) }}</template>
            </el-table-column>
            <el-table-column label="收盘余额" min-width="120">
              <template #default="{ row }">{{ formatMoney(row.closeBalance) }}</template>
            </el-table-column>
            <el-table-column label="日利润" min-width="100">
              <template #default="{ row }">
                <span :class="row.dailyProfit >= 0 ? 'text-success' : 'text-danger'">
                  {{ row.dailyProfit >= 0 ? '+' : '' }}{{ formatMoney(row.dailyProfit) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="recordCount" label="记录数" width="80" />
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Profit Summary -->
      <el-tab-pane label="利润报表" name="profit">
        <div class="card" style="margin-bottom: 16px;">
          <el-form inline>
            <el-form-item label="周期">
              <el-radio-group v-model="profitQuery.period" @change="loadProfit">
                <el-radio-button value="week">按周</el-radio-button>
                <el-radio-button value="month">按月</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button @click="profitQuery.offset++; loadProfit()">上一{{ profitQuery.period === 'week' ? '周' : '月' }}</el-button>
              <el-button @click="profitQuery.offset = Math.max(0, profitQuery.offset - 1); loadProfit()">下一{{ profitQuery.period === 'week' ? '周' : '月' }}</el-button>
              <el-button type="primary" @click="profitQuery.offset = 0; loadProfit()">当前</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="grid-4" style="margin-bottom: 16px;">
          <div class="card stat-card">
            <span class="label">{{ profitData.periodLabel || '-' }}</span>
            <span class="label">总利润</span>
            <span class="value" :class="(profitData.totalProfit || 0) >= 0 ? 'positive' : 'negative'">
              {{ (profitData.totalProfit || 0) >= 0 ? '+' : '' }}{{ formatMoney(profitData.totalProfit) }}
            </span>
          </div>
        </div>

        <div class="card">
          <div ref="profitChartRef" style="height: 300px; margin-bottom: 16px;"></div>
          <el-table :data="profitData.accounts || []" stripe>
            <el-table-column prop="accountName" label="账号" min-width="120" />
            <el-table-column label="期初余额" min-width="120">
              <template #default="{ row }">{{ formatMoney(row.startBalance) }}</template>
            </el-table-column>
            <el-table-column label="期末余额" min-width="120">
              <template #default="{ row }">{{ formatMoney(row.endBalance) }}</template>
            </el-table-column>
            <el-table-column label="利润" min-width="100">
              <template #default="{ row }">
                <span :class="row.profit >= 0 ? 'text-success' : 'text-danger'">
                  {{ row.profit >= 0 ? '+' : '' }}{{ formatMoney(row.profit) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="recordCount" label="记录数" width="80" />
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Account Trend -->
      <el-tab-pane label="账号走势" name="trend">
        <div class="card" style="margin-bottom: 16px;">
          <el-form inline>
            <el-form-item label="账号" required>
              <el-select v-model="trendQuery.accountId" placeholder="选择账号" style="width: 200px;">
                <el-option v-for="a in accountOptions" :key="a.id" :label="a.accountName" :value="a.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="天数">
              <el-select v-model="trendQuery.days" style="width: 100px;">
                <el-option :value="7" label="7天" />
                <el-option :value="14" label="14天" />
                <el-option :value="30" label="30天" />
                <el-option :value="90" label="90天" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadAccountTrend" :disabled="!trendQuery.accountId">查询</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="card">
          <div ref="trendChartRef" style="height: 400px;"></div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDailyTrend, getProfitSummary, getAccountTrend } from '../../api/report'
import { getAccountList } from '../../api/account'

const activeTab = ref('daily')
const accountOptions = ref([])

// Daily Trend
const dailyQuery = ref({
  accountId: null,
  dateRange: (() => {
    const end = new Date()
    const start = new Date()
    start.setDate(end.getDate() - 7)
    return [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
  })()
})
const dailyData = ref([])
const dailyChartRef = ref(null)
let dailyChart = null

// Profit
const profitQuery = ref({ period: 'week', offset: 0 })
const profitData = ref({})
const profitChartRef = ref(null)
let profitChart = null

// Account Trend
const trendQuery = ref({ accountId: null, days: 30 })
const trendChartRef = ref(null)
let trendChart = null

function formatMoney(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(d) {
  return d.toISOString().split('T')[0]
}

async function loadAccounts() {
  try {
    const res = await getAccountList({ pageNo: 1, pageSize: 100 })
    accountOptions.value = res.result.records || []
  } catch (e) { console.error(e) }
}

async function loadDailyTrend() {
  if (!dailyQuery.value.dateRange || dailyQuery.value.dateRange.length !== 2) return
  try {
    const res = await getDailyTrend({
      accountId: dailyQuery.value.accountId,
      startDate: dailyQuery.value.dateRange[0],
      endDate: dailyQuery.value.dateRange[1]
    })
    dailyData.value = res.result || []
    await nextTick()
    renderDailyChart(dailyData.value)
  } catch (e) { console.error(e) }
}

function renderDailyChart(data) {
  if (!dailyChartRef.value) return
  if (!dailyChart) dailyChart = echarts.init(dailyChartRef.value)

  const dates = [...new Set(data.map(d => d.date))]
  const accountMap = {}
  data.forEach(d => {
    if (!accountMap[d.accountName]) accountMap[d.accountName] = {}
    accountMap[d.accountName][d.date] = Number(d.dailyProfit)
  })

  const series = Object.entries(accountMap).map(([name, dateMap]) => ({
    name,
    type: 'bar',
    data: dates.map(d => dateMap[d] || 0)
  }))

  dailyChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: Object.keys(accountMap), top: 0 },
    grid: { top: 30, right: 20, bottom: 30, left: 60 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series
  }, true)
}

async function loadProfit() {
  try {
    const res = await getProfitSummary(profitQuery.value)
    profitData.value = res.result || {}
    await nextTick()
    renderProfitChart(profitData.value.accounts || [])
  } catch (e) { console.error(e) }
}

function renderProfitChart(accounts) {
  if (!profitChartRef.value) return
  if (!profitChart) profitChart = echarts.init(profitChartRef.value)

  profitChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { top: 20, right: 20, bottom: 30, left: 60 },
    xAxis: {
      type: 'category',
      data: accounts.map(a => a.accountName)
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: accounts.map(a => ({
        value: Number(a.profit),
        itemStyle: { color: a.profit >= 0 ? '#10b981' : '#ef4444' }
      }))
    }]
  }, true)
}

async function loadAccountTrend() {
  if (!trendQuery.value.accountId) return
  try {
    const res = await getAccountTrend(trendQuery.value)
    const data = res.result || {}
    await nextTick()
    renderTrendChart(data)
  } catch (e) { console.error(e) }
}

function renderTrendChart(data) {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  const points = data.points || []
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { top: 20, right: 20, bottom: 30, left: 60 },
    xAxis: {
      type: 'category',
      data: points.map(p => p.datetime),
      axisLabel: { rotate: 30 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: v => (v / 10000).toFixed(1) + 'w' }
    },
    series: [{
      name: data.accountName || '余额',
      type: 'line',
      smooth: true,
      data: points.map(p => Number(p.balance)),
      areaStyle: { opacity: 0.1 }
    }]
  }, true)
}

function handleTabChange(tab) {
  nextTick(() => {
    if (tab === 'daily') dailyChart?.resize()
    if (tab === 'profit') profitChart?.resize()
    if (tab === 'trend') trendChart?.resize()
  })
}

onMounted(async () => {
  await loadAccounts()
  await loadDailyTrend()
  window.addEventListener('resize', () => {
    dailyChart?.resize()
    profitChart?.resize()
    trendChart?.resize()
  })
})

onBeforeUnmount(() => {
  dailyChart?.dispose()
  profitChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.text-success { color: var(--success); font-weight: 600; }
.text-danger { color: var(--danger); font-weight: 600; }
.value.positive { color: var(--success); }
.value.negative { color: var(--danger); }
</style>
