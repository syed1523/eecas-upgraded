import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { analyticsApi } from '../services/analyticsApi';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus, Search, Filter, ShieldAlert, Activity, Clock, X } from 'lucide-react';

const MyExpenses = () => {
    const [expenses, setExpenses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [acknowledging, setAcknowledging] = useState(null);
    const [alerts, setAlerts] = useState([]);
    const [complianceScore, setComplianceScore] = useState(null);
    const [timelineExpense, setTimelineExpense] = useState(null);
    const [timelineEvents, setTimelineEvents] = useState([]);
    const [timelineLoading, setTimelineLoading] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        // Fetch Intelligence Analytics
        analyticsApi.getEmployeeAlerts().then(r => setAlerts(r.data || [])).catch(console.error);
        analyticsApi.getEmployeeScore().then(r => setComplianceScore(r.data)).catch(console.error);
    }, []);

    useEffect(() => {
        const fetchExpenses = async () => {
            setLoading(true);
            try {
                const response = await api.get(`/expenses/my?page=${page}&size=10`);
                setExpenses(response.data.content || []);
                setTotalPages(response.data.totalPages || 0);
            } catch (error) {
                console.error('Error fetching expenses:', error);
            } finally {
                setLoading(false);
            }
        };
        fetchExpenses();
    }, [page]);

    const handleAcknowledge = async (id) => {
        setAcknowledging(id);
        try {
            await api.post(`/expenses/${id}/acknowledge`, {});
            alert('Expense acknowledged and returned to DRAFT state.');
            setExpenses(expenses.map(e => e.id === id ? { ...e, status: 'DRAFT', violationDetails: null, flagged: false } : e));
            // Refresh alerts
            analyticsApi.getEmployeeAlerts().then(r => setAlerts(r.data || [])).catch(console.error);
        } catch (error) {
            console.error(error);
            alert('Failed to acknowledge expense: ' + (error.response?.data?.message || error.message));
        } finally {
            setAcknowledging(null);
        }
    };

    const openTimeline = async (expense) => {
        setTimelineExpense(expense);
        setTimelineLoading(true);
        try {
            const res = await analyticsApi.getExpenseTimeline(expense.id);
            setTimelineEvents(res.data || []);
        } catch (err) {
            console.error(err);
        } finally {
            setTimelineLoading(false);
        }
    };

    const getStatusStyle = (status) => {
        switch (status) {
            case 'APPROVED': return 'bg-green-500/20 text-green-400 border border-green-500/50';
            case 'REJECTED': return 'bg-red-500/20 text-red-400 border border-red-500/50';
            case 'FLAGGED': return 'bg-yellow-500/20 text-yellow-400 border border-yellow-500/50';
            case 'SUBMITTED': return 'bg-blue-500/20 text-blue-400 border border-blue-500/50';
            case 'REQUIRES_ACKNOWLEDGMENT': return 'bg-orange-500/20 text-orange-400 border border-orange-500/50';
            case 'PENDING_SECOND_APPROVAL': return 'bg-purple-500/20 text-purple-400 border border-purple-500/50';
            default: return 'bg-gray-500/20 text-gray-400 border border-gray-500/50';
        }
    };

    return (
        <div className="space-y-6">
            <AnimatePresence>
                {alerts.length > 0 && (
                    <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, height: 0 }} className="bg-orange-500/20 border border-orange-500/50 rounded-xl p-3 flex items-center justify-between shadow-neon">
                        <div className="flex items-center gap-3 text-orange-400 font-bold text-sm uppercase tracking-wider">
                            <ShieldAlert size={18} />
                            <span>
                                {alerts.includes('REQUIRES_ACKNOWLEDGMENT') ? 'Action Required: You have expenses blocked pending acknowledgment.' : 'Policy Alert Active'}
                            </span>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>

            <div className="flex justify-between items-center mb-6">
                <motion.div initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }}>
                    <h1 className="text-2xl font-bold text-white">Expense History</h1>
                    <p className="text-gray-400 text-sm">Track and manage your submitted expenses</p>
                </motion.div>

                {complianceScore && (
                    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="hidden md:flex items-center gap-4 bg-white/5 border border-white/10 rounded-xl px-4 py-2">
                        <div className="text-right">
                            <p className="text-[10px] text-gray-500 uppercase tracking-widest font-bold">Dept Compliance</p>
                            <p className="text-sm font-bold text-white">{complianceScore.department}</p>
                        </div>
                        <div className={`text-2xl font-bold ${complianceScore.score >= 90 ? 'text-green-400' : complianceScore.score >= 70 ? 'text-yellow-400' : 'text-red-400'}`}>
                            {complianceScore.score.toFixed(1)}
                        </div>
                    </motion.div>
                )}

                <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => navigate('/submit-expense')}
                    className="bg-primary-gradient text-white px-6 py-3 rounded-xl shadow-neon flex items-center gap-2 font-medium"
                >
                    <Plus size={18} />
                    New Submission
                </motion.button>
            </div>

            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="glass-card overflow-hidden"
            >
                <div className="p-4 border-b border-white/10 flex gap-4 items-center">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" size={18} />
                        <input
                            type="text"
                            placeholder="Search expenses..."
                            className="w-full glass-input pl-10"
                        />
                    </div>
                </div>

                {loading ? (
                    <div className="p-12 text-center text-gray-400">Loading data stream...</div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="min-w-full">
                            <thead className="bg-white/5 border-b border-white/10">
                                <tr>
                                    <th className="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Date</th>
                                    <th className="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Title</th>
                                    <th className="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Category</th>
                                    <th className="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Amount</th>
                                    <th className="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Risk</th>
                                    <th className="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Status</th>
                                    <th className="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Issues</th>
                                    <th className="px-6 py-4 text-right text-xs font-semibold text-gray-400 uppercase tracking-wider">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-white/5">
                                {expenses.map((expense, index) => (
                                    <motion.tr
                                        initial={{ opacity: 0, y: 10 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        transition={{ delay: index * 0.05 }}
                                        key={expense.id}
                                        className="hover:bg-white/5 transition-colors group"
                                    >
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300 font-mono">{expense.expenseDate}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-white font-medium">{expense.title}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                                            <span className="px-2 py-1 rounded bg-white/5 border border-white/10 text-xs">
                                                {expense.category}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-white font-bold">
                                            ₹{expense.amount}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                                            {expense.riskScore > 0 ? (
                                                <div className="flex items-center gap-2">
                                                    <div className="w-full bg-white/10 rounded-full h-2 min-w-[50px] overflow-hidden">
                                                        <div className={`h-2 rounded-full ${expense.riskScore >= 75 ? 'bg-red-500' : expense.riskScore >= 40 ? 'bg-yellow-500' : 'bg-green-500'}`} style={{ width: `${expense.riskScore}%` }}></div>
                                                    </div>
                                                    <span className="text-xs text-gray-400 font-mono">{expense.riskScore}/100</span>
                                                </div>
                                            ) : (
                                                <span className="text-xs text-gray-500">Unscored</span>
                                            )}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <span className={`px-3 py-1 text-xs font-semibold rounded-full ${getStatusStyle(expense.status)}`}>
                                                {expense.status}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 text-sm text-red-400 max-w-xs truncate">
                                            <div className="flex flex-col gap-1">
                                                {expense.ocrMismatch && (
                                                    <div className="flex items-center gap-1 text-orange-400 text-xs font-bold uppercase tracking-wider bg-orange-500/10 px-2 py-0.5 rounded border border-orange-500/20 w-fit">
                                                        <ShieldAlert size={12} />
                                                        OCR Mismatch
                                                    </div>
                                                )}
                                                {expense.violationDetails && (
                                                    <div className="flex items-center gap-1">
                                                        <div className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse shrink-0"></div>
                                                        <span className="truncate">{expense.violationDetails}</span>
                                                    </div>
                                                )}
                                            </div>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-right">
                                            {(expense.status === 'DRAFT' || expense.status === 'REJECTED') && (
                                                <button
                                                    onClick={() => navigate('/submit-expense', { state: { expense } })}
                                                    className="text-white hover:text-neon-blue transition-colors text-xs uppercase tracking-wider font-semibold opacity-0 group-hover:opacity-100 mr-2 bg-white/10 px-2 py-1 rounded"
                                                >
                                                    Edit
                                                </button>
                                            )}
                                            <button
                                                onClick={() => openTimeline(expense)}
                                                className="text-neon-purple hover:text-white transition-colors text-xs uppercase tracking-wider font-semibold opacity-0 group-hover:opacity-100 bg-purple-500/10 border border-purple-500/30 px-2 py-1 rounded"
                                            >
                                                Timeline
                                            </button>
                                            {expense.status === 'REQUIRES_ACKNOWLEDGMENT' && (
                                                <button
                                                    onClick={() => handleAcknowledge(expense.id)}
                                                    disabled={acknowledging === expense.id}
                                                    className="ml-3 text-orange-400 bg-orange-500/10 border border-orange-500/30 px-3 py-1 rounded hover:bg-orange-500 hover:text-white transition-all text-xs uppercase tracking-wider font-bold"
                                                >
                                                    {acknowledging === expense.id ? 'Wait...' : 'Acknowledge'}
                                                </button>
                                            )}
                                        </td>
                                    </motion.tr>
                                ))}
                            </tbody>
                        </table>
                        {expenses.length === 0 && (
                            <div className="p-12 text-center text-gray-500 flex flex-col items-center">
                                <motion.div
                                    initial={{ scale: 0.8, opacity: 0 }}
                                    animate={{ scale: 1, opacity: 1 }}
                                    className="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center mb-4 border border-white/10"
                                >
                                    <Search size={24} className="opacity-50 text-white" />
                                </motion.div>
                                <p>No expense records found in current timeline.</p>
                            </div>
                        )}
                        {totalPages > 1 && (
                            <div className="p-4 border-t border-white/10 flex justify-between items-center">
                                <button
                                    onClick={() => setPage(p => Math.max(0, p - 1))}
                                    disabled={page === 0}
                                    className="px-4 py-2 border border-white/10 rounded disabled:opacity-50 text-white text-sm"
                                >
                                    Previous
                                </button>
                                <span className="text-gray-400 text-sm">
                                    Page {page + 1} of {totalPages}
                                </span>
                                <button
                                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                                    disabled={page >= totalPages - 1}
                                    className="px-4 py-2 border border-white/10 rounded disabled:opacity-50 text-white text-sm"
                                >
                                    Next
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </motion.div>

            {/* VISUAL WORKFLOW TIMELINE MODAL */}
            <AnimatePresence>
                {timelineExpense && (
                    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex justify-end z-50">
                        <motion.div
                            initial={{ x: '100%' }}
                            animate={{ x: 0 }}
                            exit={{ x: '100%' }}
                            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
                            className="bg-dark-base w-full max-w-md h-full border-l border-white/10 shadow-neon-purple overflow-y-auto"
                        >
                            <div className="p-6 border-b border-white/10 flex justify-between items-center sticky top-0 bg-dark-base/90 backdrop-blur z-10">
                                <div>
                                    <h2 className="text-xl font-bold text-white">Workflow Timeline</h2>
                                    <p className="text-xs text-gray-400 mt-1">Record ID: {timelineExpense.id}</p>
                                </div>
                                <button onClick={() => setTimelineExpense(null)} className="p-2 hover:bg-white/10 rounded-full transition">
                                    <X className="text-gray-400 hover:text-white" size={20} />
                                </button>
                            </div>

                            <div className="p-6">
                                {timelineLoading ? <p className="text-center text-gray-500 py-10">Fetching immutable audit ledger...</p> : (
                                    <div className="space-y-6 relative before:absolute before:inset-0 before:ml-5 before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-white/20 before:to-transparent">
                                        {timelineEvents.map((event, index) => (
                                            <div key={event.id} className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
                                                <div className="flex items-center justify-center w-10 h-10 rounded-full border border-white/20 bg-dark-base text-gray-400 group-[.is-active]:text-neon-purple group-[.is-active]:border-neon-purple shadow-neon shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2">
                                                    <Clock size={16} />
                                                </div>
                                                <div className="w-[calc(100%-4rem)] md:w-[calc(50%-2.5rem)] glass-card p-4 rounded-xl border-white/5 shadow">
                                                    <div className="flex items-center justify-between space-x-2 mb-1">
                                                        <div className="font-bold text-white text-sm">{event.newValue || event.action}</div>
                                                        <time className="font-mono text-[10px] text-gray-500">{new Date(event.timestamp).toLocaleString()}</time>
                                                    </div>
                                                    <div className="text-gray-400 text-xs">
                                                        Action by: <span className="text-neon-blue font-medium">{event.performedBy || 'System'}</span>
                                                    </div>
                                                    {event.action.includes('OVERRIDE') && (
                                                        <div className="mt-2 text-[10px] bg-purple-500/10 text-purple-400 border border-purple-500/20 px-2 py-1 rounded inline-block">
                                                            SECURITY OVERRIDE DETECTED
                                                        </div>
                                                    )}
                                                    {event.newValue === 'ESCALATED' && (
                                                        <div className="mt-2 text-[10px] bg-red-500/10 text-red-400 border border-red-500/20 px-2 py-1 rounded inline-block">
                                                            SLA ESCALATION MARKER
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        ))}
                                        {timelineEvents.length === 0 && <p className="text-center text-gray-500 text-sm">No timeline events found in ledger.</p>}
                                    </div>
                                )}
                            </div>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default MyExpenses;
