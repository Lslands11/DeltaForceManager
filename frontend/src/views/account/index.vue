<template>
  <div class="page-container">
    <div class="page-header page-header-row">
      <div>
        <h2>账号管理</h2>
        <p>管理游戏账号和设备绑定</p>
      </div>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>添加账号
      </el-button>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="账号名称">
          <el-input v-model="query.accountName" placeholder="搜索账号" clearable style="width: 200px;" @clear="loadData" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px;" @change="loadData">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Table -->
    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="accountName" label="账号名称" min-width="120" />
        <el-table-column prop="gameName" label="游戏名称" min-width="120" />
        <el-table-column prop="deviceModel" label="设备型号" min-width="120" />
        <el-table-column prop="deviceToken" label="设备Token" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="$router.push(`/accounts/${row.id}/ocr-config`)">OCR配置</el-button>
            <el-popconfirm title="确定删除该账号？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
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

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑账号' : '添加账号'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="账号名称" required>
          <el-input v-model="form.accountName" placeholder="请输入账号名称" />
        </el-form-item>
        <el-form-item label="游戏名称">
          <el-input v-model="form.gameName" placeholder="请输入游戏名称" />
        </el-form-item>
        <el-form-item label="设备型号">
          <el-input v-model="form.deviceModel" placeholder="请输入设备型号" />
        </el-form-item>
        <el-form-item label="设备Token">
          <el-input v-model="form.deviceToken" placeholder="留空自动生成">
            <template #append>
              <el-button @click="handleGenToken">生成</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息" />
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
import { getAccountList, addAccount, editAccount, deleteAccount, generateToken } from '../../api/account'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)

const query = ref({ pageNo: 1, pageSize: 10, accountName: '', status: null })

const defaultForm = {
  id: null, accountName: '', gameName: '', deviceModel: '', deviceToken: '', status: 1, remark: ''
}
const form = ref({ ...defaultForm })

async function loadData() {
  loading.value = true
  try {
    const res = await getAccountList(query.value)
    tableData.value = res.result.records || []
    total.value = res.result.total || 0
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    form.value = { ...row }
  } else {
    form.value = { ...defaultForm }
  }
  dialogVisible.value = true
}

async function handleGenToken() {
  try {
    const res = await generateToken()
    form.value.deviceToken = res.result.deviceToken
  } catch (e) {
    console.error(e)
  }
}

async function handleSubmit() {
  if (!form.value.accountName) {
    ElMessage.warning('请输入账号名称')
    return
  }
  submitting.value = true
  try {
    if (form.value.id) {
      await editAccount(form.value)
      ElMessage.success('编辑成功')
    } else {
      await addAccount(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteAccount(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadData)
</script>
