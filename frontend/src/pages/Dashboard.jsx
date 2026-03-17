import React, { useState, useEffect } from 'react';
import api, { expenseApi, managerApi, approvalApi, forensicApi, adminApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { formatINR } from '../utils/formatCurrency';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
    ResponsiveContainer, PieChart, Pie, Cell
} from 'recharts';
import { motion } from 'framer-motion';
import {
    TrendingUp, AlertTriangle, Clock, Target, CheckCircle,
    XCircle, FileText, ShieldAlert, Eye, Edit, Zap, BarChart2,
    Users as Users2, Building, Shield as Shield2, Wallet
} from 'lucide-react';
import { Link } from 'react-router-dom';
import LiveActivityFeed from '../components/LiveActivityFeed';

// ─── Shared Primitives ──────────────────────────────────────────────────────

const COLORS = ['#00f3ff', '#bd00ff', '#ff00ff', '#4ade80', '#f59e0b'];

const STATUS_BADGE = {
    DRAFT: 'bg-gray-700 text-gray-300',
    PENDING_MANAGER: 'bg-yellow-900/60 text-yellow-300',
    PENDING_FINANCE: 'bg-orange-900/60 text-orange-300',
    APPROVED: 'bg-green-900/60 text-green-300',
    APPROVED_WITH_OVERRIDE: 'bg-teal-900/60 text-teal-300',
    REJECTED: 'bg-red-900/60 text-red-300',
    ESCALATED: 'bg-purple-900/60 text-purple-300',
    FLAGGED: 'bg-red-900/60 text-red-300',
    PAID: 'bg-blue-900/60 text-blue-300',
};

const StatCard = ({ title, value, subtext, icon: Icon, color }) => (
    <motion.div
        whileHover={{ scale: 1.02 }}
        className="glass-card p-6 flex items-start justify-between relative overflow-hidden group"
    >
        <div className={`absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity ${color}`}>
            <Icon size={100} />
        </div>
        <div>
            <p className="text-gray-400 text-xs uppercase tracking-wider mb-1">{title}</p>
            <h3 className="text-3xl font-bold text-white mb-2">{value}</h3>
            <p className="text-xs text-gray-500">{subtext}</p>
        </div>
        <div className={`p-3 rounded-xl bg-white/5 ${color} text-white`}>
            <Icon size={24} />
        </div>
    </motion.div>
);

const SectionCard = ({ title, children, delay = 0 }) => (
    <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay }}
        className="glass-card p-6"
    >
        <h3 className="text-lg font-bold text-white mb-4">{title}</h3>
        {children}
    </motion.div>
);

