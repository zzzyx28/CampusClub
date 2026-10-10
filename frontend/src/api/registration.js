import { get, post, put } from '@/utils/request';
import { API_PREFIX } from '@/utils/api-prefix';

export function getRegistrationById(id) {
  return get(`${API_PREFIX}/registration/${id}`);
}

export function getRegistrationDetail(id) {
  return get(`${API_PREFIX}/registration/${id}/detail`);
}

export function listRegistrationsByUserId(userId) {
  return get(`${API_PREFIX}/registration/user/${userId}`);
}

export function listRegistrationsWithDetail(userId) {
  return get(`${API_PREFIX}/registration/user/${userId}/detail`);
}

export function listRegistrationsByActivityId(activityId) {
  return get(`${API_PREFIX}/registration/activity/${activityId}`);
}

export function createRegistration(data, operationKey = crypto.randomUUID()) {
  return post(`${API_PREFIX}/registration`, data, { headers: { 'Idempotency-Key': operationKey } });
}

export function cancelRegistration(id, operationKey = crypto.randomUUID()) {
  return put(`${API_PREFIX}/registration/${id}/cancel`, undefined, { headers: { 'Idempotency-Key': operationKey } });
}

export function completeRegistration(id) {
  return put(`${API_PREFIX}/registration/${id}/complete`);
}

export function listAllRegistrations() {
  return get(`${API_PREFIX}/registration/admin/list`);
}

export function getRegistrationOperation(key) {
  return get(`${API_PREFIX}/registration/operations/${encodeURIComponent(key)}`);
}
