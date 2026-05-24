<script setup>
import { ref, onMounted, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { registrationApi } from '@/api';

const registrations = ref([]);
const loading = ref(true);
const searchText = ref('');
const filteredRegistrations = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const paginatedRegistrations = ref([]);

const detailDialogVisible = ref(false);
const detailLoading = ref(false);
const registrationDetail = reactive({
  registrationId: null,
  ticketCode: '',
  tierName: '',
  ticketCount: 1,
  unitPrice: 0,
  totalPrice: 0,
  status: null,
  createdAt: '',
  userId: null,
  username: '',
  realName: '',
  phone: '',
  activityId: null,
  activityCode: '',
  activityTitle: '',
  activityType: '',
  clubName: '',
  venue: '',
  registrationStart: '',
  eventStart: '',
  eventEnd: ''
});

const statusOptions = [
  { value: 'all', label: '全部' },
  { value: 1, label: '已报名' },
  { value: 2, label: '已取消' },
  { value: 3, label: '已参加' }
];

const currentStatus = ref('all');

const fetchRegistrations = async () => {
  loading.value = true;
  try {
    const response = await registrationApi.listAllRegistrations();
    if (response.code === 200) {
      registrations.value = response.data;
    } else {
      throw new Error(response.message || '获取报名列表失败');
    }
    filterRegistrations();
  } catch (error) {
    console.error('获取报名列表失败:', error);
    ElMessage.error('获取报名列表失败');
  } finally {
    loading.value = false;
  }
};

const filterRegistrations = () => {
  let statusFiltered =
    currentStatus.value === 'all'
      ? registrations.value
      : registrations.value.filter((reg) => reg.status === currentStatus.value);

  if (searchText.value) {
    const searchValue = searchText.value.toLowerCase();
    filteredRegistrations.value = statusFiltered.filter(
      (reg) =>
        (reg.id && String(reg.id).includes(searchValue)) ||
        (reg.userId && String(reg.userId).includes(searchValue)) ||
        (reg.activityId && String(reg.activityId).includes(searchValue))
    );
  } else {
    filteredRegistrations.value = statusFiltered;
  }

  total.value = filteredRegistrations.value.length;
  updatePaginatedData();
};

const updatePaginatedData = () => {
  const startIndex = (currentPage.value - 1) * pageSize.value;
  const endIndex = startIndex + pageSize.value;
  paginatedRegistrations.value = filteredRegistrations.value.slice(startIndex, endIndex);
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
  filterRegistrations();
};

const handleStatusChange = () => {
  currentPage.value = 1;
  filterRegistrations();
};

const viewRegistrationDetail = async (registrationId) => {
  detailLoading.value = true;
  detailDialogVisible.value = true;

  try {
    const response = await registrationApi.getRegistrationDetail(registrationId);
    if (response.code === 200) {
      const data = response.data;
      Object.assign(registrationDetail, {
        registrationId: data.registrationId || data.id,
        ticketCode: data.ticketCode || '',
        tierName: data.tierName || '',
        ticketCount: data.ticketCount ?? 1,
        unitPrice: data.unitPrice ?? 0,
        totalPrice: data.totalPrice ?? 0,
        status: data.status,
        createdAt: data.createdAt,
        userId: data.userId,
        username: data.username || '',
        realName: data.realName || '',
        phone: data.phone || '',
        activityId: data.activityId,
        activityCode: data.activityCode || '',
        activityTitle: data.activityTitle || '',
        activityType: data.activityType || '',
        clubName: data.clubName || '',
        venue: data.venue || '',
        registrationStart: data.registrationStart || '',
        eventStart: data.eventStart || '',
        eventEnd: data.eventEnd || ''
      });
    } else {
      throw new Error(response.message || '获取报名详情失败');
    }
  } catch (error) {
    console.error('获取报名详情失败:', error);
    ElMessage.error('获取报名详情失败');
    detailDialogVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
};

const cancelRegistration = (reg) => {
  if (reg.status !== 1) {
    ElMessage.warning('仅已报名的记录可取消');
    return;
  }

  ElMessageBox.confirm(`确定要取消报名 #${reg.id} 吗？`, '取消报名', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await registrationApi.cancelRegistration(reg.id);
        ElMessage.success('报名已取消');
        fetchRegistrations();
      } catch (error) {
        ElMessage.error('取消报名失败');
      }
    })
    .catch(() => {});
};

