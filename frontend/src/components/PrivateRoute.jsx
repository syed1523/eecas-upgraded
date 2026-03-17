import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * PrivateRoute — blocks unauthenticated access to any protected route.
 */
const PrivateRoute = () => {
    const { user, loading } = useAuth();
    if (loading) return <div>Loading...</div>;
    return user ? <Outlet /> : <Navigate to="/login" />;
};

/**
 * RoleRoute — blocks access to routes that require specific roles.
 * Auditors attempting /submit-expense, /my-expenses, /manager-hub, etc. are
 * redirected to "/" instead of seeing a blank or broken page.
 *
 * Usage: <RoleRoute allowed={['ROLE_EMPLOYEE', 'ROLE_MANAGER']} />
 */
export const RoleRoute = ({ allowed }) => {
    const { user } = useAuth();
    const hasRole = user?.roles?.some(r => allowed.includes(r));
    return hasRole ? <Outlet /> : <Navigate to="/" replace />;
};

export default PrivateRoute;
