<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { activityApi } from '@/api';

const activities = ref([]);
const loading = ref(true);
const searchText = ref('');
const filteredActivities = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const paginatedActivities = ref([]);
const activityTypeOptions = ['讲座', '比赛', '招新', '志愿', '文艺', '体育', '其他'];

const activityForm = reactive({
  id: null,
  activityCode: '',
  title: '',
  clubName: '',
  activityType: '',
  description: '',
  venue: '',
  registrationStart: '',
  eventStart: '',
  eventEnd: '',
  posterUrl: '',
  status: 1,
  tierList: []
});

const dialogVisible = ref(false);
const dialogTitle = ref('添加活动');
const isEditMode = ref(false);

const fetchActivities = async () => {
  loading.value = true;
  try {
    const response = await activityApi.listActivities();
    if (response.code === 200) {
      activities.value = response.data.map((a) => ({
        id: a.id,
        activityCode: a.activityCode,
        title: a.title,
        clubName: a.clubName,
        activityType: a.activityType,
        description: a.description,
        venue: a.venue,
        registrationStart: a.registrationStart,
        eventStart: a.eventStart,
        eventEnd: a.eventEnd,
        posterUrl: a.posterUrl,
        status: a.status,
        tierList: a.tierList || [],
        createdAt: a.createdAt,
        updatedAt: a.updatedAt
      }));
      filterActivities();
    } else {
      throw new Error(response.message || '获取活动列表失败');
    }
  } catch (error) {
    console.error('获取活动列表失败:', error);
    ElMessage.error('获取活动列表失败');
  } finally {
    loading.value = false;
  }
};

const filterActivities = () => {
  let filteredResults = activities.value;

  if (searchText.value) {
    const searchValue = searchText.value.toLowerCase();
    filteredActivities.value = filteredResults.filter(
      (a) =>
        a.activityCode.toLowerCase().includes(searchValue) ||
        a.title.toLowerCase().includes(searchValue) ||
        a.clubName.toLowerCase().includes(searchValue) ||
        a.activityType.toLowerCase().includes(searchValue) ||
        a.venue.toLowerCase().includes(searchValue) ||
        (a.description || '').toLowerCase().includes(searchValue)
    );
  } else {
    filteredActivities.value = filteredResults;
  }

  total.value = filteredActivities.value.length;
  updatePaginatedData();
};

const updatePaginatedData = () => {
  const startIndex = (currentPage.value - 1) * pageSize.value;
  const endIndex = startIndex + pageSize.value;
  paginatedActivities.value = filteredActivities.value.slice(startIndex, endIndex);
};

const handleCurrentChange = (page) => {
  currentPage.value = page;
  updatePaginatedData();
};

const handleSizeChange = (size) => {
  pageSize.value = size;
  updatePaginatedData();
};

const handleSearchInput = () => {
  currentPage.value = 1;
  filterActivities();
};

const openAddDialog = () => {
  isEditMode.value = false;
  Object.assign(activityForm, {
    id: null,
    activityCode: '',
    title: '',
    clubName: '',
    activityType: '',
    description: '',
    venue: '',
    registrationStart: '',
    eventStart: '',
    eventEnd: '',
    posterUrl: '',
    status: 1,
    tierList: [
      {
        id: null,
        ticketCode: '',
        tierName: '',
        price: 0,
        originalPrice: 0,
        capacity: 0,
        isDefault: 1
      }
    ]
  });
  dialogTitle.value = '添加活动';
  dialogVisible.value = true;
};

const openEditDialog = (row) => {
  isEditMode.value = true;
  Object.assign(activityForm, {
    ...row,
    tierList: row.tierList?.length
      ? row.tierList.map((tier) => ({
          id: tier.id ?? null,
          ticketCode: tier.ticketCode || '',
          tierName: tier.tierName || '',
          price: tier.price ?? 0,
          originalPrice: tier.originalPrice ?? 0,
          capacity: tier.capacity ?? 0,
          isDefault: tier.isDefault === 1 ? 1 : 0
        }))
      : []
  });
  if (!activityForm.tierList.length) {
    addTierRow();
  }
  dialogTitle.value = '编辑活动';
  dialogVisible.value = true;
};

