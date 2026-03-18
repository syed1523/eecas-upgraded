import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082/api',
});

api.interceptors.request.use((config) => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (user && user.token) {
        config.headers.Authorization = `Bearer ${user.token}`;
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});

// Global response interceptor — clears stale token and redirects on auth failure
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            localStorage.removeItem('user');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// Expense APIs
export const expenseApi = {
    getAllRequest: (page = 0, size = 10) => api.get(`/expenses?page=${page}&size=${size}`),
    getMyExpenses: (page = 0, size = 10) => api.get(`/expenses/my?page=${page}&size=${size}`),
    submit: (formData) => api.post('/expenses', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    }),
    saveDraft: (data) => api.post('/expenses/draft', data),
    acknowledge: (id) => api.post(`/expenses/${id}/acknowledge`, {}),
};

// Approval/Workflow APIs
export const approvalApi = {
    getPending: (page = 0, size = 10) => api.get(`/approvals/pending?page=${page}&size=${size}`),
    getHistory: (id) => api.get(`/approvals/history/${id}`),
    approve: (id, comments) => api.post(`/approvals/${id}`, { action: 'APPROVE', comments }),
    reject: (id, comments) => api.post(`/approvals/${id}`, { action: 'REJECT', comments }),
    escalate: (id, reason) => api.post(`/approvals/${id}/escalate`, { reason }),
    pay: (id) => api.post(`/approvals/${id}/pay`, {}), // Empty body for post
};

// Manager Scoped APIs
export const managerApi = {
    getTeamSummary: () => api.get('/manager/reports/team-summary'),
    getExpenses: (page = 0, size = 20) => api.get(`/manager/expenses?page=${page}&size=${size}`),
    overrideApprove: (id, payload) => api.post(`/manager/expenses/${id}/override-approve`, payload),
    getOverrides: () => api.get('/manager/overrides'),
};

// Forensic / Auditor APIs
export const forensicApi = {
    getFlagged: (page = 0, size = 5) => api.get(`/forensics/expenses?isFlagged=true&page=${page}&size=${size}`),
    getLogs: (page = 0, size = 5) => api.get(`/forensics/logs?page=${page}&size=${size}`),
    searchExpenses: (params) => api.get('/forensics/expenses', { params }),
};

// Admin APIs — ADMIN role only
export const adminApi = {
    getUsers: (page = 0, size = 20) => api.get(`/admin/users?page=${page}&size=${size}`),
    setUserStatus: (id, active) => api.patch(`/admin/users/${id}/status`, { active }),
    changeUserRole: (id, role) => api.patch(`/admin/users/${id}/role`, { role }),
    getDepartments: () => api.get('/admin/departments'),
    createDept: (data) => api.post('/admin/departments', data),
    getRules: () => api.get('/admin/rules'),
    createRule: (data) => api.post('/admin/rules', data),
    updateRule: (id, data) => api.put(`/admin/rules/${id}`, data),
    deleteRule: (id) => api.delete(`/admin/rules/${id}`),
    toggleRule: (id) => api.patch(`/admin/rules/${id}/toggle`),
    getBudgets: (page = 0, size = 20) => api.get(`/admin/budgets?page=${page}&size=${size}`),
    getLogs: (page = 0, size = 50) => api.get(`/admin/logs?page=${page}&size=${size}`),

    // Configurations and Recommendations
    getConfigs: () => api.get('/admin/configurations'),
    updateConfig: (key, value) => api.patch(`/admin/configurations/${key}`, { value }),
    getRecommendations: () => api.get('/admin/recommendations'),
    updateRecommendation: (id, status) => api.patch(`/admin/recommendations/${id}`, { status }),
};

export default api;
