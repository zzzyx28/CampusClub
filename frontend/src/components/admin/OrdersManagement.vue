<script setup>
import { ref, onMounted, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { orderApi } from '@/api';

const orders = ref([]);
const loading = ref(true);
const searchText = ref('');
const filteredOrders = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const paginatedOrders = ref([]);

const detailDialogVisible = ref(false);
const detailLoading = ref(false);
const orderDetail = reactive({
  orderId: null,
  sku: '',
  specOption: '',
  quantity: 1,
  unitPrice: 0,
  totalPrice: 0,
  status: null,
  createdAt: '',
  userId: null,
  username: '',
  realName: '',
  phone: '',
  productId: null,
  spu: '',
  productName: '',
  category: '',
  brand: ''
});

const statusOptions = [
  { value: 'all', label: '全部' },
  { value: 1, label: '进行中' },
  { value: 2, label: '已取消' },
  { value: 3, label: '已完成' }
];

const currentStatus = ref('all');

const fetchOrders = async () => {
  loading.value = true;
  try {
    const response = await orderApi.listAllOrders();
    if (response.code === 200) {
      orders.value = response.data;
    } else {
      throw new Error(response.message || '获取订单列表失败');
    }
    filterOrders();
  } catch (error) {
    console.error('获取订单列表失败:', error);
    ElMessage.error('获取订单列表失败');
  } finally {
    loading.value = false;
  }
};

const filterOrders = () => {
  let statusFiltered =
    currentStatus.value === 'all'
      ? orders.value
      : orders.value.filter((order) => order.status === currentStatus.value);

  if (searchText.value) {
    const searchValue = searchText.value.toLowerCase();
    filteredOrders.value = statusFiltered.filter(
      (order) =>
        (order.id && String(order.id).includes(searchValue)) ||
        (order.userId && String(order.userId).includes(searchValue)) ||
        (order.productId && String(order.productId).includes(searchValue)) ||
        (order.specOption && order.specOption.toLowerCase().includes(searchValue)) ||
        (order.user && order.user.username && order.user.username.toLowerCase().includes(searchValue)) ||
        (order.product && order.product.spu && order.product.spu.toLowerCase().includes(searchValue)) ||
        (order.product && order.product.category && order.product.category.toLowerCase().includes(searchValue)) ||
        (order.product && order.product.brand && order.product.brand.toLowerCase().includes(searchValue))
    );
  } else {
    filteredOrders.value = statusFiltered;
  }

  total.value = filteredOrders.value.length;
  updatePaginatedData();
};

const updatePaginatedData = () => {
  const startIndex = (currentPage.value - 1) * pageSize.value;
  const endIndex = startIndex + pageSize.value;
  paginatedOrders.value = filteredOrders.value.slice(startIndex, endIndex);
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
  filterOrders();
};

const handleStatusChange = () => {
  currentPage.value = 1;
  filterOrders();
};

const viewOrderDetail = async (orderId) => {
  detailLoading.value = true;
  detailDialogVisible.value = true;

  try {
    let response;
    try {
      response = await orderApi.getOrderDetail(orderId);
    } catch (detailError) {
      console.warn('获取订单详情失败，尝试基本订单:', detailError);
      response = await orderApi.getOrderById(orderId);
    }

    if (response.code === 200) {
      const data = response.data;

      Object.assign(orderDetail, {
        orderId: data.id || data.orderId,
        specOption: data.specOption || '',
        status: data.status,
        createdAt: data.createdAt,
        userId: data.userId,
        username: data.username || (data.user ? data.user.username : `用户${data.userId}`),
        realName: data.realName || (data.user ? data.user.realName : ''),
        phone: data.phone || (data.user ? data.user.phone : ''),
        productId: data.productId,
        sku: data.sku || (data.product ? data.product.sku : ''),
        spu: data.spu || (data.product ? data.product.spu : ''),
        productName: data.productName || (data.product ? data.product.name : ''),
        category: data.category || (data.product ? data.product.category : ''),
        brand: data.brand || (data.product ? data.product.brand : ''),
        quantity: data.quantity ?? 1,
        unitPrice: data.unitPrice ?? 0,
        totalPrice: data.totalPrice ?? 0
      });
    } else {
      throw new Error(response.message || '获取订单详情失败');
    }
  } catch (error) {
    console.error('获取订单详情失败:', error);
    ElMessage.error('获取订单详情失败');
    detailDialogVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
};

const cancelOrder = (order) => {
  if (order.status !== 1) {
    ElMessage.warning('仅进行中的订单可取消');
    return;
  }

  ElMessageBox.confirm(`确定要取消订单 #${order.id} 吗？`, '取消订单', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await orderApi.cancelOrder(order.id);
        ElMessage.success('订单已取消');
        fetchOrders();
      } catch (error) {
        ElMessage.error('取消订单失败');
      }
    })
    .catch(() => {});
};

