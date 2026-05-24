import { get, post, put, del } from '@/utils/request';
import { API_PREFIX } from '@/utils/api-prefix';

export function listActivities() {
  return get(`${API_PREFIX}/activity/list`);
}

export function getActivityById(id) {
  return get(`${API_PREFIX}/activity/${id}`);
}

export function getActivityByTicketCode(ticketCode) {
  return get(`${API_PREFIX}/activity/ticket/${encodeURIComponent(ticketCode)}`);
}

export function searchActivities(params) {
  return get(`${API_PREFIX}/activity/search`, params);
}

export function listByClub(clubName) {
  return get(`${API_PREFIX}/activity/club/${encodeURIComponent(clubName)}`);
}

export function addActivity(data) {
  return post(`${API_PREFIX}/activity`, data);
}

export function updateActivity(id, data) {
  return put(`${API_PREFIX}/activity/${id}`, data);
}

export function deleteActivity(id) {
  return del(`${API_PREFIX}/activity/${id}`);
}
