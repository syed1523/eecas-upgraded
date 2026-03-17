import React, { useEffect, useState } from 'react';
import { approvalApi } from '../services/api';
import { analyticsApi } from '../services/analyticsApi';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle, XCircle, AlertTriangle, ShieldAlert, ArrowRight, BarChart2, TrendingUp, AlertOctagon, Activity } from 'lucide-react';

const ManagerHub = () => {
    const [approvals, setApprovals] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedExpense, setSelectedExpense] = useState(null);
    const [actionType, setActionType] = useState(''); // APPROVE, REJECT, ESCALATE
    const [comment, setComment] = useState('');
    const [processing, setProcessing] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [activeTab, setActiveTab] = useState('approvals');
    const [overview, setOverview] = useState(null);
    const [trends, setTrends] = useState([]);
    const [alerts, setAlerts] = useState([]);

    useEffect(() => {
        analyticsApi.getManagerOverview().then(r => setOverview(r.data)).catch(console.error);
        analyticsApi.getManagerTrends().then(r => setTrends(r.data || [])).catch(console.error);
        analyticsApi.getManagerAlerts().then(r => setAlerts(r.data || [])).catch(console.error);
    }, []);

    useEffect(() => {
        const fetchApprovals = async () => {
            setLoading(true);
            try {
                const res = await approvalApi.getPending(page, 10);
                setApprovals(res.data.content || []);
                setTotalPages(res.data.totalPages || 0);
            } catch (err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchApprovals();
    }, [page]);

    const handleAction = async () => {
        if (!selectedExpense) return;
        setProcessing(true);
        try {
            await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate API call

            if (actionType === 'APPROVE') {
                await approvalApi.approve(selectedExpense.id, comment);
            } else if (actionType === 'REJECT') {
                await approvalApi.reject(selectedExpense.id, comment);
            } else if (actionType === 'ESCALATE') {
                await approvalApi.escalate(selectedExpense.id, comment);
            }

            setApprovals(approvals.filter(a => a.id !== selectedExpense.id));
            alert(`Expense ${actionType}D successfully`);
            closeModal();
        } catch (err) {
            alert('Action failed: ' + (err.response?.data?.message || err.message));
        } finally {
            setProcessing(false);
        }
    };

    const openModal = (expense, type) => {
        setSelectedExpense(expense);
        setActionType(type);
        setComment('');
    };

    const closeModal = () => {
        setSelectedExpense(null);
        setActionType('');
        setComment('');
    };

    const getStatusStyle = (status) => {
        switch (status) {
            case 'PENDING_SECOND_APPROVAL': return 'bg-purple-500/20 text-purple-400 border border-purple-500/50';
            case 'FLAGGED': return 'bg-yellow-500/20 text-yellow-400 border border-yellow-500/50';
            default: return 'bg-blue-500/20 text-blue-400 border border-blue-500/50';
        }
    };

    return (
        <div className="space-y-6">
            <AnimatePresence>
                {alerts.length > 0 && (
                    <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, height: 0 }} className="bg-red-500/20 border border-red-500/50 rounded-xl p-3 flex items-center justify-between shadow-neon">
                        <div className="flex items-center gap-3 text-red-400 font-bold text-sm uppercase tracking-wider">
                            <AlertOctagon size={18} />
                            <span>Governance Alert: {alerts.join(', ')}</span>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>

            <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className="flex justify-between items-end">
                <div>
                    <h1 className="text-2xl font-bold text-white">Manager Hub</h1>
                    <p className="text-gray-400 text-sm">Review approvals and monitor department governance.</p>
                </div>
                {overview && (
                    <div className="hidden md:flex gap-4">
                        <div className="glass-card px-4 py-2 text-right">
                            <div className="text-[10px] text-gray-400 font-bold uppercase tracking-wider mb-1">Compliance Score</div>
                            <div className={`text-xl font-bold ${overview.complianceScore >= 90 ? 'text-green-400' : 'text-yellow-400'}`}>{overview.complianceScore?.toFixed(1) || 100}</div>
                        </div>
                        <div className="glass-card px-4 py-2 text-right border-l-2 border-orange-500/50">
                            <div className="text-[10px] text-orange-400 font-bold uppercase tracking-wider mb-1">Escalation Rate</div>
                            <div className="text-xl font-bold text-white">{(overview.escalationRate * 100)?.toFixed(1) || 0}%</div>
                        </div>
                    </div>
                )}
            </motion.div>

            <div className="flex gap-4 border-b border-white/10 pb-2">
                <button
                    onClick={() => setActiveTab('approvals')}
                    className={`pb-2 px-4 transition-all uppercase tracking-widest text-sm font-bold ${activeTab === 'approvals' ? 'text-neon-blue border-b-2 border-neon-blue' : 'text-gray-500 hover:text-white'}`}
                >
                    <div className="flex items-center gap-2"><ShieldAlert size={16} /> Pending Actions</div>
                </button>
                <button
                    onClick={() => setActiveTab('analytics')}
                    className={`pb-2 px-4 transition-all uppercase tracking-widest text-sm font-bold ${activeTab === 'analytics' ? 'text-neon-purple border-b-2 border-neon-purple' : 'text-gray-500 hover:text-white'}`}
                >
                    <div className="flex items-center gap-2"><BarChart2 size={16} /> Governance Analytics</div>
                </button>
            </div>

            {activeTab === 'approvals' && (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.1 }}
                    className="glass-card p-6"
                >
                    <div className="flex items-center gap-2 mb-6">
                        <ShieldAlert size={24} className="text-neon-blue" />
                        <h2 className="text-lg font-bold text-white">Pending Approvals ({approvals.length})</h2>
                    </div>

                    {loading ? <p className="text-gray-400 text-center py-8">Loading...</p> : approvals.length === 0 ? (
                        <div className="text-center py-12 text-gray-500">All caught up! No pending approvals.</div>
                    ) : (
                        <div className="space-y-4">
                            {approvals.map((expense, index) => (
                                <motion.div
                                    initial={{ opacity: 0, x: -10 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    transition={{ delay: index * 0.1 }}
                                    key={expense.id}
                                    className="p-4 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-all group"
                                >
                                    <div className="flex flex-col md:flex-row justify-between gap-4">
                                        <div className="flex-1 space-y-2">
                                            <div className="flex items-center gap-3">
                                                <span className={`px-2 py-0.5 rounded text-xs border uppercase tracking-wider font-bold ${getStatusStyle(expense.status)}`}>
                                                    {expense.status}
                                                </span>
                                                <span className="text-lg font-bold text-white">{expense.title}</span>
                                                <span className="text-xs bg-white/10 text-gray-300 px-2 py-0.5 rounded border border-white/10 uppercase tracking-wider">{expense.category}</span>
                                            </div>
                                            <div className="flex items-center gap-4 text-sm text-gray-400">
                                                <span className="flex items-center gap-2">
                                                    <div className="w-2 h-2 rounded-full bg-neon-purple"></div>
                                                    {expense.user?.username}
                                                </span>
                                                <span>•</span>
                                                <span className="font-mono text-white tracking-wide">{expense.expenseDate}</span>
                                            </div>

                                            {/* Phase 9: Intelligent Risk Metrics */}
                                            {(expense.riskScore > 0 || expense.fraudIndicator || expense.ocrMismatch) && (
                                                <div className="flex flex-wrap items-center gap-2 mt-2">
                                                    {expense.riskScore > 0 && (
                                                        <div className={`text-xs px-2 py-1 rounded font-bold border ${expense.riskScore >= 75 ? 'bg-red-500/10 text-red-400 border-red-500/30' : expense.riskScore >= 40 ? 'bg-yellow-500/10 text-yellow-400 border-yellow-500/30' : 'bg-green-500/10 text-green-400 border-green-500/30'}`}>
                                                            Risk Score: {expense.riskScore}
                                                        </div>
                                                    )}
                                                    {expense.fraudIndicator && (
                                                        <div className="flex items-center gap-1 text-xs bg-red-500/20 text-red-500 border border-red-500/50 px-2 py-1 rounded font-bold uppercase tracking-wider animate-pulse">
                                                            <ShieldAlert size={12} /> Fraud Alert
                                                        </div>
                                                    )}
                                                    {expense.ocrMismatch && (
                                                        <div className="flex items-center gap-1 text-xs bg-orange-500/10 text-orange-400 border border-orange-500/30 px-2 py-1 rounded font-bold">
                                                            <AlertOctagon size={12} /> OCR Mismatch
                                                        </div>
                                                    )}
                                                </div>
                                            )}

                                            {expense.violationDetails && (
                                                <div className="flex items-center gap-2 text-red-400 text-xs bg-red-500/10 p-2 rounded border border-red-500/20 max-w-fit mt-2">
                                                    <AlertTriangle size={14} className="shrink-0" />
                                                    <span>Possible Violation: {expense.violationDetails}</span>
                                                </div>
                                            )}
                                        </div>

                                        <div className="flex items-center gap-6 border-l border-white/10 pl-6">
                                            <div className="text-right min-w-[100px]">
                                                <div className="text-xl font-bold text-white">₹{expense.amount}</div>
                                                <div className="text-xs text-gray-500 uppercase tracking-wider">Amount</div>
                                            </div>

                                            <div className="flex gap-2">
                                                <button
                                                    onClick={() => openModal(expense, 'APPROVE')}
                                                    className="p-2 rounded-lg bg-green-500/10 text-green-400 hover:bg-green-500 hover:text-white border border-green-500/30 transition-all"
                                                    title="Approve"
                                                >
                                                    <CheckCircle size={20} />
                                                </button>
                                                <button
                                                    onClick={() => openModal(expense, 'REJECT')}
                                                    className="p-2 rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500 hover:text-white border border-red-500/30 transition-all"
                                                    title="Reject"
                                                >
                                                    <XCircle size={20} />
                                                </button>
                                                <button
                                                    onClick={() => openModal(expense, 'ESCALATE')}
                                                    className="p-2 rounded-lg bg-yellow-500/10 text-yellow-400 hover:bg-yellow-500 hover:text-white border border-yellow-500/30 transition-all"
                                                    title="Escalate"
                                                >
                                                    <ArrowRight size={20} />
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>
                            ))}
                        </div>
                    )}
                    {totalPages > 1 && (
                        <div className="p-4 border-t border-white/10 flex justify-between items-center mt-4 glass-card">
                            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="px-4 py-2 border border-white/10 rounded disabled:opacity-50 text-white text-sm">Previous</button>
                            <span className="text-gray-400 text-sm">Page {page + 1} of {totalPages}</span>
                            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="px-4 py-2 border border-white/10 rounded disabled:opacity-50 text-white text-sm">Next</button>
                        </div>
                    )}
                </motion.div>
            )}

            {activeTab === 'analytics' && (
                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        <div className="glass-card p-6">
                            <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4 flex items-center gap-2"><TrendingUp size={16} /> Department Risk Trend</h3>
                            <div className="h-48 flex items-end gap-2 border-l border-b border-white/10 p-2">
                                {trends.length === 0 ? <p className="text-gray-500 text-xs w-full text-center pb-20">No historical data.</p> : trends.map((day, i) => (
                                    <div key={i} className="flex-1 bg-neon-purple/50 rounded-t group relative hover:bg-neon-purple transition-all" style={{ height: `${day.complianceScore}%` }}>
                                        <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-black/80 px-2 py-1 rounded text-[10px] text-white opacity-0 group-hover:opacity-100 whitespace-nowrap">
                                            {day.recordDate}: {day.complianceScore?.toFixed(1)}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        <div className="glass-card p-6">
                            <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4 flex items-center gap-2"><AlertOctagon size={16} /> Escalation Frequency</h3>
                            <div className="flex items-center justify-center h-48 relative">
                                <div className="text-center absolute">
                                    <p className="text-4xl font-black text-red-500 drop-shadow-[0_0_10px_rgba(239,68,68,0.5)]">
                                        {overview ? (overview.escalationRate * 100).toFixed(1) : 0}%
                                    </p>
                                    <p className="text-xs text-red-400 uppercase font-bold mt-2">Breach Rate</p>
                                </div>
                                <svg className="w-40 h-40 transform -rotate-90">
                                    <circle cx="80" cy="80" r="70" fill="transparent" stroke="rgba(255,255,255,0.05)" strokeWidth="12" />
                                    <circle cx="80" cy="80" r="70" fill="transparent" stroke="#ef4444" strokeWidth="12" strokeDasharray="439.8" strokeDashoffset={overview ? 439.8 - (439.8 * overview.escalationRate) : 439.8} className="transition-all duration-1000 ease-out" />
                                </svg>
                            </div>
                        </div>

                        <div className="glass-card p-6">
                            <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4 flex items-center gap-2"><Activity size={16} /> Burn Rate Indicator</h3>
                            <div className="h-48 flex flex-col justify-center space-y-4">
                                <p className="text-xs text-gray-400">Current month budget tracking</p>
                                <div className="w-full bg-white/5 rounded-full h-8 overflow-hidden relative shadow-inner border border-white/10">
                                    <div className="bg-gradient-to-r from-green-500 to-yellow-500 h-full w-[65%]" />
                                    <span className="absolute inset-0 flex items-center justify-center text-xs font-bold text-white uppercase tracking-widest drop-shadow-md">65% Utilized</span>
                                </div>
                                <div className="flex justify-between text-xs font-bold text-gray-500 uppercase tracking-wider">
                                    <span>Safe</span>
                                    <span>Warning</span>
                                    <span>Critical</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </motion.div>
            )}

            <AnimatePresence>
                {selectedExpense && (
                    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
                        <motion.div
                            initial={{ scale: 0.9, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            exit={{ scale: 0.9, opacity: 0 }}
                            className="glass-card w-full max-w-md p-6 border-white/20 shadow-neon"
                        >
                            <h3 className="text-xl font-bold mb-4 text-white flex items-center gap-2">
                                {actionType === 'APPROVE' && <CheckCircle className="text-green-400" />}
                                {actionType === 'REJECT' && <XCircle className="text-red-400" />}
                                {actionType === 'ESCALATE' && <ArrowRight className="text-yellow-400" />}
                                {actionType} EXPENSE
                            </h3>

                            <p className="text-gray-300 mb-6 text-sm leading-relaxed">
                                {actionType === 'ESCALATE'
                                    ? "Escalating requires a valid reason for the auditor."
                                    : `Are you sure you want to ${actionType.toLowerCase()} this claim for `}
                                <span className="text-white font-bold">₹{selectedExpense.amount}</span>?
                            </p>

                            <textarea
                                className="w-full glass-input neon-focus rounded-xl h-24 mb-6 resize-none bg-dark-base/50"
                                placeholder={actionType === 'ESCALATE' ? "Reason for escalation..." : "Optional comments..."}
                                value={comment}
                                onChange={(e) => setComment(e.target.value)}
                            />

                            <div className="flex justify-end gap-3">
                                <button onClick={closeModal} className="px-4 py-2 text-gray-400 hover:text-white transition-colors text-sm font-medium">Cancel</button>
                                <button
                                    onClick={handleAction}
                                    disabled={processing}
                                    className={`
                                        px-6 py-2 rounded-xl text-white font-bold shadow-lg transition-all
                                        ${actionType === 'APPROVE' ? 'bg-green-500 hover:bg-green-600 shadow-green-500/20' :
                                            actionType === 'REJECT' ? 'bg-red-500 hover:bg-red-600 shadow-red-500/20' :
                                                'bg-yellow-500 hover:bg-yellow-600 shadow-yellow-500/20 text-black'}
                                    `}
                                >
                                    {processing ? 'Processing...' : 'Confirm Action'}
                                </button>
                            </div>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default ManagerHub;
