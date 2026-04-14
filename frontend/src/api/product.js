import { get, post, put, del } from '@/utils/request';
import { API_PREFIX } from '@/utils/api-prefix';

export function listProducts() {
  return get(`${API_PREFIX}/product/list`);
}

export function getProductById(id) {
  return get(`${API_PREFIX}/product/${id}`);
}

export function getProductBySku(sku) {
  return get(`${API_PREFIX}/product/sku/${encodeURIComponent(sku)}`);
}

export function searchProducts(params) {
  return get(`${API_PREFIX}/product/search`, params);
}

export function listByBrand(brand) {
  return get(`${API_PREFIX}/product/brand/${encodeURIComponent(brand)}`);
}

export function addProduct(data) {
  return post(`${API_PREFIX}/product`, data);
}

export function updateProduct(id, data) {
  return put(`${API_PREFIX}/product/${id}`, data);
}

export function deleteProduct(id) {
  return del(`${API_PREFIX}/product/${id}`);
}
