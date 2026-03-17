import api from './api';

export const auditApi = {
    getExpenseHistory: (id) => api.get(`/audit/expenses/${id}/history`),
    getRecentActivity: () => api.get('/audit/recent'),
    getTodayActivity: () => api.get('/audit/today'),
    getSystemTriggered: () => api.get('/audit/system'),
};
