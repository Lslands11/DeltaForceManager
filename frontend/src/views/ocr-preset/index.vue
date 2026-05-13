<template>
  <div class="page-container">
    <div class="page-header page-header-row">
      <div>
        <h2>OCR 预设配置</h2>
        <p>按游戏类型管理截图识别参数</p>
      </div>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>添加预设
      </el-button>
    </div>

    <!-- Table -->
    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="gameName" label="游戏名称" min-width="140" />
        <el-table-column label="裁剪区域" min-width="140">
          <template #default="{ row }">
            {{ row.cropX }},{{ row.cropY }} / {{ row.cropWidth }}x{{ row.cropHeight }}
          </template>
        </el-table-column>
        <el-table-column label="放大倍数" width="90">
          <template #default="{ row }">{{ row.scaleFactor }}x</template>
        </el-table-column>
        <el-table-column label="二值化" width="80">
          <template #default="{ row }">{{ row.thresholdValue }}</template>
        </el-table-column>
        <el-table-column label="反色" width="70">
          <template #default="{ row }">{{ row.invertColors === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="unitSuffix" label="单位后缀" width="100">
          <template #default="{ row }">{{ row.unitSuffix || '自动' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该预设？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑预设' : '添加预设'" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="游戏名称" required>
          <el-input v-model="form.gameName" placeholder="请输入游戏名称" :disabled="!!form.id" />
        </el-form-item>

        <el-divider content-position="left">裁剪区域</el-divider>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0 var(--space-md);">
          <el-form-item label="裁剪X坐标">
            <el-input-number v-model="form.cropX" :min="0" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="裁剪Y坐标">
            <el-input-number v-model="form.cropY" :min="0" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="裁剪宽度">
            <el-input-number v-model="form.cropWidth" :min="1" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="裁剪高度">
            <el-input-number v-model="form.cropHeight" :min="1" style="width: 100%;" />
          </el-form-item>
        </div>

        <el-divider content-position="left">图像处理</el-divider>
        <el-form-item label="放大倍数">
          <el-input-number v-model="form.scaleFactor" :min="1" :max="10" :step="0.5" :precision="1" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="二值化阈值">
          <el-slider v-model="form.thresholdValue" :min="0" :max="255" show-input style="width: 100%;" />
        </el-form-item>
        <el-form-item label="反色处理">
          <el-switch v-model="form.invertColors" :active-value="1" :inactive-value="0" active-text="是" inactive-text="否" />
        </el-form-item>

        <el-divider content-position="left">Tesseract 设置</el-divider>
        <el-form-item label="页面分割模式">
          <el-select v-model="form.tesseractPsm" style="width: 200px;">
            <el-option :value="3" label="3 - 全自动分割" />
            <el-option :value="6" label="6 - 统一文本块" />
            <el-option :value="7" label="7 - 单行文本" />
            <el-option :value="8" label="8 - 单个单词" />
            <el-option :value="13" label="13 - 原始行" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位后缀">
          <el-select v-model="form.unitSuffix" style="width: 200px;" clearable>
            <el-option value="" label="自动识别" />
            <el-option value="万" label="万 (x10000)" />
            <el-option value="W" label="W (x10000)" />
            <el-option value="K" label="K (x1000)" />
            <el-option value="M" label="M (x1000000)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getOcrPresetList, saveOcrConfig, deleteOcrPreset } from '../../api/account'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)

const defaultForm = {
  id: null, gameName: '',
  cropX: 0, cropY: 0, cropWidth: 200, cropHeight: 60,
  scaleFactor: 2.0, thresholdValue: 128, invertColors: 0,
  tesseractPsm: 7, unitSuffix: ''
}
const form = ref({ ...defaultForm })

async function loadData() {
  loading.value = true
  try {
    const res = await getOcrPresetList()
    tableData.value = res.result || []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    form.value = { ...row, unitSuffix: row.unitSuffix || '' }
  } else {
    form.value = { ...defaultForm }
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.gameName) {
    ElMessage.warning('请输入游戏名称')
    return
  }
  submitting.value = true
  try {
    await saveOcrConfig(form.value)
    ElMessage.success(form.value.id ? '编辑成功' : '添加成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteOcrPreset(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadData)
</script>
