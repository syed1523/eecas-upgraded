import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082/api';

const getAuthHeader = () => {
    const userStr = localStorage.getItem('user');
    let token = null;
    if (userStr) {
        try {
            token = JSON.parse(userStr).token;
        } catch (e) { }
    }
    return { Authorization: `Bearer ${token}` };
};

export const analyticsApi = {
    // Employee
    getEmployeeScore: () => axios.get(`${BASE_URL}/analytics/employee/score`, { headers: getAuthHeader() }),
    getEmployeeAlerts: () => axios.get(`${BASE_URL}/alerts/employee`, { headers: getAuthHeader() }),

    // Manager
    getManagerOverview: () => axios.get(`${BASE_URL}/analytics/manager/overview`, { headers: getAuthHeader() }),
    getManagerTrends: () => axios.get(`${BASE_URL}/analytics/manager/trends`, { headers: getAuthHeader() }),
    getManagerAlerts: () => axios.get(`${BASE_URL}/alerts/manager`, { headers: getAuthHeader() }),

    // Admin / Enterprise
    getEnterpriseOverview: () => axios.get(`${BASE_URL}/analytics/enterprise/overview`, { headers: getAuthHeader() }),
    getExecutiveTrends: () => axios.get(`${BASE_URL}/analytics/enterprise/executive`, { headers: getAuthHeader() }),
    getEnterpriseDeepDive: () => axios.get(`${BASE_URL}/analytics/enterprise/deepdive`, { headers: getAuthHeader() }),
    getAdminAlerts: () => axios.get(`${BASE_URL}/alerts/admin`, { headers: getAuthHeader() }),

    // Auditor
    getAuditorRisk: () => axios.get(`${BASE_URL}/analytics/auditor/risk`, { headers: getAuthHeader() }),
    getAuditorPatterns: () => axios.get(`${BASE_URL}/analytics/auditor/patterns`, { headers: getAuthHeader() }),

    // Common / Timeline
    getExpenseTimeline: (id) => axios.get(`${BASE_URL}/expenses/${id}/timeline`, { headers: getAuthHeader() })
};
