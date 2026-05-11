<template>
  <div class="page-container">
    <div class="page-header">
      <el-button link @click="$router.back()" style="margin-bottom: 8px;">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>OCR 配置</h2>
      <p>配置截图识别参数，适配不同游戏界面</p>
    </div>

    <div class="card" style="max-width: 640px;">
      <el-form :model="form" label-width="120px" v-loading="loading">
        <el-divider content-position="left">裁剪区域</el-divider>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px;">
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
          <el-input v-model="form.unitSuffix" placeholder="如 万、W、K" style="width: 200px;" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存配置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getOcrConfig, saveOcrConfig } from '../../api/account'

const route = useRoute()
const accountId = route.params.id

const loading = ref(false)
const saving = ref(false)

const form = ref({
  accountId: Number(accountId),
  cropX: 0, cropY: 0, cropWidth: 200, cropHeight: 60,
  scaleFactor: 2.0, thresholdValue: 128, invertColors: 0,
  tesseractPsm: 7, unitSuffix: '万'
})

async function loadConfig() {
  loading.value = true
  try {
    const res = await getOcrConfig(accountId)
    if (res.result) {
      form.value = { ...form.value, ...res.result }
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveOcrConfig(form.value)
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>