const addTierRow = () => {
  activityForm.tierList.push({
    id: null,
    ticketCode: '',
    tierName: '',
    price: 0,
    originalPrice: 0,
    capacity: 0,
    isDefault: activityForm.tierList.length === 0 ? 1 : 0
  });
};

const removeTierRow = (index) => {
  if (activityForm.tierList.length <= 1) {
    ElMessage.warning('至少保留一个票档');
    return;
  }
  activityForm.tierList.splice(index, 1);
  if (!activityForm.tierList.some((t) => t.isDefault === 1)) {
    activityForm.tierList[0].isDefault = 1;
  }
};

const setDefaultTier = (index) => {
  activityForm.tierList.forEach((tier, idx) => {
    tier.isDefault = idx === index ? 1 : 0;
  });
};

const saveActivity = async () => {
  if (!activityForm.title.trim()) {
    ElMessage.warning('请输入活动标题');
    return;
  }
  if (!activityForm.clubName.trim()) {
    ElMessage.warning('请输入主办社团');
    return;
  }
  if (!activityForm.activityType.trim()) {
    ElMessage.warning('请选择活动类型');
    return;
  }
  if (!activityForm.venue.trim()) {
    ElMessage.warning('请输入活动地点');
    return;
  }
  if (!activityForm.registrationStart || !activityForm.eventStart) {
    ElMessage.warning('请填写报名开始与活动开始时间');
    return;
  }
  if (!activityForm.tierList.length) {
    ElMessage.warning('请至少添加一个票档');
    return;
  }
  const defaultCount = activityForm.tierList.filter((t) => t.isDefault === 1).length;
  if (defaultCount !== 1) {
    ElMessage.warning('必须且仅能设置一个默认票档');
    return;
  }
  for (const tier of activityForm.tierList) {
    if (!tier.tierName?.trim()) {
      ElMessage.warning('票档名称不能为空');
      return;
    }
  }

  try {
    loading.value = true;
    if (isEditMode.value) {
      const response = await activityApi.updateActivity(activityForm.id, activityForm);
      if (response.code === 200) {
        ElMessage.success('活动信息已更新');
      } else {
        throw new Error(response.message || '更新活动失败');
      }
    } else {
      const response = await activityApi.addActivity(activityForm);
      if (response.code === 200) {
        ElMessage.success('活动已添加');
      } else {
        throw new Error(response.message || '添加活动失败');
      }
    }
    dialogVisible.value = false;
    fetchActivities();
  } catch (error) {
    console.error('保存活动失败:', error);
    ElMessage.error(error.message || '操作失败');
  } finally {
    loading.value = false;
  }
};

const removeActivity = (id) => {
  ElMessageBox.confirm('确定要删除该活动吗？此操作不可逆。', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        loading.value = true;
        const response = await activityApi.deleteActivity(id);
        if (response.code === 200) {
          ElMessage.success('活动已删除');
          fetchActivities();
        } else {
          throw new Error(response.message || '删除活动失败');
        }
      } catch (error) {
        console.error('删除活动失败:', error);
        ElMessage.error(error.message || '删除失败');
      } finally {
        loading.value = false;
      }
    })
    .catch(() => {});
};

onMounted(fetchActivities);
</script>

