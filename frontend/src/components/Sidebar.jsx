import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
    LayoutDashboard,
    PlusCircle,
    FileText,
    CheckSquare,
    ShieldCheck,
    DollarSign,
    LogOut,
    Eye,
    Shield,
} from 'lucide-react';
import { motion } from 'framer-motion';

const ROLE_BADGE = {
    ROLE_AUDITOR: { label: 'Auditor', color: 'text-purple-400 bg-purple-400/10' },
    ROLE_MANAGER: { label: 'Finance Manager', color: 'text-yellow-400 bg-yellow-400/10' },
    ROLE_FINANCE: { label: 'Finance', color: 'text-blue-400 bg-blue-400/10' },
    ROLE_EMPLOYEE: { label: 'Employee', color: 'text-green-400 bg-green-400/10' },
    ROLE_ADMIN: { label: 'Administrator', color: 'text-red-400 bg-red-400/10' },
};

const Sidebar = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const isAuditor = user?.roles?.includes('ROLE_AUDITOR');
    const isManager = user?.roles?.includes('ROLE_MANAGER');
    const isFinance = user?.roles?.includes('ROLE_FINANCE');
    const isEmployee = user?.roles?.includes('ROLE_EMPLOYEE');
    const isAdmin = user?.roles?.includes('ROLE_ADMIN');

    const primaryRole = user?.roles?.find(r => ROLE_BADGE[r]);
    const badge = primaryRole ? ROLE_BADGE[primaryRole] : null;

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    // ── Nav items are strictly role-scoped ───────────────────────────────────
    // ADMIN is a governance-only role:
    //   - NO Submit Expense   (not in expense lifecycle)
    //   - NO My Expenses      (no personal financial records)
    //   - NO Approval Queue   (not a transaction approver)
    //   - ONLY: Overview + Admin Governance Portal
    const navItems = [
        { path: '/', label: 'Overview', icon: LayoutDashboard },

        // EMPLOYEE ONLY — ADMIN explicitly excluded
        ...(isEmployee ? [
            { path: '/submit-expense', label: 'Submit Expense', icon: PlusCircle },
            { path: '/my-expenses', label: 'My Expenses', icon: FileText },
        ] : []),

        // Role-specific sections — each role gets only their domain
        ...(isManager ? [{ path: '/manager-hub', label: 'Manager Hub', icon: CheckSquare }] : []),
        ...(isAuditor ? [{ path: '/auditor-center', label: 'Forensic Center', icon: ShieldCheck }] : []),
        ...(isFinance ? [{ path: '/finance-desk', label: 'Finance Desk', icon: DollarSign }] : []),

        // ADMIN ONLY — governance nav
        ...(isAdmin ? [{ path: '/admin', label: 'Admin Portal', icon: Shield }] : []),
    ];

    return (
        <motion.div
            initial={{ x: -50, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            className="w-64 h-screen fixed left-0 top-0 glass-card rounded-l-none border-l-0 border-y-0 z-50 flex flex-col"
        >
            {/* Brand + Identity */}
            <div className="p-6 border-b border-white/10">
                <h1 className="text-2xl font-bold bg-clip-text text-transparent bg-primary-gradient uppercase tracking-widest">
                    EECAS Enterprise
                </h1>
                <p className="text-xs text-gray-500 tracking-wider mt-1">
                    Logged in as <span className="text-neon-blue">{user?.username}</span>
                </p>
                {badge && (
                    <span className={`inline-block mt-2 text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full ${badge.color}`}>
                        {badge.label}
                    </span>
                )}
                {isAdmin && (
                    <p className="text-[10px] text-red-500/70 mt-1 flex items-center gap-1">
                        <Shield size={10} /> Governance · No expense access
                    </p>
                )}
                {isAuditor && (
                    <p className="text-[10px] text-gray-600 mt-1 flex items-center gap-1">
                        <Eye size={10} /> Enterprise-wide · Read-only
                    </p>
                )}
                {isManager && (
                    <p className="text-[10px] text-gray-600 mt-1 flex items-center gap-1">
                        <CheckSquare size={10} /> Dept-Scoped · Approval Authority
                    </p>
                )}
            </div>

            <nav className="flex-1 overflow-y-auto py-6 px-3 space-y-2">
                {navItems.map((item) => (
                    <NavLink
                        key={item.path}
                        to={item.path}
                        className={({ isActive }) => `
                            flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-300 group
                            ${isActive
                                ? 'bg-primary-gradient shadow-neon text-white'
                                : 'text-gray-400 hover:bg-white/5 hover:text-white'}
                        `}
                    >
                        <item.icon size={20} className="group-hover:scale-110 transition-transform" />
                        <span className="font-medium text-sm tracking-wide">{item.label}</span>
                    </NavLink>
                ))}
            </nav>

            <div className="p-4 border-t border-white/10">
                <button
                    onClick={handleLogout}
                    className="flex items-center gap-3 px-4 py-3 w-full rounded-xl text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-all duration-300"
                >
                    <LogOut size={20} />
                    <span className="font-medium text-sm tracking-wide">Disconnect</span>
                </button>
            </div>
        </motion.div>
    );
};

export default Sidebar;
