<template>
  <div class="page-container">
    <div class="page-header page-header-row">
      <div>
        <h2>用户管理</h2>
        <p>管理系统用户账号</p>
      </div>
      <el-button type="primary" @click="openAdd">
        <el-icon><Plus /></el-icon>添加用户
      </el-button>
    </div>

    <!-- Table -->
    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="openResetPwd(row)">重置密码</el-button>
            <el-popconfirm title="确定删除该用户？" @confirm="handleDelete(row.id)">
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

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '添加用户'" width="420px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%;">
            <el-option value="USER" label="普通用户" />
            <el-option value="ADMIN" label="管理员" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="400px">
      <p style="margin-bottom: var(--space-md); color: var(--color-text-secondary);">
        为用户 <strong>{{ resetPwdUser?.username }}</strong> 设置新密码
      </p>
      <el-form label-width="80px">
        <el-form-item label="新密码" required>
          <el-input v-model="newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwd" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getUserList, addUser, editUser, deleteUser, resetPassword } from '../../api/user'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const resetPwdVisible = ref(false)
const resetPwdUser = ref(null)
const newPassword = ref('')

const query = ref({ pageNo: 1, pageSize: 10 })

const form = ref({
  id: null,
  username: '',
  password: '',
  nickname: '',
  role: 'USER',
  status: 1
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function openAdd() {
  isEdit.value = false
  form.value = { id: null, username: '', password: '', nickname: '', role: 'USER', status: 1 }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = { ...row, password: '' }
  dialogVisible.value = true
}

function openResetPwd(row) {
  resetPwdUser.value = row
  newPassword.value = ''
  resetPwdVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const res = await getUserList(query.value)
    tableData.value = res.result.records || []
    total.value = res.result.total || 0
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await editUser(form.value)
      ElMessage.success('编辑成功')
    } else {
      await addUser(form.value)
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
    await deleteUser(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

async function handleResetPwd() {
  if (!newPassword.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  submitting.value = true
  try {
    await resetPassword(resetPwdUser.value.id, newPassword.value)
    ElMessage.success('密码重置成功')
    resetPwdVisible.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>
