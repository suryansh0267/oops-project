import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('itsm_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      error.message = 'Cannot reach the server. Check that the backend is running.';
    } else if (error.response.status === 401 && !error.config.url.startsWith('/auth/')) {
      localStorage.removeItem('itsm_token');
      localStorage.removeItem('itsm_user');
      if (!['/login', '/register'].includes(window.location.pathname)) {
        window.location.href = '/login?expired=1';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