<template>
  <div class="activities-management">
    <div class="toolbar">
      <el-button type="primary" class="add-button" @click="openAddDialog">
        <el-icon><Plus /></el-icon> 添加活动
      </el-button>

      <el-input
        v-model="searchText"
        placeholder="搜索 标题、社团、类型、地点"
        clearable
        class="search-input"
        @input="handleSearchInput"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <el-table :data="paginatedActivities" style="width: 100%" border v-loading="loading">
      <el-table-column prop="activityCode" label="活动编号" min-width="140" />
      <el-table-column prop="title" label="活动标题" min-width="160" />
      <el-table-column prop="clubName" label="主办社团" min-width="120" />
      <el-table-column prop="activityType" label="类型" min-width="90" />
      <el-table-column prop="venue" label="地点" min-width="140" />
      <el-table-column prop="eventStart" label="活动开始" min-width="170" />
      <el-table-column prop="status" label="状态" min-width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '开放报名' : '已关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="140" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="removeActivity(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        :page-sizes="[5, 10, 20, 50]"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" class="activity-edit-dialog" top="5vh">
      <el-form :model="activityForm" label-position="top" class="activity-form">
        <el-alert title="活动编号、票种编码由系统自动生成，无需手工输入。" type="info" :closable="false" class="dialog-tip" />

        <div class="section-title">基础信息</div>
        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动标题">
              <el-input v-model="activityForm.title" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="主办社团">
              <el-input v-model="activityForm.clubName" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动类型">
              <el-select v-model="activityForm.activityType" placeholder="请选择类型" style="width: 100%">
                <el-option v-for="item in activityTypeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动地点">
              <el-input v-model="activityForm.venue" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="报名开始">
              <el-date-picker v-model="activityForm.registrationStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动开始">
              <el-date-picker v-model="activityForm.eventStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动结束">
              <el-date-picker v-model="activityForm.eventEnd" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="状态">
              <el-segmented
                v-model="activityForm.status"
                :options="[
                  { label: '开放报名', value: 1 },
                  { label: '已关闭', value: 0 }
                ]"
                block
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24">
            <el-form-item label="海报链接">
              <el-input v-model="activityForm.posterUrl" placeholder="https://..." />
            </el-form-item>
          </el-col>
          <el-col :xs="24">
            <el-form-item label="活动简介">
              <el-input v-model="activityForm.description" type="textarea" :rows="3" resize="none" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-title section-title-tier">
          <span>票档与名额</span>
          <el-button type="primary" plain size="small" @click="addTierRow">新增票档</el-button>
        </div>

        <div class="tier-list">
          <div v-for="(tier, index) in activityForm.tierList" :key="index" class="tier-row">
            <div class="tier-row-header">
              <span>票档 {{ index + 1 }}</span>
              <div class="tier-actions">
                <el-button type="primary" plain size="small" @click="setDefaultTier(index)" :disabled="tier.isDefault === 1">
                  设为默认
                </el-button>
                <el-tag v-if="tier.isDefault === 1" type="success" effect="light">默认票档</el-tag>
                <el-button type="danger" plain size="small" @click="removeTierRow(index)">删除</el-button>
              </div>
            </div>

            <el-row :gutter="12">
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="票种编码">
                  <el-input :model-value="tier.ticketCode || '保存后自动生成'" disabled />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="票档名称">
                  <el-input v-model="tier.tierName" placeholder="如 普通票" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="剩余名额">
                  <el-input-number v-model="tier.capacity" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="票价">
                  <el-input-number v-model="tier.price" :min="0" :precision="2" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="原价">
                  <el-input-number v-model="tier.originalPrice" :min="0" :precision="2" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveActivity">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.activities-management {
  width: 100%;
}

.toolbar {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 20px;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.search-input {
  width: 280px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.tier-row {
  padding: 14px 14px 4px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  margin-bottom: 12px;
  background: #f8fafc;
}

.tier-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tier-row-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: 600;
  color: #334155;
}

.activity-form {
  max-height: 68vh;
  overflow: auto;
  padding-right: 4px;
}

.section-title {
  margin: 4px 0 10px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.section-title-tier {
  margin-top: 2px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dialog-tip {
  margin-bottom: 14px;
}
</style>
