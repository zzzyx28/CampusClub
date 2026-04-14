<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { productApi } from '@/api';

const products = ref([]);
const loading = ref(true);
const searchText = ref('');
const filteredProducts = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const paginatedProducts = ref([]);
const categoryOptions = [
  '手机通讯',
  '电脑办公',
  '数码配件',
  '智能穿戴',
  '家用电器',
  '服装鞋包',
  '食品饮料',
  '美妆护肤',
  '个护健康',
  '图书文娱'
];

const productForm = reactive({
  id: null,
  spu: '',
  name: '',
  brand: '',
  category: '',
  description: '',
  coverImage: '',
  status: 1,
  skuList: []
});

const dialogVisible = ref(false);
const dialogTitle = ref('添加商品');
const isEditMode = ref(false);

const fetchProducts = async () => {
  loading.value = true;
  try {
    const response = await productApi.listProducts();
    if (response.code === 200) {
      products.value = response.data.map((p) => ({
        id: p.id,
        spu: p.spu,
        name: p.name,
        brand: p.brand,
        category: p.category,
        description: p.description,
        coverImage: p.coverImage,
        status: p.status,
        skuList: p.skuList || [],
        createdAt: p.createdAt,
        updatedAt: p.updatedAt
      }));
      filterProducts();
    } else {
      throw new Error(response.message || '获取商品列表失败');
    }
  } catch (error) {
    console.error('获取商品列表失败:', error);
    ElMessage.error('获取商品列表失败');
  } finally {
    loading.value = false;
  }
};

const filterProducts = () => {
  let filteredResults = products.value;

  if (searchText.value) {
    const searchValue = searchText.value.toLowerCase();
    filteredProducts.value = filteredResults.filter(
      (p) =>
        p.spu.toLowerCase().includes(searchValue) ||
        p.name.toLowerCase().includes(searchValue) ||
        p.brand.toLowerCase().includes(searchValue) ||
        p.category.toLowerCase().includes(searchValue) ||
        (p.description || '').toLowerCase().includes(searchValue)
    );
  } else {
    filteredProducts.value = filteredResults;
  }

  total.value = filteredProducts.value.length;
  updatePaginatedData();
};

const updatePaginatedData = () => {
  const startIndex = (currentPage.value - 1) * pageSize.value;
  const endIndex = startIndex + pageSize.value;
  paginatedProducts.value = filteredProducts.value.slice(startIndex, endIndex);
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
  filterProducts();
};

const openAddDialog = () => {
  isEditMode.value = false;
  Object.assign(productForm, {
    id: null,
    spu: '',
    name: '',
    brand: '',
    category: '',
    description: '',
    coverImage: '',
    status: 1,
    skuList: [
      {
        id: null,
        sku: '',
        specOption: '',
        salePrice: 0,
        marketPrice: 0,
        stock: 0,
        isDefault: 1
      }
    ]
  });
  dialogTitle.value = '添加商品';
  dialogVisible.value = true;
};

const openEditDialog = (row) => {
  isEditMode.value = true;
  Object.assign(productForm, {
    ...row,
    skuList: row.skuList?.length
      ? row.skuList.map((sku) => ({
          id: sku.id ?? null,
          sku: sku.sku || '',
          specOption: sku.specOption || '',
          salePrice: sku.salePrice ?? 0,
          marketPrice: sku.marketPrice ?? 0,
          stock: sku.stock ?? 0,
          isDefault: sku.isDefault === 1 ? 1 : 0
        }))
      : []
  });
  if (!productForm.skuList.length) {
    addSkuRow();
  }
  dialogTitle.value = '编辑商品';
  dialogVisible.value = true;
};

const addSkuRow = () => {
  productForm.skuList.push({
    id: null,
    sku: '',
    specOption: '',
    salePrice: 0,
    marketPrice: 0,
    stock: 0,
    isDefault: productForm.skuList.length === 0 ? 1 : 0
  });
};

const removeSkuRow = (index) => {
  if (productForm.skuList.length <= 1) {
    ElMessage.warning('至少保留一个规格');
    return;
  }
  productForm.skuList.splice(index, 1);
  if (!productForm.skuList.some((s) => s.isDefault === 1)) {
    productForm.skuList[0].isDefault = 1;
  }
};

const setDefaultSku = (index) => {
  productForm.skuList.forEach((sku, idx) => {
    sku.isDefault = idx === index ? 1 : 0;
  });
};

const saveProduct = async () => {
  if (!productForm.name.trim()) {
    ElMessage.warning('请输入商品名称');
    return;
  }
  if (!productForm.brand.trim()) {
    ElMessage.warning('请输入品牌');
    return;
  }
  if (!productForm.category.trim()) {
    ElMessage.warning('请输入类目');
    return;
  }
  if (productForm.status !== 0 && productForm.status !== 1) {
    ElMessage.warning('状态仅支持上架或下架');
    return;
  }
  if (!productForm.skuList.length) {
    ElMessage.warning('请至少添加一个商品规格');
    return;
  }
  const defaultCount = productForm.skuList.filter((s) => s.isDefault === 1).length;
  if (defaultCount !== 1) {
    ElMessage.warning('必须且仅能设置一个默认规格');
    return;
  }
  for (const sku of productForm.skuList) {
    if (!sku.specOption?.trim()) {
      ElMessage.warning('规格说明不能为空');
      return;
    }
  }

  try {
    loading.value = true;
    if (isEditMode.value) {
      const response = await productApi.updateProduct(productForm.id, productForm);
      if (response.code === 200) {
        ElMessage.success('商品信息已更新');
      } else {
        throw new Error(response.message || '更新商品失败');
      }
    } else {
      const response = await productApi.addProduct(productForm);
      if (response.code === 200) {
        ElMessage.success('商品已添加');
      } else {
        throw new Error(response.message || '添加商品失败');
      }
    }
    dialogVisible.value = false;
    fetchProducts();
  } catch (error) {
    console.error('保存商品失败:', error);
    ElMessage.error(error.message || '操作失败');
  } finally {
    loading.value = false;
  }
};

