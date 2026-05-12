<template>
  <div class="page-container">
    <div class="page-header">
      <h2>截图日志</h2>
      <p>查看截图上传和 OCR 识别记录</p>
    </div>

    <!-- Pending Review Alert -->
    <el-alert
      v-if="pendingCount > 0"
      :title="`有 ${pendingCount} 条截图待审核`"
      type="warning"
      show-icon
      :closable="false"
      class="section-gap"
    >
      <template #default>
        <el-button type="warning" size="small" @click="showPending = true">查看待审核</el-button>
      </template>
    </el-alert>

    <!-- Filters -->
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="账号">
          <el-select v-model="query.accountId" placeholder="全部账号" clearable style="width: 200px;" @change="loadData">
            <el-option v-for="a in accountOptions" :key="a.id" :label="a.accountName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.ocrStatus" placeholder="全部状态" clearable style="width: 140px;" @change="loadData">
            <el-option label="待处理" :value="0" />
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="2" />
            <el-option label="待审核" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="uploadDialogVisible = true">上传截图</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Table -->
    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="账号" min-width="120">
          <template #default="{ row }">
            {{ accountMap[row.accountId] || row.accountId }}
          </template>
        </el-table-column>
        <el-table-column label="OCR状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.ocrStatus)" size="small">{{ statusLabel(row.ocrStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="识别金额" min-width="100">
          <template #default="{ row }">
            {{ row.parsedAmount != null ? formatMoney(row.parsedAmount) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="置信度" width="90">
          <template #default="{ row }">
            {{ row.ocrConfidence != null ? row.ocrConfidence + '%' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="人工金额" min-width="100">
          <template #default="{ row }">
            {{ row.manualAmount != null ? formatMoney(row.manualAmount) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="ocrRawText" label="OCR原文" min-width="150" show-overflow-tooltip />
        <el-table-column prop="uploadTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.ocrStatus === 3"
              link type="primary" size="small"
              @click="openReview(row)"
            >审核</el-button>
            <el-button
              v-if="row.ocrStatus === 2 || row.ocrStatus === 3"
              link type="warning" size="small"
              @click="handleReprocess(row.id)"
            >重试</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
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

    <!-- Pending Review Drawer -->
    <el-drawer v-model="showPending" title="待审核截图" size="500px">
      <div v-if="pendingList.length === 0" class="empty-state">
        暂无待审核记录
      </div>
      <div v-for="item in pendingList" :key="item.id" class="pending-item">
        <div class="pending-info">
          <div><strong>ID:</strong> {{ item.id }}</div>
          <div><strong>OCR识别:</strong> {{ item.parsedAmount || '-' }}</div>
          <div><strong>置信度:</strong> {{ item.ocrConfidence || '-' }}%</div>
          <div><strong>原文:</strong> {{ item.ocrRawText || '-' }}</div>
          <div><strong>上传时间:</strong> {{ item.uploadTime }}</div>
        </div>
        <el-button type="primary" size="small" @click="openReview(item)">审核</el-button>
      </div>
    </el-drawer>

    <!-- Upload Dialog -->
    <el-dialog v-model="uploadDialogVisible" title="上传截图" width="460px" @close="resetUploadForm">
      <el-form label-width="80px">
        <el-form-item label="账号" required>
          <el-select v-model="uploadForm.accountId" placeholder="请选择账号" style="width: 100%;">
            <el-option v-for="a in accountOptions" :key="a.id" :label="a.accountName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截图" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="uploadForm.fileList"
          >
            <el-button type="primary">选择图片</el-button>
            <template #tip>
              <div style="color: var(--color-text-muted); font-size: var(--font-size-xs); margin-top: var(--space-xs);">
                支持 jpg/png 格式，最大 20MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">上传并解析</el-button>
      </template>
    </el-dialog>

    <!-- Review Dialog -->
    <el-dialog v-model="reviewVisible" title="人工审核" width="400px">
      <p style="margin-bottom: var(--space-md); color: var(--color-text-secondary);">
        截图ID: {{ reviewItem?.id }}，OCR识别: {{ reviewItem?.parsedAmount || '-' }}
      </p>
      <el-form label-width="80px">
        <el-form-item label="正确金额" required>
          <el-input-number v-model="reviewAmount" :precision="2" :step="100" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReview" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getScreenshotList, getPendingReview, reviewScreenshot, reprocessScreenshot, uploadScreenshot } from '../../api/screenshot'
import { getAccountList } from '../../api/account'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const accountOptions = ref([])
const showPending = ref(false)
const pendingList = ref([])
const reviewVisible = ref(false)
const reviewItem = ref(null)
const reviewAmount = ref(0)
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadRef = ref(null)
const uploadForm = ref({ accountId: null, fileList: [], rawFile: null })

const query = ref({ pageNo: 1, pageSize: 10, accountId: null, ocrStatus: null })

const pendingCount = computed(() => pendingList.value.length)

const accountMap = computed(() => {
  const map = {}
  accountOptions.value.forEach(a => { map[a.id] = a.accountName })
  return map
})

function formatMoney(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function statusLabel(s) {
  return { 0: '待处理', 1: '成功', 2: '失败', 3: '待审核' }[s] || '未知'
}

function statusTagType(s) {
  return { 0: 'info', 1: 'success', 2: 'danger', 3: 'warning' }[s] || ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await getScreenshotList(query.value)
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
  } catch (e) { console.error(e) }
}

async function loadPending() {
  try {
    const res = await getPendingReview()
    pendingList.value = res.result || []
  } catch (e) { console.error(e) }
}

function openReview(item) {
  reviewItem.value = item
  reviewAmount.value = item.parsedAmount ? Number(item.parsedAmount) : 0
  reviewVisible.value = true
}

async function handleReview() {
  submitting.value = true
  try {
    await reviewScreenshot(reviewItem.value.id, reviewAmount.value)
    ElMessage.success('审核完成')
    reviewVisible.value = false
    showPending.value = false
    loadData()
    loadPending()
  } finally {
    submitting.value = false
  }
}

async function handleReprocess(id) {
  try {
    await reprocessScreenshot(id)
    ElMessage.success('已重新提交OCR')
    loadData()
  } catch (e) { console.error(e) }
}

function handleFileChange(file) {
  uploadForm.value.rawFile = file.raw
}

function handleFileRemove() {
  uploadForm.value.rawFile = null
}

function resetUploadForm() {
  uploadForm.value = { accountId: null, fileList: [], rawFile: null }
}

async function handleUpload() {
  if (!uploadForm.value.accountId) {
    ElMessage.warning('请选择账号')
    return
  }
  if (!uploadForm.value.rawFile) {
    ElMessage.warning('请选择截图文件')
    return
  }
  uploading.value = true
  try {
    await uploadScreenshot(uploadForm.value.accountId, uploadForm.value.rawFile)
    ElMessage.success('上传成功，OCR处理中')
    uploadDialogVisible.value = false
    loadData()
  } finally {
    uploading.value = false
  }
}

onMounted(() => {
  loadData()
  loadAccounts()
  loadPending()
})
</script>

<style scoped>
.pending-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-border-light);
}

.pending-info {
  font-size: var(--font-size-sm);
  line-height: 1.6;
}
</style>
