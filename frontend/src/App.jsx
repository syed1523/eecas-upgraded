import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Dashboard from './pages/Dashboard';
import SubmitExpense from './pages/SubmitExpense';
import MyExpenses from './pages/MyExpenses';
import Approvals from './pages/Approvals';
import Admin from './pages/Admin';
import ManagerHub from './pages/ManagerHub';
import AuditorCenter from './pages/AuditorCenter';
import FinanceDesk from './pages/FinanceDesk';
import NLQueryPage from './pages/NLQueryPage';
import PrivateRoute, { RoleRoute } from './components/PrivateRoute';
import { AuthProvider } from './context/AuthContext';
import './index.css';

import MainLayout from './layouts/MainLayout';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />

          {/* All routes require authentication */}
          <Route element={<PrivateRoute />}>
            <Route element={<MainLayout />}>

              {/* Overview — all authenticated users */}
              <Route path="/" element={<Dashboard />} />

              {/* EMPLOYEE only — ADMIN explicitly excluded from expense submission */}
              <Route element={<RoleRoute allowed={['ROLE_EMPLOYEE']} />}>
                <Route path="/submit-expense" element={<SubmitExpense />} />
                <Route path="/my-expenses" element={<MyExpenses />} />
              </Route>

              {/* Approvals — Manager and Finance only */}
              <Route element={<RoleRoute allowed={['ROLE_MANAGER', 'ROLE_FINANCE']} />}>
                <Route path="/approvals" element={<Approvals />} />
              </Route>

              {/* Manager Hub — Manager only */}
              <Route element={<RoleRoute allowed={['ROLE_MANAGER']} />}>
                <Route path="/manager-hub" element={<ManagerHub />} />
              </Route>

              {/* Auditor Center — Auditors only */}
              <Route element={<RoleRoute allowed={['ROLE_AUDITOR']} />}>
                <Route path="/auditor-center" element={<AuditorCenter />} />
              </Route>

              {/* Finance Desk — Finance only */}
              <Route element={<RoleRoute allowed={['ROLE_FINANCE']} />}>
                <Route path="/finance-desk" element={<FinanceDesk />} />
                <Route path="/finance/nl-query" element={<NLQueryPage />} />
              </Route>

              {/* Admin Governance Portal — ADMIN only */}
              <Route element={<RoleRoute allowed={['ROLE_ADMIN']} />}>
                <Route path="/admin" element={<Admin />} />
              </Route>

            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