const completeRegistration = (reg) => {
  if (reg.status !== 1) {
    ElMessage.warning('仅已报名的记录可标记为已参加');
    return;
  }
  ElMessageBox.confirm(`确认将报名 #${reg.id} 标记为已参加？`, '标记已参加', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  })
    .then(async () => {
      try {
        await registrationApi.completeRegistration(reg.id);
        ElMessage.success('已标记为已参加');
        fetchRegistrations();
      } catch (error) {
        ElMessage.error('操作失败');
      }
    })
    .catch(() => {});
};

const getStatusType = (status) => {
  switch (status) {
    case 1:
      return 'warning';
    case 2:
      return 'danger';
    case 3:
      return 'success';
    default:
      return 'info';
  }
};

const getStatusName = (status) => {
  switch (status) {
    case 1:
      return '已报名';
    case 2:
      return '已取消';
    case 3:
      return '已参加';
    default:
      return '未知';
  }
};

const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  return new Date(dateTimeStr).toLocaleString();
};

onMounted(fetchRegistrations);
</script>

<template>
  <div class="registrations-management">
    <div class="action-bar">
      <div class="status-filter">
        <el-radio-group v-model="currentStatus" @change="handleStatusChange">
          <el-radio-button v-for="option in statusOptions" :key="option.value" :label="option.value">
            {{ option.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <el-input
        v-model="searchText"
        placeholder="搜索报名(ID/用户/活动)"
        prefix-icon="Search"
        style="width: 380px; margin-left: auto;"
        @input="handleSearchInput"
      />
    </div>

    <el-table :data="paginatedRegistrations" style="width: 100%; margin-top: 20px" v-loading="loading" border>
      <el-table-column prop="id" label="报名ID" min-width="80" />
      <el-table-column prop="userId" label="用户ID" min-width="80" />
      <el-table-column prop="activityId" label="活动ID" min-width="80" />
      <el-table-column label="报名时间" min-width="150">
        <template #default="scope">
          {{ formatDateTime(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusName(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" min-width="160">
        <template #default="scope">
          <div class="action-buttons">
            <el-button size="small" type="primary" @click="viewRegistrationDetail(scope.row.id)">详情</el-button>
            <el-button size="small" type="success" :disabled="scope.row.status !== 1" @click="completeRegistration(scope.row)">
              已参加
            </el-button>
            <el-button size="small" type="danger" :disabled="scope.row.status !== 1" @click="cancelRegistration(scope.row)">
              取消
            </el-button>
          </div>
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

    <el-dialog v-model="detailDialogVisible" title="报名详情" width="720px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报名ID" :span="2">{{ registrationDetail.registrationId }}</el-descriptions-item>
          <el-descriptions-item label="票档">{{ registrationDetail.tierName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(registrationDetail.status)">{{ getStatusName(registrationDetail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报名时间" :span="2">{{ formatDateTime(registrationDetail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ registrationDetail.realName || registrationDetail.username }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ registrationDetail.phone || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="活动" :span="2">{{ registrationDetail.activityTitle }}</el-descriptions-item>
          <el-descriptions-item label="社团">{{ registrationDetail.clubName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ registrationDetail.activityType }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ registrationDetail.venue }}</el-descriptions-item>
          <el-descriptions-item label="活动开始">{{ formatDateTime(registrationDetail.eventStart) }}</el-descriptions-item>
          <el-descriptions-item label="票价">¥{{ registrationDetail.unitPrice }}</el-descriptions-item>
          <el-descriptions-item label="总价">¥{{ registrationDetail.totalPrice }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.registrations-management {
  width: 100%;
}

.action-bar {
  display: flex;
  margin-bottom: 20px;
  align-items: center;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