const StatusBadge = ({ status }) => (
    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider ${STATUS_BADGE[status] || 'bg-gray-700 text-gray-300'}`}>
        {status?.replace(/_/g, ' ')}
    </span>
);

// ─── Employee View ───────────────────────────────────────────────────────────

const EmployeeDashboard = ({ user }) => {
    const [myExpenses, setMyExpenses] = useState([]);
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            expenseApi.getMyExpenses(0, 20),
            api.get('/expenses/dashboard/summary'),
        ]).then(([expRes, sumRes]) => {
            setMyExpenses(expRes.data.content || []);
            setSummary(sumRes.data);
        }).catch(console.error).finally(() => setLoading(false));
    }, []);

    if (loading) return <div className="text-gray-400 text-sm text-center py-10">Loading your workspace...</div>;

    const now = new Date();
    const thisMonth = (e) => {
        const d = new Date(e.expenseDate || e.createdAt || '');
        return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
    };

    const draftCount = myExpenses.filter(e => e.status === 'DRAFT').length;
    const pendingCount = myExpenses.filter(e => e.status?.startsWith('PENDING')).length;
    const rejectedCount = myExpenses.filter(e => e.status === 'REJECTED').length;
    const approvedMonth = myExpenses.filter(e => e.status === 'APPROVED' && thisMonth(e)).length;
    const flaggedItems = myExpenses.filter(e => e.flagged);
    const recent5 = [...myExpenses].slice(0, 5);

    const policyAlerts = [];
    myExpenses.forEach(e => {
        if (e.flagged && e.violationDetails?.includes('RECEIPT')) policyAlerts.push({ type: 'Missing Receipt', expense: e });
        if (e.flagged && e.violationDetails?.includes('AMOUNT')) policyAlerts.push({ type: 'Over-limit Warning', expense: e });
        if (e.flagged && !e.violationDetails) policyAlerts.push({ type: 'Manager Clarification Needed', expense: e });
    });

    return (
        <div className="space-y-6">
            {/* KPI Row */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard title="Drafts" value={draftCount} subtext="Saved, not submitted" icon={FileText} color="text-gray-400" />
                <StatCard title="Pending Approval" value={pendingCount} subtext="Awaiting review" icon={Clock} color="text-yellow-400" />
                <StatCard title="Rejected" value={rejectedCount} subtext="Needs your attention" icon={XCircle} color="text-red-400" />
                <StatCard title="Approved (Month)" value={approvedMonth} subtext="Approved this month" icon={CheckCircle} color="text-green-400" />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Recent Expenses */}
                <SectionCard title="Recent Expenses" delay={0.1} >
                    {recent5.length === 0
                        ? <p className="text-gray-500 text-sm">No expenses yet.</p>
                        : <div className="space-y-3">
                            {recent5.map(e => (
                                <div key={e.id} className="flex items-center justify-between bg-white/5 rounded-lg px-3 py-2">
                                    <div className="space-y-1">
                                        <p className="text-white text-sm font-medium truncate max-w-[140px]">{e.title}</p>
                                        <div className="flex items-center gap-2">
                                            <StatusBadge status={e.status} />
                                            {e.flagged && <span className="text-[10px] text-red-400 font-bold">⚠ POLICY</span>}
                                        </div>
                                    </div>
                                    <div className="flex flex-col items-end gap-1">
                                        <span className="text-white text-sm font-bold">₹{e.amount}</span>
                                        <Link to="/submit-expense" state={{ expense: e }} className="text-[10px] text-neon-blue hover:underline">Edit</Link>
                                    </div>
                                </div>
                            ))}
                        </div>
                    }
                </SectionCard>

                {/* Policy Alerts */}
                <SectionCard title="Policy Alerts" delay={0.15}>
                    {policyAlerts.length === 0
                        ? <p className="text-green-400 text-sm">✔ No active policy alerts</p>
                        : <div className="space-y-3">
                            {policyAlerts.slice(0, 4).map((a, i) => (
                                <div key={i} className="flex items-start gap-3 bg-red-900/20 border border-red-800/40 rounded-lg px-3 py-2">
                                    <AlertTriangle size={14} className="text-red-400 mt-0.5 shrink-0" />
                                    <div>
                                        <p className="text-red-300 text-xs font-semibold">{a.type}</p>
                                        <p className="text-gray-400 text-[10px] truncate max-w-[160px]">{a.expense.title}</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    }
                </SectionCard>

                {/* Personal Spend Snapshot — Pie */}
                <SectionCard title="Category Breakdown" delay={0.2}>
                    <div className="h-52">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie data={summary?.categories || []} cx="50%" cy="50%" innerRadius={45} outerRadius={70} paddingAngle={4} dataKey="value">
                                    {(summary?.categories || []).map((_, i) => (
                                        <Cell key={i} fill={COLORS[i % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip contentStyle={{ backgroundColor: '#121721', borderColor: '#333', color: '#fff' }} />
                                <Legend iconSize={8} wrapperStyle={{ fontSize: 11 }} />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </SectionCard>
            </div>

            {/* Spend Trajectory */}
            <SectionCard title="Personal Spend Trajectory" delay={0.25}>
                <div className="h-56">
                    <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={summary?.monthly || []}>
                            <CartesianGrid strokeDasharray="3 3" stroke="#333" vertical={false} />
                            <XAxis dataKey="name" stroke="#666" fontSize={11} tickLine={false} axisLine={false} />
                            <YAxis stroke="#666" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(value) => `₹${value.toLocaleString('en-IN')}`} />
                            <Tooltip contentStyle={{ backgroundColor: '#121721', borderColor: '#333', color: '#fff' }} itemStyle={{ color: '#00f3ff' }} formatter={(value, name) => [formatINR(value), name]} />
                            <Bar dataKey="amount" radius={[4, 4, 0, 0]}>
                                {(summary?.monthly || []).map((_, i) => (
                                    <Cell key={i} fill={i % 2 === 0 ? '#00f3ff' : '#bd00ff'} />
                                ))}
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </SectionCard>
        </div>
    );
};

// ─── Manager View ────────────────────────────────────────────────────────────

const ManagerDashboard = ({ user }) => {
    const [teamSummary, setTeamSummary] = useState(null);
    const [pending, setPending] = useState([]);
    const [overrides, setOverrides] = useState([]);
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [actioning, setActioning] = useState(null);

    useEffect(() => {
        Promise.all([
            managerApi.getTeamSummary(),
            approvalApi.getPending(0, 5),
            managerApi.getOverrides(),
            api.get('/expenses/dashboard/summary'),
        ]).then(([tsRes, pendRes, ovRes, sumRes]) => {
            setTeamSummary(tsRes.data);
            setPending(pendRes.data.content || []);
            setOverrides(ovRes.data || []);
            setSummary(sumRes.data);
        }).catch(console.error).finally(() => setLoading(false));
    }, []);

    const handleApproval = async (id, action) => {
        setActioning(id);
        try {
            await approvalApi[action === 'APPROVE' ? 'approve' : 'reject'](id, `Quick ${action} from Dashboard`);
            setPending(prev => prev.filter(e => e.id !== id));
        } catch (e) {
            console.error(e);
        } finally { setActioning(null); }
    };

    if (loading) return <div className="text-gray-400 text-sm text-center py-10">Loading team data...</div>;

    const deptSpend = teamSummary?.totalSpend ?? 0;
    const violations = teamSummary?.policyExceptionsCount ?? 0;
    const overrideCount = teamSummary?.overrideCount ?? 0;

    return (
        <div className="space-y-6">

            {/* Role Identity Header */}
            <div className="glass-card px-5 py-3 flex items-center justify-between border border-yellow-500/20">
                <div>
                    <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-0.5">Role</p>
                    <p className="text-white text-sm font-bold">Finance Manager</p>
                </div>
                <div className="text-center">
                    <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-0.5">Department</p>
                    <p className="text-yellow-400 text-sm font-bold">{teamSummary?.department || 'Unassigned'}</p>
                </div>
                <div className="text-right">
                    <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-0.5">Scope</p>
                    <p className="text-gray-300 text-sm">Dept-Scoped Approval Authority</p>
                </div>
            </div>

            {/* KPI Row */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard title="Pending Approvals" value={pending.length} subtext="Awaiting your decision" icon={Clock} color="text-yellow-400" />
                <StatCard title="Policy Violations" value={violations} subtext="This period" icon={AlertTriangle} color="text-red-400" />
                <StatCard title="Overrides Used" value={overrideCount} subtext="Month-to-date" icon={Zap} color="text-purple-400" />
                <StatCard title="Dept Monthly Spend" value={formatINR(deptSpend)} subtext={teamSummary?.department || 'Your department'} icon={TrendingUp} color="text-neon-blue" />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Approval Queue Preview */}
                <SectionCard title="Approval Queue" delay={0.1}>
                    {pending.length === 0
                        ? <p className="text-green-400 text-sm">✔ No pending approvals</p>
                        : <div className="space-y-3">
                            {pending.map(e => (
                                <div key={e.id} className="bg-white/5 rounded-lg px-3 py-3 flex items-center justify-between gap-3">
                                    <div className="flex-1 min-w-0">
                                        <p className="text-white text-sm font-medium truncate">{e.title}</p>
                                        <p className="text-gray-400 text-[11px]">{e.submittedBy} · <span className="font-bold text-white">₹{e.amount}</span></p>
                                        {e.flagged && <span className="text-[10px] text-red-400 font-bold">⚠ High Risk</span>}
                                    </div>
                                    <div className="flex gap-2 shrink-0">
                                        <button
                                            disabled={actioning === e.id}
                                            onClick={() => handleApproval(e.id, 'APPROVE')}
                                            className="text-[11px] px-2 py-1 rounded-md bg-green-700/60 text-green-300 hover:bg-green-700 transition disabled:opacity-50"
                                        >✔ Approve</button>
                                        <button
                                            disabled={actioning === e.id}
                                            onClick={() => handleApproval(e.id, 'REJECT')}
                                            className="text-[11px] px-2 py-1 rounded-md bg-red-700/60 text-red-300 hover:bg-red-700 transition disabled:opacity-50"
                                        >✘ Reject</button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    }
                </SectionCard>

                {/* Override Log Preview */}
                <SectionCard title="Recent Overrides" delay={0.15}>
                    {overrides.length === 0
                        ? <p className="text-gray-500 text-sm">No overrides this period.</p>
                        : <div className="space-y-3">
                            {overrides.slice(0, 5).map(o => (
                                <div key={o.id} className="bg-white/5 rounded-lg px-3 py-2">
                                    <div className="flex justify-between items-center mb-1">
                                        <span className="text-white text-xs font-semibold">Expense #{o.expenseId}</span>
                                        <span className="text-[10px] text-purple-400">{o.ruleViolated}</span>
                                    </div>
                                    <p className="text-gray-400 text-[11px] truncate">{o.justification}</p>
                                </div>
                            ))}
                        </div>
                    }
                </SectionCard>
            </div>

            <div className="grid grid-cols-1 gap-6">
                <LiveActivityFeed />
            </div>

            {/* Spend Trajectory */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <SectionCard title="Department Spend Trajectory" delay={0.2}>
                    <div className="h-56">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={summary?.monthly || []}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#333" vertical={false} />
                                <XAxis dataKey="name" stroke="#666" fontSize={11} tickLine={false} axisLine={false} />
                                <YAxis stroke="#666" fontSize={11} tickLine={false} axisLine={false} />
                                <Tooltip contentStyle={{ backgroundColor: '#121721', borderColor: '#333', color: '#fff' }} itemStyle={{ color: '#00f3ff' }} />
                                <Bar dataKey="amount" radius={[4, 4, 0, 0]}>
                                    {(summary?.monthly || []).map((_, i) => (
                                        <Cell key={i} fill={i % 2 === 0 ? '#00f3ff' : '#bd00ff'} />
                                    ))}
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </SectionCard>

                <SectionCard title="Category Breakdown" delay={0.25}>
                    <div className="h-56">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie data={summary?.categories || []} cx="50%" cy="50%" innerRadius={45} outerRadius={70} paddingAngle={4} dataKey="value">
                                    {(summary?.categories || []).map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                                </Pie>
                                <Tooltip contentStyle={{ backgroundColor: '#121721', borderColor: '#333', color: '#fff' }} />
                                <Legend iconSize={8} wrapperStyle={{ fontSize: 11 }} />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </SectionCard>
            </div>
        </div>
    );
};

// ─── Auditor View ────────────────────────────────────────────────────────────

const AuditorDashboard = ({ user }) => {
    const [flagged, setFlagged] = useState([]);
    const [logs, setLogs] = useState([]);
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            forensicApi.getFlagged(0, 8),
            forensicApi.getLogs(0, 5),
            api.get('/expenses/dashboard/summary'),
        ]).then(([fRes, lRes, sRes]) => {
            setFlagged(fRes.data.content || []);
            setLogs(lRes.data.content || []);
            setSummary(sRes.data);
        }).catch(console.error).finally(() => setLoading(false));
    }, []);

    if (loading) return <div className="text-gray-400 text-sm text-center py-10">Loading forensic data...</div>;

    const enterpriseSpend = summary?.totalApproved ?? 0;
    const flaggedCount = flagged.length;
    const overrideItems = flagged.filter(e => e.status === 'APPROVED_WITH_OVERRIDE').length;

    return (
        <div className="space-y-6">
            {/* KPI Row */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard title="Enterprise Spend" value={formatINR(enterpriseSpend)} subtext="Total approved this period" icon={TrendingUp} color="text-neon-blue" />
                <StatCard title="Flagged Transactions" value={flaggedCount} subtext="Require investigation" icon={ShieldAlert} color="text-red-400" />
                <StatCard title="Override Frequency" value={overrideItems} subtext="Policy-overridden approvals" icon={Zap} color="text-purple-400" />
                <StatCard title="Audit Log Entries" value={logs.length > 0 ? `${logs.length}+` : '0'} subtext="Recent system events" icon={BarChart2} color="text-yellow-400" />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Flagged Items Table */}
                <SectionCard title="Flagged Items — Forensic Preview" delay={0.1}>
                    {flagged.length === 0
                        ? <p className="text-green-400 text-sm">✔ No flagged expenses</p>
                        : <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                            {flagged.map(e => (
                                <div key={e.id} className="bg-red-900/10 border border-red-800/30 rounded-lg px-3 py-2">
                                    <div className="flex items-center justify-between mb-1">
                                        <span className="text-white text-xs font-semibold">#{e.id} · {e.title}</span>
                                        <StatusBadge status={e.status} />
                                    </div>
                                    <div className="text-gray-400 text-[11px] flex gap-3">
                                        <span>Dept: <span className="text-white">{e.departmentName || '—'}</span></span>
                                        <span>Amt: <span className="text-white">₹{e.amount}</span></span>
                                        {e.violationDetails && <span className="text-red-400 truncate max-w-[100px]">{e.violationDetails}</span>}
                                    </div>
                                </div>
                            ))}
                        </div>
                    }
                </SectionCard>

                <SectionCard title="Live System Activity" delay={0.15}>
                    <LiveActivityFeed />
                </SectionCard>
            </div>

            {/* Enterprise Spend Chart */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <SectionCard title="Enterprise Spend Trajectory" delay={0.2}>
                    <div className="h-56">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={summary?.monthly || []}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#333" vertical={false} />
                                <XAxis dataKey="name" stroke="#666" fontSize={11} tickLine={false} axisLine={false} />
                                <YAxis stroke="#666" fontSize={11} tickLine={false} axisLine={false} />
                                <Tooltip contentStyle={{ backgroundColor: '#121721', borderColor: '#333', color: '#fff' }} itemStyle={{ color: '#00f3ff' }} />
                                <Bar dataKey="amount" radius={[4, 4, 0, 0]}>
                                    {(summary?.monthly || []).map((_, i) => <Cell key={i} fill={i % 2 === 0 ? '#00f3ff' : '#bd00ff'} />)}
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </SectionCard>

                <SectionCard title="Category Distribution" delay={0.25}>
                    <div className="h-56">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie data={summary?.categories || []} cx="50%" cy="50%" innerRadius={45} outerRadius={70} paddingAngle={4} dataKey="value">
                                    {(summary?.categories || []).map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                                </Pie>
                                <Tooltip contentStyle={{ backgroundColor: '#121721', borderColor: '#333', color: '#fff' }} />
                                <Legend iconSize={8} wrapperStyle={{ fontSize: 11 }} />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </SectionCard>
            </div>
        </div>
    );
};

// ─── Admin Overview ──────────────────────────────────────────────────────────

const AdminDashboard = ({ user }) => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.allSettled([
            adminApi.getUsers(0, 1),
            adminApi.getDepartments(),
            adminApi.getRules(0, 1),
            adminApi.getBudgets(0, 1),
            adminApi.getLogs(0, 10),
        ]).then(([uRes, dRes, rRes, bRes, lRes]) => {
            setStats({
                totalUsers: uRes.status === 'fulfilled' ? (uRes.value.data.totalElements ?? 0) : 0,
                totalDepts: dRes.status === 'fulfilled' ? (dRes.value.data || []).length : 0,
                totalRules: rRes.status === 'fulfilled' ? (rRes.value.data.totalElements ?? 0) : 0,
                totalBudgets: bRes.status === 'fulfilled' ? (bRes.value.data.totalElements ?? 0) : 0,
                recentLogs: lRes.status === 'fulfilled' ? (lRes.value.data.content || []) : [],
            });
        }).finally(() => setLoading(false));
    }, []);

    if (loading) return <div className="text-gray-400 text-sm text-center py-10">Loading system metrics...</div>;
    if (!stats) return <div className="text-red-400 text-sm text-center py-10">Failed to load metrics.</div>;

    return (
        <div className="space-y-6">
            {/* Role Identity */}
            <div className="glass-card px-5 py-3 flex items-center justify-between border border-red-500/20">
                <div><p className="text-[10px] text-gray-500 uppercase tracking-widest mb-0.5">Role</p><p className="text-white text-sm font-bold">System Administrator</p></div>
                <div className="text-center"><p className="text-[10px] text-gray-500 uppercase tracking-widest mb-0.5">Scope</p><p className="text-red-400 text-sm font-bold">Enterprise-Wide</p></div>
                <div className="text-right"><p className="text-[10px] text-gray-500 uppercase tracking-widest mb-0.5">Access</p><p className="text-gray-300 text-sm">User · Dept · Policy · Logs</p></div>
            </div>

            {/* KPI Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard title="Total Users" value={stats.totalUsers} subtext="Registered accounts" icon={Users2} color="text-neon-blue" />
                <StatCard title="Departments" value={stats.totalDepts} subtext="Active organizational units" icon={Building} color="text-green-400" />
                <StatCard title="Compliance Rules" value={stats.totalRules} subtext="Policy engine rules" icon={Shield2} color="text-yellow-400" />
                <StatCard title="Budgets Tracked" value={stats.totalBudgets} subtext="Dept budget limits" icon={Wallet} color="text-purple-400" />
            </div>

            {/* Recent System Logs */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <SectionCard title="Recent System Events" delay={0.15}>
                    {stats.recentLogs.length === 0
                        ? <p className="text-gray-500 text-sm">No recent log entries.</p>
                        : <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                            {stats.recentLogs.map((l, i) => (
                                <div key={l.id || i} className="bg-white/5 rounded-lg px-4 py-2 flex items-start justify-between gap-4">
                                    <div className="flex-1 min-w-0">
                                        <p className="text-white text-xs font-bold">{l.action}</p>
                                        <p className="text-gray-400 text-[11px]">{l.performedBy} · {l.entityType}</p>
                                    </div>
                                    <span className="text-[10px] text-gray-500 shrink-0">{new Date(l.timestamp).toLocaleTimeString()}</span>
                                </div>
                            ))}
                        </div>
                    }
                </SectionCard>
                <LiveActivityFeed />
            </div>
        </div>
    );
};

// ─── Root Dashboard ──────────────────────────────────────────────────────────

const Dashboard = () => {
    const { user } = useAuth();

    const isManager = user?.roles?.includes('ROLE_MANAGER');
    const isAuditor = user?.roles?.includes('ROLE_AUDITOR');
    const isAdmin = user?.roles?.includes('ROLE_ADMIN');

    const roleLabel = isAdmin ? 'Administrator'
        : isManager ? 'Finance Manager'
            : isAuditor ? 'Auditor'
                : 'Employee';

    return (
        <div className="space-y-6">
            <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
                <h1 className="text-2xl font-bold text-white mb-1">Mission Control</h1>
                <p className="text-gray-400 text-sm">
                    Welcome back, <span className="text-white font-semibold">{user?.username}</span>
                    <span className="ml-2 text-[11px] text-neon-blue bg-neon-blue/10 px-2 py-0.5 rounded-full">{roleLabel}</span>
                </p>
            </motion.div>

            {isAdmin && <AdminDashboard user={user} />}
            {isManager && !isAdmin && <ManagerDashboard user={user} />}
            {isAuditor && !isAdmin && <AuditorDashboard user={user} />}
            {!isManager && !isAuditor && !isAdmin && <EmployeeDashboard user={user} />}
        </div>
    );
};

export default Dashboard;
