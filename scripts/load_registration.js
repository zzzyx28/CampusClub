// k6 script. Configure BASE_URL, TOKEN, ACTIVITY_ID; no benchmark claim is made by the repository.
import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';
const unexpected = new Rate('unexpected_response');
import exec from 'k6/execution';
export const options = { scenarios: { competition: { executor: 'shared-iterations', vus: 100, iterations: 100, maxDuration: '30s' } }, thresholds: { unexpected_response: ['rate==0'] } };
export default function () {
  const userId = exec.scenario.iterationInTest + 10000;
  const payload = JSON.stringify({ userId, activityId: Number(__ENV.ACTIVITY_ID), tierName: __ENV.TIER_NAME || 'Standard' });
  const response = http.post(`${__ENV.BASE_URL || 'http://localhost:8080'}/api/registration`, payload, { headers: { Authorization: `Bearer ${__ENV.TOKEN}`, 'Content-Type': 'application/json', 'Idempotency-Key': `k6-${userId}` } });
  const expected = [200, 409].includes(response.status);
  unexpected.add(!expected);
  check(response, { 'business response': () => expected });
}
