<template>
  <div class="page-container">
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: center;">
      <div>
        <h2>余额记录</h2>
        <p>查看和管理余额变动记录</p>
      </div>
      <el-button type="primary" @click="dialogVisible = true">
        <el-icon><EditPen /></el-icon>手动录入
      </el-button>
    </div>

    <!-- Filters -->
    <div class="card" style="margin-bottom: 16px;">
      <el-form inline>
        <el-form-item label="账号">
          <el-select v-model="query.accountId" placeholder="全部账号" clearable style="width: 200px;" @change="loadData">
            <el-option v-for="a in accountOptions" :key="a.id" :label="a.accountName" :value="a.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <!-- Table -->
    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="账号" min-width="120">
          <template #default="{ row }">
            {{ accountMap[row.accountId] || row.accountId }}
          </template>
        </el-table-column>
        <el-table-column label="余额" min-width="120">
          <template #default="{ row }">
            <span style="font-weight: 600;">{{ formatMoney(row.balance) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="变动" min-width="100">
          <template #default="{ row }">
            <span :class="row.balanceChange >= 0 ? 'text-success' : 'text-danger'">
              {{ row.balanceChange >= 0 ? '+' : '' }}{{ formatMoney(row.balanceChange) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="sourceTagType(row.source)" size="small">{{ sourceLabel(row.source) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordTime" label="记录时间" width="170" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-popconfirm title="确定删除该记录？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- Manual Input Dialog -->
    <el-dialog v-model="dialogVisible" title="手动录入余额" width="420px">
      <el-form :model="manualForm" label-width="80px">
        <el-form-item label="账号" required>
          <el-select v-model="manualForm.accountId" placeholder="选择账号" style="width: 100%;">
            <el-option v-for="a in accountOptions" :key="a.id" :label="a.accountName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="余额" required>
          <el-input-number v-model="manualForm.balance" :precision="2" :step="100" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleManualInput" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getBalanceList, manualInput, deleteBalance } from '../../api/balance'
import { getAccountList } from '../../api/account'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const accountOptions = ref([])

const query = ref({ pageNo: 1, pageSize: 10, accountId: null })
const manualForm = ref({ accountId: null, balance: 0 })

const accountMap = computed(() => {
  const map = {}
  accountOptions.value.forEach(a => { map[a.id] = a.accountName })
  return map
})

function formatMoney(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function sourceLabel(s) {
  return { 1: 'OCR识别', 2: '手动录入', 3: '人工校正' }[s] || '未知'
}

function sourceTagType(s) {
  return { 1: 'success', 2: 'warning', 3: 'info' }[s] || ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await getBalanceList(query.value)
    tableData.value = res.result.records || []
    total.value = res.result.total || 0
  } finally {
    loading.value = false
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

async function handleManualInput() {
  if (!manualForm.value.accountId) {
    ElMessage.warning('请选择账号')
    return
  }
  submitting.value = true
  try {
    await manualInput(manualForm.value)
    ElMessage.success('录入成功')
    dialogVisible.value = false
    manualForm.value = { accountId: null, balance: 0 }
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteBalance(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadData()
  loadAccounts()
})
</script>

<style scoped>
.text-success { color: var(--success); font-weight: 600; }
.text-danger { color: var(--danger); font-weight: 600; }
</style>
