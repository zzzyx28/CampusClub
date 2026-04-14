import { get, post, put } from '@/utils/request';
import { API_PREFIX } from '@/utils/api-prefix';

/**
 * 获取订单详情
 * @param {number} id - 订单ID
 * @returns {Promise} - 返回订单详情
 */
export function getOrderById(id) {
  return get(`${API_PREFIX}/order/${id}`);
}

/**
 * 获取订单详细信息（包含用户和商品信息）
 * @param {number} id - 订单ID
 * @returns {Promise} - 返回订单详细信息
 */
export function getOrderDetail(id) {
  return get(`${API_PREFIX}/order/${id}/detail`);
}

/**
 * 获取用户订单列表
 * @param {number} userId - 用户ID
 * @returns {Promise} - 返回订单列表
 */
export function listOrdersByUserId(userId) {
  return get(`${API_PREFIX}/order/user/${userId}`);
}

/**
 * 获取用户订单详情列表（包含商品和用户信息）
 * @param {number} userId - 用户ID
 * @returns {Promise} - 返回订单详情列表
 */
export function listOrdersWithDetail(userId) {
  return get(`${API_PREFIX}/order/user/${userId}/detail`);
}

/**
 * 获取某商品关联的订单列表
 * @param {number} productId - 商品ID
 * @returns {Promise} - 返回订单列表
 */
export function listOrdersByProductId(productId) {
  return get(`${API_PREFIX}/order/product/${productId}`);
}

/**
 * 创建订单
 * @param {Object} data - 订单信息 {userId, productId, specOption?}
 * @returns {Promise} - 返回创建结果
 */
export function createOrder(data) {
  return post(`${API_PREFIX}/order`, data);
}

/**
 * 取消订单
 * @param {number} id - 订单ID
 * @returns {Promise} - 返回取消结果
 */
export function cancelOrder(id) {
  return put(`${API_PREFIX}/order/${id}/cancel`);
}

/**
 * 完成订单（管理员）
 * @param {number} id - 订单ID
 * @returns {Promise}
 */
export function completeOrder(id) {
  return put(`${API_PREFIX}/order/${id}/complete`);
}

/**
 * 获取所有订单列表（管理员使用）
 * @returns {Promise} - 返回订单列表
 */
export function listAllOrders() {
  return get(`${API_PREFIX}/order/admin/list`);
} 