const completeOrder = (order) => {
  if (order.status !== 1) {
    ElMessage.warning('仅进行中的订单可完成');
    return;
  }
  ElMessageBox.confirm(`确认将订单 #${order.id} 标记为已完成？`, '完成订单', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  })
    .then(async () => {
      try {
        await orderApi.completeOrder(order.id);
        ElMessage.success('订单已完成');
        fetchOrders();
      } catch (error) {
        ElMessage.error('完成订单失败');
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
      return '进行中';
    case 2:
      return '已取消';
    case 3:
      return '已完成';
    default:
      return '未知';
  }
};

const getUserName = (order) => {
  return order.user ? order.user.username : `用户ID: ${order.userId}`;
};

const getProductSku = (order) => {
  return order.product ? order.product.spu : `商品ID: ${order.productId}`;
};

const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  const date = new Date(dateTimeStr);
  return date.toLocaleString();
};

onMounted(() => {
  fetchOrders();
});
</script>

<template>
  <div class="orders-management">
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
        placeholder="搜索订单(ID/用户/SPU/规格/类目/品牌)"
        prefix-icon="Search"
        style="width: 380px; margin-left: auto;"
        @input="handleSearchInput"
      />
    </div>

    <el-table :data="paginatedOrders" style="width: 100%; margin-top: 20px" v-loading="loading" border>
      <el-table-column prop="id" label="订单ID" min-width="80" />
      <el-table-column label="用户" min-width="120">
        <template #default="scope">
          {{ getUserName(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="140">
        <template #default="scope">
          {{ getProductSku(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column prop="specOption" label="选购规格" min-width="100" />
      <el-table-column label="下单时间" min-width="150">
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
            <el-button size="small" type="primary" @click="viewOrderDetail(scope.row.id)">详情</el-button>
            <el-button
              size="small"
              type="success"
              :disabled="scope.row.status !== 1"
              @click="completeOrder(scope.row)"
            >
              完成
            </el-button>
            <el-button
              size="small"
              type="danger"
              :disabled="scope.row.status !== 1"
              @click="cancelOrder(scope.row)"
            >
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
        class="pagination"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        :page-sizes="[5, 10, 20, 50]"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="detailDialogVisible" title="订单详情" width="680px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单ID" :span="2">{{ orderDetail.orderId }}</el-descriptions-item>
          <el-descriptions-item label="选购规格">{{ orderDetail.specOption }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(orderDetail.status)">{{ getStatusName(orderDetail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间" :span="2">{{ formatDateTime(orderDetail.createdAt) }}</el-descriptions-item>

          <el-descriptions-item label="用户ID">{{ orderDetail.userId }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ orderDetail.username }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ orderDetail.realName || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ orderDetail.phone || '未提供' }}</el-descriptions-item>

          <el-descriptions-item label="商品信息" :span="2" class="product-info-header">
            <div class="product-header">
              <span class="product-sku">{{ orderDetail.sku }}</span>
              <span class="brand">{{ orderDetail.brand }}</span>
            </div>
            <div class="product-title">{{ orderDetail.productName }}</div>
          </el-descriptions-item>

          <el-descriptions-item label="SPU">{{ orderDetail.spu }}</el-descriptions-item>
          <el-descriptions-item label="类目">{{ orderDetail.category }}</el-descriptions-item>
          <el-descriptions-item label="品牌">{{ orderDetail.brand }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ orderDetail.quantity }}</el-descriptions-item>
          <el-descriptions-item label="成交单价">¥{{ orderDetail.unitPrice }}</el-descriptions-item>
          <el-descriptions-item label="成交总价">¥{{ orderDetail.totalPrice }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button
            type="success"
            :disabled="orderDetail.status !== 1"
            @click="completeOrder({ id: orderDetail.orderId, status: orderDetail.status })"
          >
            完成订单
          </el-button>
          <el-button
            type="danger"
            :disabled="orderDetail.status !== 1"
            @click="cancelOrder({ id: orderDetail.orderId, status: orderDetail.status })"
          >
            取消订单
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.orders-management {
  width: 100%;
}

.action-bar {
  display: flex;
  margin-bottom: 20px;
  align-items: center;
}

.status-filter {
  margin-right: 20px;
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

.action-buttons .el-button {
  margin-left: 0;
}

.product-info-header {
  font-weight: bold;
}

.product-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-sku {
  font-size: 16px;
  font-weight: bold;
}

.brand {
  color: #666;
}

.product-title {
  margin-top: 6px;
  font-weight: normal;
  color: #333;
}
</style>
