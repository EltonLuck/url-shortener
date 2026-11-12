import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 }, // ramp up
    { duration: '1m', target: 50 },  // steady state at 50 VUs
    { duration: '30s', target: 0 },  // ramp down
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  // redirects: 0 is the whole point — without it k6 follows the 302
  // and you'd be timing google.com, not your app.
  const res = http.get('http://localhost:8080/2', { redirects: 0 });
  check(res, {
    'status is 302': (r) => r.status === 302,
    'location is google.com': (r) =>
      (r.headers.Location || r.headers.location || '').includes('google.com'),
  });
}
