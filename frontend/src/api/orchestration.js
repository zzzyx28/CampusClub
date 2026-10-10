import service from '@/utils/request';
import { API_PREFIX } from '@/utils/api-prefix';

export function startRun(data, idempotencyKey) {
  return service({
    url: `${API_PREFIX}/agent-runs`,
    method: 'post',
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  });
}

export function getRun(runId) {
  return service({ url: `${API_PREFIX}/agent-runs/${runId}`, method: 'get' });
}

export function decideApproval(runId, approved) {
  return service({
    url: `${API_PREFIX}/agent-runs/${runId}/approval`,
    method: 'post',
    data: { approved }
  });
}

export function cancelRun(runId) {
  return service({ url: `${API_PREFIX}/agent-runs/${runId}/cancel`, method: 'post' });
}
