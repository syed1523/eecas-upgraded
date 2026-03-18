import React, { useEffect, useState } from 'react';
import { managerApi } from '../services/api';
import { analyticsApi } from '../services/analyticsApi';
import axios from 'axios';
import { motion, AnimatePresence } from 'framer-motion';
import { ShieldAlert, AlertTriangle, CheckCircle, XCircle, BarChart2, TrendingUp, Search, Activity, Zap, ChevronDown, ChevronUp, HelpCircle, History } from 'lucide-react';
import ExplainPanel from '../components/ExplainPanel';
import ExpenseTimeline from '../components/ExpenseTimeline';

const AuditorCenter = () => {
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedExpense, setSelectedExpense] = useState(null);
    const [actionType, setActionType] = useState(''); // CLEAR, FRAUD
    const [reason, setReason] = useState('');
    const [processing, setProcessing] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [toast, setToast] = useState(null);
    const [underReviewIds, setUnderReviewIds] = useState(new Set());
    const [highRiskFilter, setHighRiskFilter] = useState(false);

    const [riskProfile, setRiskProfile] = useState(null);
    const [patterns, setPatterns] = useState(null);
    const [historyExpenseId, setHistoryExpenseId] = useState(null);

    // Create a local Axios instance for forensics since they aren't completely mapped in api.js yet to save time
    const user = JSON.parse(localStorage.getItem('user'));
    const forensicApi = axios.create({
        baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082/api',
        headers: { Authorization: `Bearer ${user?.token}` }
    });

    useEffect(() => {
        analyticsApi.getAuditorRisk().then(r => setRiskProfile(r.data)).catch(console.error);
        analyticsApi.getAuditorPatterns().then(r => setPatterns(r.data)).catch(console.error);
    }, []);

    useEffect(() => {
        fetchTasks();
    }, [page]);

    const fetchTasks = async () => {
        setLoading(true);
        try {
            const res = await forensicApi.get(`/forensics/expenses?page=${page}&size=10&flagged=true`);
            setTasks(res.data.content || []);
            setTotalPages(res.data.totalPages || 0);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const showToast = (message) => {
        setToast(message);
        setTimeout(() => setToast(null), 3000);
    };

    const handleAction = async () => {
        if (!selectedExpense) return;
        setProcessing(true);
        try {
            await new Promise(resolve => setTimeout(resolve, 1000));
            if (actionType === 'INVESTIGATE') {
                const res = await forensicApi.post(`/forensics/expenses/${selectedExpense.id}/investigate`);
                setUnderReviewIds(prev => new Set(prev).add(selectedExpense.id));
                showToast(`Investigation #${res.data.investigationId} opened successfully`);
            } else {
                const payload = {
                    reason: reason,
                    status: actionType === 'CLEAR' ? 'REVIEWED' : 'OPEN'
                };
                await forensicApi.post(`/forensics/expenses/${selectedExpense.id}/findings`, payload);
                showToast('Forensic Finding successfully logged against record.');
            }

            closeModal();
        } catch (err) {
            alert('Action failed: ' + (err.response?.data?.message || err.message));
        } finally {
            setProcessing(false);
        }
    };

    const updateInvStatus = async (id, status) => {
        try {
            await forensicApi.patch(`/forensics/investigations/${id}`, { status });
            alert('Investigation ' + status);
        } catch (err) {
            alert('Failed to update investigation: ' + (err.response?.data?.message || err.message));
        }
    };

    const openModal = (expense, type) => {
        setSelectedExpense(expense);
        setActionType(type);
        setReason('');
    };

    const closeModal = () => {
        setSelectedExpense(null);
        setActionType('');
        setReason('');
    };

    const [activeTab, setActiveTab] = useState('alerts');
    const [investigations, setInvestigations] = useState([]);

    // Anomaly Detection state
    const [anomalies, setAnomalies] = useState([]);
    const [anomalyLoading, setAnomalyLoading] = useState(false);
    const [expandedAnomaly, setExpandedAnomaly] = useState(null);

    // Explainable Flagging state
    const [explainExpenseId, setExplainExpenseId] = useState(null);

    useEffect(() => {
        if (activeTab === 'investigations') {
            forensicApi.get('/forensics/investigations').then(r => setInvestigations(r.data || [])).catch(console.error);
        }
        if (activeTab === 'anomalies') {
            setAnomalyLoading(true);
            forensicApi.get('/forensics/anomalies')
                .then(r => setAnomalies(r.data || []))
                .catch(console.error)
                .finally(() => setAnomalyLoading(false));
        }
    }, [activeTab]);

    const criticalCount = tasks.filter(e => e.riskLevel === 'CRITICAL').length;
    const highCount = tasks.filter(e => e.riskLevel === 'HIGH').length;
    const flaggedToday = tasks.filter(e => {
        if (!e.expenseDate) return false;
        return new Date(e.expenseDate).toDateString() === new Date().toDateString();
    }).length;
    const cleanCount = tasks.filter(e => !e.flagCount || e.flagCount === 0).length;

    return (
        <div className="space-y-6">
            {/* Risk Summary Cards */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white/5 backdrop-blur-xl border border-red-500/20 rounded-2xl p-4">
                    <p className="text-[10px] text-gray-500 uppercase tracking-widest font-bold">Critical Risk</p>
                    <p className="text-3xl font-black text-red-400 mt-1 drop-shadow-[0_0_8px_rgba(248,113,113,0.5)]">{criticalCount}</p>
                </div>
                <div className="bg-white/5 backdrop-blur-xl border border-orange-500/20 rounded-2xl p-4">
                    <p className="text-[10px] text-gray-500 uppercase tracking-widest font-bold">High Risk</p>
                    <p className="text-3xl font-black text-orange-400 mt-1 drop-shadow-[0_0_8px_rgba(249,115,22,0.5)]">{highCount}</p>
                </div>
                <div className="bg-white/5 backdrop-blur-xl border border-yellow-500/20 rounded-2xl p-4">
                    <p className="text-[10px] text-gray-500 uppercase tracking-widest font-bold">Flagged Today</p>
                    <p className="text-3xl font-black text-yellow-400 mt-1 drop-shadow-[0_0_8px_rgba(250,204,21,0.5)]">{flaggedToday}</p>
                </div>
                <div className="bg-white/5 backdrop-blur-xl border border-green-500/20 rounded-2xl p-4">
                    <p className="text-[10px] text-gray-500 uppercase tracking-widest font-bold">Clean Expenses</p>
                    <p className="text-3xl font-black text-green-400 mt-1 drop-shadow-[0_0_8px_rgba(74,222,128,0.5)]">{cleanCount}</p>
                </div>
            </div>

            <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className="flex justify-between items-end">
                <div>
                    <h1 className="text-2xl font-bold text-white">Auditor Center</h1>
                    <p className="text-gray-400 text-sm">Review escalated items and manage formal investigations.</p>
                </div>

                <div className="flex bg-white/5 p-1 rounded-xl border border-white/10">
                    <button onClick={() => setActiveTab('alerts')} className={`px-4 py-2 text-sm font-medium rounded-lg transition-all ${activeTab === 'alerts' ? 'bg-primary-gradient text-white shadow-neon' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}>Active Alerts</button>
                    <button onClick={() => setActiveTab('anomalies')} className={`px-4 py-2 text-sm font-medium rounded-lg transition-all relative ${activeTab === 'anomalies' ? 'bg-primary-gradient text-white shadow-neon' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}>
                        Anomaly Detection
                        {anomalies.length > 0 && (
                            <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center shadow-lg animate-pulse">{anomalies.length}</span>
                        )}
                    </button>
                    <button onClick={() => setActiveTab('investigations')} className={`px-4 py-2 text-sm font-medium rounded-lg transition-all ${activeTab === 'investigations' ? 'bg-primary-gradient text-white shadow-neon' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}>Investigations</button>
                    <button onClick={() => setActiveTab('analytics')} className={`px-4 py-2 text-sm font-medium rounded-lg transition-all ${activeTab === 'analytics' ? 'bg-primary-gradient text-white shadow-neon' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}>Risk & Patterns</button>
                </div>
            </motion.div>

            <AnimatePresence mode="wait">
                <motion.div
                    key={activeTab}
                    initial={{ opacity: 0, x: 10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -10 }}
                    className="glass-card p-6"
                >
                    <div className="flex items-center justify-between mb-6">
                        <div className="flex items-center gap-2">
                            <ShieldAlert size={24} className="text-red-500" />
                            <h2 className="text-lg font-bold text-white">Compliance Alerts ({tasks.length})</h2>
                        </div>
                        <div className="flex items-center gap-2 text-sm bg-white/5 px-3 py-1.5 rounded-lg border border-white/10">
                            <label className="text-gray-400 flex items-center gap-2 cursor-pointer">
                                <input
                                    type="checkbox"
                                    checked={highRiskFilter}
                                    onChange={(e) => setHighRiskFilter(e.target.checked)}
                                    className="accent-red-500 w-4 h-4 cursor-pointer"
                                />
                                High Risk Only
                            </label>
                        </div>
                    </div>

                    {loading ? <p className="text-gray-400 text-center py-12">Loading audit stream...</p> : tasks.length === 0 ? (
                        <div className="text-center py-12 text-gray-500">System Secure. No items requiring audit.</div>
                    ) : (
                        <div className="space-y-4">
                            {tasks
                                .filter(expense => !highRiskFilter || (expense.riskScore && expense.riskScore >= 75) || expense.fraudIndicator)
                                .map((expense, index) => (
                                    <motion.div
                                        initial={{ opacity: 0, x: -10 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        transition={{ delay: index * 0.1 }}
                                        key={expense.id}
                                        className="border border-red-500/20 bg-red-500/5 rounded-xl p-4 hover:bg-red-500/10 transition group"
                                    >
                                        <div className="flex flex-col md:flex-row justify-between items-start gap-4">
                                            <div className="flex-1">
                                                <div className="flex items-center gap-3 mb-2 flex-wrap">
                                                    <h3 className="font-bold text-lg text-white">{expense.title}</h3>
                                                    <span className="text-xs bg-red-500/20 text-red-400 px-2 py-0.5 rounded border border-red-500/30 uppercase tracking-wider">Flagged</span>
                                                    {expense.riskLevel && expense.riskLevel !== 'LOW' && (
                                                        <span className={`text-xs px-2 py-0.5 rounded-full border uppercase tracking-wider font-bold ${expense.riskLevel === 'CRITICAL' ? 'bg-red-500/30 text-red-300 border-red-500/50'
                                                            : expense.riskLevel === 'HIGH' ? 'bg-orange-500/30 text-orange-300 border-orange-500/50'
                                                                : 'bg-yellow-500/30 text-yellow-300 border-yellow-500/50'
                                                            }`}>{expense.riskLevel}</span>
                                                    )}
                                                    {expense.anomalyScore && (
                                                        <span className={`text-xs px-2 py-0.5 rounded border uppercase tracking-wider font-bold ${expense.anomalyScore > 3 ? 'bg-red-500/20 text-red-400 border-red-500/50' : 'bg-orange-500/20 text-orange-400 border-orange-500/50'}`}>
                                                            Z-Score: {expense.anomalyScore.toFixed(1)}
                                                        </span>
                                                    )}
                                                </div>
                                                <div className="text-sm text-gray-400 flex items-center gap-4">
                                                    <span>User: <span className="text-white">{expense.user?.username}</span></span>
                                                    <span>•</span>
                                                    <span className="font-mono text-white">₹{expense.amount}</span>
                                                    {expense.flagCount > 0 && <span>• <span className="text-red-400 font-bold">{expense.flagCount} flag{expense.flagCount > 1 ? 's' : ''}</span></span>}
                                                </div>
                                            </div>
                                            <div className="flex flex-col gap-2 w-full md:w-auto min-w-[140px]">
                                                <button
                                                    onClick={() => setExplainExpenseId(explainExpenseId === expense.id ? null : expense.id)}
                                                    className="py-2 px-4 rounded-lg bg-white/10 text-white hover:bg-white/20 border border-white/20 transition-all flex items-center justify-center gap-2 font-medium text-sm"
                                                >
                                                    <HelpCircle size={16} /> Why Flagged?
                                                </button>
                                                <button
                                                    onClick={() => openModal(expense, 'REVIEWED')}
                                                    className="py-2 px-4 rounded-lg bg-blue-500/10 text-blue-400 hover:bg-blue-500 hover:text-white border border-blue-500/30 transition-all flex items-center justify-center gap-2 font-medium text-sm"
                                                >
                                                    <CheckCircle size={16} /> Log Review
                                                </button>
                                                {underReviewIds.has(expense.id) ? (
                                                    <span className="py-2 px-4 rounded-lg bg-blue-500/20 text-blue-300 border border-blue-500/40 flex items-center justify-center gap-2 font-medium text-sm cursor-default">
                                                        <CheckCircle size={16} /> Under Review
                                                    </span>
                                                ) : (
                                                    <button
                                                        onClick={() => openModal(expense, 'INVESTIGATE')}
                                                        className="py-2 px-4 rounded-lg bg-orange-500/10 text-orange-400 hover:bg-orange-500 hover:text-white border border-orange-500/30 transition-all flex items-center justify-center gap-2 font-medium text-sm"
                                                    >
                                                        <AlertTriangle size={16} /> Open Case
                                                    </button>
                                                )}
                                                <button
                                                    onClick={() => setHistoryExpenseId(expense.id)}
                                                    className="py-2 px-4 rounded-lg bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500 hover:text-white border border-emerald-500/30 transition-all flex items-center justify-center gap-2 font-medium text-sm"
                                                >
                                                    <History size={16} /> View History
                                                </button>
                                            </div>
                                        </div>
                                        <AnimatePresence>
                                            {explainExpenseId === expense.id && (
                                                <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }} className="mt-4 pt-4 border-t border-white/10">
                                                    <ExplainPanel expense={expense} />
                                                </motion.div>
                                            )}
                                        </AnimatePresence>
                                    </motion.div>
                                ))}
                        </div>
                    )}
                    {activeTab === 'alerts' && totalPages > 1 && (
                        <div className="p-4 border-t border-white/10 flex justify-between items-center mt-4 glass-card">
                            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="px-4 py-2 border border-white/10 rounded disabled:opacity-50 text-white text-sm">Previous</button>
                            <span className="text-gray-400 text-sm">Page {page + 1} of {totalPages}</span>
                            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="px-4 py-2 border border-white/10 rounded disabled:opacity-50 text-white text-sm">Next</button>
                        </div>
                    )}

                    {activeTab === 'investigations' && (
                        <div className="space-y-4">
                            {investigations.length === 0 ? <p className="text-gray-500 text-center py-12">No active investigations.</p> : investigations.map(inv => (
                                <div key={inv.id} className="p-4 rounded-xl bg-white/5 border border-white/10 flex justify-between items-center group hover:bg-white/10 transition">
                                    <div className="space-y-1">
                                        <div className="flex items-center gap-2">
                                            <span className={`px-2 py-0.5 text-[10px] font-bold uppercase rounded border ${inv.status === 'OPEN' ? 'bg-red-900/40 text-red-400 border-red-700/50' : inv.status === 'CLOSED' ? 'bg-gray-800 text-gray-500 border-gray-600' : 'bg-yellow-900/40 text-yellow-400 border-yellow-700/50'}`}>{inv.status}</span>
                                            <span className="text-white font-bold text-sm">Case #{inv.id} (Finding: {inv.finding?.id})</span>
                                        </div>
                                        <p className="text-gray-400 text-xs">Notes: {inv.investigationNotes}</p>
                                        <p className="text-gray-500 text-[10px] uppercase tracking-wider">Opened: {new Date(inv.openedAt).toLocaleString()}</p>
                                    </div>
                                    <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition">
                                        {inv.status !== 'CLOSED' && (
                                            <>
                                                <button onClick={() => updateInvStatus(inv.id, 'UNDER_REVIEW')} className="px-3 py-1 bg-yellow-500/10 text-yellow-400 border border-yellow-500/30 rounded text-xs hover:bg-yellow-500 hover:text-white transition font-medium">Review</button>
                                                <button onClick={() => updateInvStatus(inv.id, 'CLOSED')} className="px-3 py-1 bg-gray-500/10 text-gray-400 border border-gray-500/30 rounded text-xs hover:bg-gray-500 hover:text-white transition font-medium">Close</button>
                                            </>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {activeTab === 'anomalies' && (
                        <div className="space-y-6">
                            {/* Stats Header */}
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-4">
                                    <p className="text-[10px] text-gray-500 uppercase tracking-widest font-bold">Total Anomalies</p>
                                    <p className="text-3xl font-black text-red-400 mt-1 drop-shadow-[0_0_8px_rgba(248,113,113,0.5)]">{anomalies.length}</p>
                                </div>
                                <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-4">
                                    <p className="text-[10px] text-gray-500 uppercase tracking-widest font-bold">Highest Z-Score</p>
                                    <p className="text-3xl font-black text-orange-400 mt-1 drop-shadow-[0_0_8px_rgba(249,115,22,0.5)]">
                                        {anomalies.length > 0 ? Math.max(...anomalies.map(a => Math.abs(a.anomalyScore || 0))).toFixed(1) : '—'}
                                    </p>
                                </div>
                                <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-4">
                                    <p className="text-[10px] text-gray-500 uppercase tracking-widest font-bold">Most Flagged Category</p>
                                    <p className="text-xl font-black text-yellow-400 mt-1">
                                        {anomalies.length > 0
                                            ? Object.entries(anomalies.reduce((acc, a) => { acc[a.category] = (acc[a.category] || 0) + 1; return acc; }, {})).sort((a, b) => b[1] - a[1])[0]?.[0] || '—'
                                            : '—'}
                                    </p>
                                </div>
                            </div>

                            {/* Anomaly Table */}
                            {anomalyLoading ? (
                                <div className="space-y-3">
                                    {[1, 2, 3].map(i => (
                                        <div key={i} className="h-16 bg-white/5 rounded-xl animate-pulse" />
                                    ))}
                                </div>
                            ) : anomalies.length === 0 ? (
                                <div className="text-center py-12 text-gray-500 flex flex-col items-center gap-2">
                                    <Zap size={32} className="opacity-30" />
                                    <p>No statistical anomalies detected.</p>
                                </div>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="min-w-full">
                                        <thead className="bg-white/5 border-b border-white/10">
                                            <tr>
                                                <th className="px-4 py-3 text-left text-[10px] font-bold text-gray-500 uppercase tracking-wider">Employee</th>
                                                <th className="px-4 py-3 text-left text-[10px] font-bold text-gray-500 uppercase tracking-wider">Category</th>
                                                <th className="px-4 py-3 text-left text-[10px] font-bold text-gray-500 uppercase tracking-wider">Amount</th>
                                                <th className="px-4 py-3 text-left text-[10px] font-bold text-gray-500 uppercase tracking-wider">Z-Score</th>
                                                <th className="px-4 py-3 text-left text-[10px] font-bold text-gray-500 uppercase tracking-wider">Severity</th>
                                                <th className="px-4 py-3 text-left text-[10px] font-bold text-gray-500 uppercase tracking-wider">Risk Level</th>
                                                <th className="px-4 py-3 text-left text-[10px] font-bold text-gray-500 uppercase tracking-wider">Status</th>
                                                <th className="px-4 py-3 text-right text-[10px] font-bold text-gray-500 uppercase tracking-wider">Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-white/5">
                                            {anomalies.map((expense, index) => {
                                                const absZ = Math.abs(expense.anomalyScore || 0);
                                                const severity = absZ > 3 ? { label: 'CRITICAL', cls: 'bg-red-500/30 text-red-300 border-red-500/50' }
                                                    : absZ > 2 ? { label: 'HIGH', cls: 'bg-orange-500/30 text-orange-300 border-orange-500/50' }
                                                        : { label: 'MEDIUM', cls: 'bg-yellow-500/30 text-yellow-300 border-yellow-500/50' };
                                                const isExpanded = expandedAnomaly === expense.id;
                                                const riskCls = expense.riskLevel === 'CRITICAL' ? 'bg-red-500/30 text-red-300 border-red-500/50'
                                                    : expense.riskLevel === 'HIGH' ? 'bg-orange-500/30 text-orange-300 border-orange-500/50'
                                                        : expense.riskLevel === 'MEDIUM' ? 'bg-yellow-500/30 text-yellow-300 border-yellow-500/50'
                                                            : 'bg-green-500/30 text-green-300 border-green-500/50';
                                                return (
                                                    <React.Fragment key={expense.id}>
                                                        <motion.tr
                                                            initial={{ opacity: 0, y: 10 }}
                                                            animate={{ opacity: 1, y: 0 }}
                                                            transition={{ delay: index * 0.05 }}
                                                            className={`cursor-pointer transition-colors ${isExpanded ? 'bg-red-500/10' : 'hover:bg-white/5'} border-l-2 ${absZ > 3 ? 'border-l-red-500' : absZ > 2 ? 'border-l-orange-500' : 'border-l-yellow-500'}`}
                                                            onClick={() => setExpandedAnomaly(isExpanded ? null : expense.id)}
                                                        >
                                                            <td className="px-4 py-3 text-sm text-white font-medium">{expense.user?.username || 'N/A'}</td>
                                                            <td className="px-4 py-3"><span className="px-2 py-1 rounded bg-white/5 border border-white/10 text-xs text-gray-300">{expense.category}</span></td>
                                                            <td className="px-4 py-3 text-sm text-white font-bold font-mono">₹{expense.amount?.toLocaleString('en-IN')}</td>
                                                            <td className="px-4 py-3 text-sm font-mono font-bold text-white">{(expense.anomalyScore || 0).toFixed(2)}</td>
                                                            <td className="px-4 py-3">
                                                                <span className={`px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider rounded-full border ${severity.cls}`}>
                                                                    {severity.label}
                                                                </span>
                                                            </td>
                                                            <td className="px-4 py-3">
                                                                <span className={`px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider rounded-full border ${riskCls}`}>
                                                                    {expense.riskLevel || 'LOW'}
                                                                </span>
                                                            </td>
                                                            <td className="px-4 py-3">
                                                                <span className={`px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider rounded-full border ${expense.status === 'FLAGGED' ? 'bg-yellow-500/20 text-yellow-400 border-yellow-500/50'
                                                                    : expense.status === 'SUBMITTED' ? 'bg-blue-500/20 text-blue-400 border-blue-500/50'
                                                                        : 'bg-gray-500/20 text-gray-400 border-gray-500/50'
                                                                    }`}>
                                                                    {expense.status}
                                                                </span>
                                                            </td>
                                                            <td className="px-4 py-3 text-right">
                                                                <button
                                                                    onClick={(e) => { e.stopPropagation(); setExpandedAnomaly(isExpanded ? null : expense.id); }}
                                                                    className="px-3 py-1 rounded-full text-xs bg-white/10 border border-white/20 hover:bg-white/20 text-white transition-all inline-flex items-center gap-1"
                                                                >
                                                                    <HelpCircle size={12} /> {isExpanded ? 'Close' : 'Why?'}
                                                                </button>
                                                            </td>
                                                        </motion.tr>
                                                        <AnimatePresence>
                                                            {isExpanded && (
                                                                <motion.tr
                                                                    initial={{ opacity: 0, height: 0 }}
                                                                    animate={{ opacity: 1, height: 'auto' }}
                                                                    exit={{ opacity: 0, height: 0 }}
                                                                >
                                                                    <td colSpan={8} className="px-4 py-4 bg-red-500/5 border-l-2 border-l-red-500">
                                                                        <ExplainPanel expense={expense} />
                                                                        <div className="flex gap-2 pt-3 mt-3 border-t border-white/10">
                                                                            <button
                                                                                onClick={(e) => { e.stopPropagation(); openModal(expense, 'INVESTIGATE'); }}
                                                                                className="px-4 py-2 rounded-lg bg-orange-500/10 text-orange-400 hover:bg-orange-500 hover:text-white border border-orange-500/30 transition-all flex items-center gap-2 text-xs font-bold uppercase tracking-wider"
                                                                            >
                                                                                <AlertTriangle size={14} /> Investigate
                                                                            </button>
                                                                            <button
                                                                                onClick={(e) => { e.stopPropagation(); openModal(expense, 'REVIEWED'); }}
                                                                                className="px-4 py-2 rounded-lg bg-blue-500/10 text-blue-400 hover:bg-blue-500 hover:text-white border border-blue-500/30 transition-all flex items-center gap-2 text-xs font-bold uppercase tracking-wider"
                                                                            >
                                                                                <CheckCircle size={14} /> Mark Reviewed
                                                                            </button>
                                                                        </div>
                                                                    </td>
                                                                </motion.tr>
                                                            )}
                                                        </AnimatePresence>
                                                    </React.Fragment>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}

                    {activeTab === 'analytics' && (
                        <div className="space-y-6">
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                <div className="glass-card p-6 border-l-2 border-red-500/50">
                                    <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4 flex items-center gap-2"><Activity size={16} /> Investigation Lifecycle</h3>
                                    <div className="flex flex-col gap-4">
                                        <div className="flex justify-between items-center border-b border-white/10 pb-2">
                                            <span className="text-gray-400 text-sm">Avg Resolution Time</span>
                                            <span className="text-white font-bold">{riskProfile?.averageResolutionTimeHours?.toFixed(1) || 0} hrs</span>
                                        </div>
                                        <div className="flex justify-between items-center border-b border-white/10 pb-2">
                                            <span className="text-gray-400 text-sm">Escalation Rate</span>
                                            <span className="text-yellow-400 font-bold">{((riskProfile?.averageEscalationRate || 0) * 100).toFixed(1)}%</span>
                                        </div>
                                        <div className="flex justify-between items-center">
                                            <span className="text-gray-400 text-sm">Enterprise Risk Index</span>
                                            <span className="text-red-400 font-bold text-lg">{riskProfile?.globalRiskIndex?.toFixed(1) || 0}</span>
                                        </div>
                                    </div>
                                </div>

                                <div className="glass-card p-6 col-span-1 md:col-span-2 border-t-2 border-orange-500/50">
                                    <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4 flex items-center gap-2"><Search size={16} /> Override Pattern Analytics</h3>
                                    <div className="overflow-x-auto">
                                        <table className="w-full text-left">
                                            <thead>
                                                <tr className="border-b border-white/10 text-[10px] text-gray-500 uppercase tracking-wider">
                                                    <th className="pb-2 font-bold">Manager</th>
                                                    <th className="pb-2 font-bold text-right">Overrides</th>
                                                    <th className="pb-2 font-bold text-right">Breach %</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {patterns?.managers?.length === 0 ? <tr><td colSpan={3} className="py-4 text-center text-gray-500 text-xs">No pattern data</td></tr> : null}
                                                {patterns?.managers?.slice(0, 5).map((m, i) => (
                                                    <tr key={i} className="border-b border-white/5 hover:bg-white/5 transition">
                                                        <td className="py-2 text-sm text-white font-bold">{m.managerUsername}</td>
                                                        <td className="py-2 text-sm text-yellow-500 font-mono text-right">{m.totalOverrides}</td>
                                                        <td className="py-2 text-sm text-red-400 font-mono text-right">{((m.slaBreachRate || 0) * 100).toFixed(1)}%</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>

                            <div className="glass-card p-6 border-b-2 border-neon-purple/50">
                                <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4 flex items-center gap-2"><TrendingUp size={16} /> Department Compliance Scores</h3>
                                <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                                    {patterns?.departments?.map((d, i) => (
                                        <div key={i} className="p-3 bg-white/5 border border-white/10 rounded-xl hover:bg-white/10 transition-colors">
                                            <p className="text-[10px] text-gray-400 font-bold uppercase tracking-wider truncate mb-1">{d.departmentName}</p>
                                            <div className="flex justify-between items-end">
                                                <span className={`text-2xl font-black ${d.averageComplianceScore >= 90 ? 'text-green-400 drop-shadow-[0_0_8px_rgba(74,222,128,0.5)]' : d.averageComplianceScore >= 75 ? 'text-yellow-400 drop-shadow-[0_0_8px_rgba(250,204,21,0.5)]' : 'text-red-400 drop-shadow-[0_0_8px_rgba(248,113,113,0.5)]'}`}>
                                                    {d.averageComplianceScore?.toFixed(1)}
                                                </span>
                                            </div>
                                        </div>
                                    ))}
                                    {(!patterns?.departments || patterns.departments.length === 0) && (
                                        <p className="text-gray-500 text-xs col-span-5 text-center py-8 border border-dashed border-white/10 rounded-xl">No department analytics available.</p>
                                    )}
                                </div>
                            </div>
                        </div>
                    )}
                </motion.div>
            </AnimatePresence>

            <AnimatePresence>
                {selectedExpense && (
                    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
                        <motion.div
                            initial={{ scale: 0.95, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            exit={{ scale: 0.95, opacity: 0 }}
                            className="glass-card w-full max-w-md p-6 border-white/20 shadow-neon"
                        >
                            <h3 className="text-xl font-bold mb-4 text-white flex items-center gap-2">
                                {actionType === 'REVIEWED' ? <CheckCircle className="text-blue-400" /> : <ShieldAlert className="text-red-500" />}
                                {actionType === 'REVIEWED' ? 'Log Review Activity' : 'Flag as Suspicious'}
                            </h3>

                            <p className="text-gray-300 mb-6 text-sm leading-relaxed">
                                {actionType === 'REVIEWED'
                                    ? "Add a formal note that this item was reviewed by an Auditor. This does NOT alter the submission state."
                                    : actionType === 'INVESTIGATE'
                                        ? "Open a formal incident investigation into this finding. This enters the record into the Phase 8 tracker."
                                        : "Add a compliance annotation marking this record as suspicious. This action creates a permanent finding."}
                            </p>

                            <textarea
                                className="w-full glass-input neon-focus rounded-xl h-24 mb-6 bg-dark-base/50 resize-none"
                                placeholder="Audit findings / Reason..."
                                value={reason}
                                onChange={(e) => setReason(e.target.value)}
                            />

                            <div className="flex justify-end gap-3">
                                <button onClick={closeModal} className="px-4 py-2 text-gray-400 hover:text-white transition-colors text-sm font-medium">Cancel</button>
                                <button
                                    onClick={handleAction}
                                    disabled={processing}
                                    className={`
                                        px-6 py-2 rounded-xl text-white font-bold shadow-lg transition-all
                                        ${actionType === 'CLEAR' || actionType === 'REVIEWED' ? 'bg-green-500 hover:bg-green-600 shadow-green-500/20' : actionType === 'INVESTIGATE' ? 'bg-orange-500 hover:bg-orange-600 shadow-orange-500/20' : 'bg-red-500 hover:bg-red-600 shadow-red-500/20'}
                                    `}
                                >
                                    {processing ? 'Processing...' : 'Confirm Action'}
                                </button>
                            </div>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>

            {/* Toast Notification */}
            <AnimatePresence>
                {toast && (
                    <motion.div
                        initial={{ opacity: 0, y: 40, x: 20 }}
                        animate={{ opacity: 1, y: 0, x: 0 }}
                        exit={{ opacity: 0, y: 40 }}
                        className="fixed bottom-6 right-6 z-[100] px-5 py-3 rounded-xl bg-white/10 backdrop-blur-2xl border border-white/20 shadow-2xl text-white text-sm font-medium flex items-center gap-3"
                    >
                        <CheckCircle size={18} className="text-green-400 shrink-0" />
                        {toast}
                    </motion.div>
                )}
            </AnimatePresence>
            {/* Expense Timeline Overlay */}
            <AnimatePresence>
                {historyExpenseId && (
                    <ExpenseTimeline
                        expenseId={historyExpenseId}
                        onClose={() => setHistoryExpenseId(null)}
                    />
                )}
            </AnimatePresence>
        </div>
    );
};

export default AuditorCenter;
