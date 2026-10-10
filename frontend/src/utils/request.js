import axios from 'axios';

const defaultBaseURL = '/api';

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || defaultBaseURL,
  timeout: parseInt(import.meta.env.VITE_APP_API_TIMEOUT || '10000')
});

function redirectToLogin() {
  localStorage.removeItem('token');
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login';
  }
}

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['token'] = token;
    }
    return config;
  },
  error => Promise.reject(error)
);

service.interceptors.response.use(
  response => {
    const res = response.data;
    if (res.code !== 200) {
      if (res.code === 401 && res.msg !== '旧密码错误') {
        redirectToLogin();
      }
      return Promise.reject(new Error(res.msg || '请求失败'));
    }
    return res;
  },
  error => {
    let message = '请求失败';
    if (error.response?.data?.msg) {
      message = error.response.data.msg;
    } else if (error.response) {
      switch (error.response.status) {
        case 401:
          message = '未授权，请重新登录';
          redirectToLogin();
          break;
        case 403:
          message = '拒绝访问';
          break;
        case 404:
          message = '请求的资源不存在';
          break;
        case 429:
          message = '请求过于频繁，请稍后再试';
          break;
        case 500:
          message = '服务器内部错误';
          break;
        default:
          message = `请求失败: ${error.response.status}`;
      }
    }
    return Promise.reject(new Error(message));
  }
);

export function get(url, params) {
  return service({ url, method: 'get', params });
}

export function post(url, data, config = {}) {
  return service({ ...config, url, method: 'post', data });
}

export function put(url, data, config = {}) {
  return service({ ...config, url, method: 'put', data });
}

export function del(url, data) {
  return service({ url, method: 'delete', data });
}

export default service;
