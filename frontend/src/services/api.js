import axios from 'axios';

const API = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

// Attach JWT token to every request
API.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 responses (expired/invalid token)
API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// ==========================================
// Auth API
// ==========================================

export const authAPI = {
  register: (data) => API.post('/auth/register', data),
  login: (data) => API.post('/auth/login', data),
};

// ==========================================
// File API
// ==========================================

export const fileAPI = {
  upload: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return API.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getMyFiles: () => API.get('/files/my-files'),

  getSharedWithMe: () => API.get('/files/shared-with-me'),

  download: (fileId) =>
    API.get(`/files/download/${fileId}`, {
      responseType: 'arraybuffer',
    }),

  deleteFile: (fileId) => API.delete(`/files/${fileId}`),
};

// ==========================================
// Share API
// ==========================================

export const shareAPI = {
  shareFile: (fileId, recipientUsername) =>
    API.post('/share', { fileId, recipientUsername }),

  revokeAccess: (fileId, recipientUsername) =>
    API.delete('/share', { data: { fileId, recipientUsername } }),

  getSharedUsers: (fileId) => API.get(`/share/${fileId}/users`),
};

export default API;