const removeProduct = (id) => {
  ElMessageBox.confirm('确定要删除该商品吗？此操作不可逆。', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        loading.value = true;
        const response = await productApi.deleteProduct(id);
        if (response.code === 200) {
          ElMessage.success('商品已删除');
          fetchProducts();
        } else {
          throw new Error(response.message || '删除商品失败');
        }
      } catch (error) {
        console.error('删除商品失败:', error);
        ElMessage.error(error.message || '删除失败');
      } finally {
        loading.value = false;
      }
    })
    .catch(() => {});
};

onMounted(fetchProducts);
</script>

<template>
  <div class="products-management">
    <div class="toolbar">
      <el-button type="primary" class="add-button" @click="openAddDialog">
        <el-icon><Plus /></el-icon> 添加商品
      </el-button>

      <el-input
        v-model="searchText"
        placeholder="搜索 名称、品牌、类目、描述"
        clearable
        class="search-input"
        @input="handleSearchInput"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

    </div>

    <el-table :data="paginatedProducts" style="width: 100%" border v-loading="loading">
      <el-table-column prop="spu" label="SPU" min-width="140" />
      <el-table-column prop="name" label="商品名称" min-width="140" />
      <el-table-column prop="brand" label="品牌" min-width="100" />
      <el-table-column prop="category" label="类目" min-width="90" />
      <el-table-column prop="status" label="状态" min-width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" />
      <el-table-column label="操作" min-width="140" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="removeProduct(scope.row.id)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="860px" class="product-edit-dialog" top="6vh">
      <el-form :model="productForm" label-position="top" class="product-form">
        <el-alert title="SPU、SKU 由系统自动生成，无需手工输入。" type="info" :closable="false" class="dialog-tip" />

        <div class="section-title">基础信息</div>
        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="商品名称">
              <el-input v-model="productForm.name" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="品牌">
              <el-input v-model="productForm.brand" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="类目">
              <el-select v-model="productForm.category" placeholder="请选择类目" style="width: 100%">
                <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="状态">
              <el-segmented
                v-model="productForm.status"
                :options="[
                  { label: '上架', value: 1 },
                  { label: '下架', value: 0 }
                ]"
                block
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24">
            <el-form-item label="封面图链接">
              <el-input v-model="productForm.coverImage" placeholder="https://..." />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24">
            <el-form-item label="商品描述">
              <el-input v-model="productForm.description" type="textarea" :rows="3" resize="none" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-title section-title-sku">
          <span>规格与价格</span>
          <el-button type="primary" plain size="small" @click="addSkuRow">新增规格</el-button>
        </div>

        <div class="sku-list">
          <div v-for="(sku, index) in productForm.skuList" :key="index" class="sku-row">
            <div class="sku-row-header">
              <span>规格 {{ index + 1 }}</span>
              <div class="sku-actions">
                <el-button type="primary" plain size="small" @click="setDefaultSku(index)" :disabled="sku.isDefault === 1">
                  设为默认
                </el-button>
                <el-tag v-if="sku.isDefault === 1" type="success" effect="light">默认规格</el-tag>
                <el-button type="danger" plain size="small" @click="removeSkuRow(index)">删除</el-button>
              </div>
            </div>

            <el-row :gutter="12">
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item :label="`SKU 编码`">
                  <el-input :model-value="sku.sku || '保存后自动生成'" disabled />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="规格说明">
                  <el-input v-model="sku.specOption" placeholder="如 256G 深空灰" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="库存">
                  <el-input-number v-model="sku.stock" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="销售价">
                  <el-input-number v-model="sku.salePrice" :min="0" :precision="2" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="划线价">
                  <el-input-number v-model="sku.marketPrice" :min="0" :precision="2" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveProduct">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.products-management {
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
  margin-right: 0;
}

.date-picker {
  width: 260px;
  margin-right: 0;
}

.add-button {
  margin-left: 0;
  margin-right: 0;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.sku-row {
  padding: 14px 14px 4px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  margin-bottom: 12px;
  background: #f8fafc;
}

.sku-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.sku-row-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: 600;
  color: #334155;
}

.product-form {
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

.section-title-sku {
  margin-top: 2px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dialog-tip {
  margin-bottom: 14px;
}

.sku-list {
  margin-top: 8px;
}

@media (max-width: 992px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-input,
  .date-picker {
    width: 100%;
    margin-bottom: 10px;
  }

  .product-form {
    max-height: 72vh;
  }

  .sku-row-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
}
</style>
