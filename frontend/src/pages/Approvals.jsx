import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle, XCircle, AlertCircle, ShieldAlert } from 'lucide-react';

const Approvals = () => {
    const [expenses, setExpenses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const navigate = useNavigate();
    const [comment, setComment] = useState('');
    const [selectedExpense, setSelectedExpense] = useState(null);
    const [actionType, setActionType] = useState(''); // 'APPROVE' or 'REJECT'

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            try {
                const response = await api.get(`/approvals/pending?page=${page}&size=10`);
                setExpenses(response.data.content || []);
                setTotalPages(response.data.totalPages || 0);
            } catch (error) {
                console.error('Error fetching pending approvals:', error);
                setExpenses([]);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [page]);

    const handleAction = async () => {
        if (!selectedExpense || !actionType) return;

        setActionLoading(true);
        try {
            await api.post(`/approvals/${selectedExpense.id}?action=${actionType}&comments=${encodeURIComponent(comment)}`);

            setExpenses(expenses.filter(e => e.id !== selectedExpense.id));
            alert(`Expense ${actionType}D successfully`);
            setSelectedExpense(null);
            setComment('');
        } catch (error) {
            alert('Failed to process approval: ' + (error.response?.data?.message || 'Unknown error'));
        } finally {
            setActionLoading(false);
        }
    };

    const openModal = (expense, type) => {
        setSelectedExpense(expense);
        setActionType(type);
        setComment('');
    };

    return (
        <div className="space-y-6">
            <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
                <h1 className="text-2xl font-bold text-white">Pending Approvals</h1>
                <p className="text-gray-400 text-sm">Validate and authorize expense requests.</p>
            </motion.div>

            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="grid gap-6"
            >
                {loading ? <p className="text-gray-400 text-center py-8">Loading...</p> : expenses.length === 0 ? (
                    <div className="glass-card p-12 text-center text-gray-500">No pending approvals found.</div>
                ) : (
                    expenses.map((expense, index) => (
                        <motion.div
                            initial={{ opacity: 0, scale: 0.98 }}
                            animate={{ opacity: 1, scale: 1 }}
                            transition={{ delay: index * 0.1 }}
                            key={expense.id}
                            className="glass-card p-6 flex flex-col md:flex-row justify-between items-start gap-6 group hover:border-white/20 transition-all"
                        >
                            <div className="flex-1 space-y-2">
                                <div className="flex items-center gap-3">
                                    <h3 className="text-lg font-bold text-white">{expense.title}</h3>
                                    <span className={`px-2 py-0.5 rounded text-xs border uppercase tracking-wider ${expense.status === 'FLAGGED' ? 'bg-yellow-500/10 text-yellow-400 border-yellow-500/30' : 'bg-blue-500/10 text-blue-400 border-blue-500/30'}`}>
                                        {expense.status}
                                    </span>
                                </div>
                                <div className="text-sm text-gray-400 grid grid-cols-2 gap-x-8 gap-y-1 max-w-md">
                                    <p>User: <span className="text-white">{expense.user?.username}</span></p>
                                    <p>Date: <span className="text-white">{expense.expenseDate}</span></p>
                                    <p>Category: <span className="text-white">{expense.category}</span></p>
                                </div>

                                <div className="mt-2 text-2xl font-bold text-white tracking-tight">
                                    <span className="text-lg text-gray-500 font-normal mr-1">₹</span>
                                    {expense.amount}
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
                                                <AlertCircle size={12} /> OCR Mismatch
                                            </div>
                                        )}
                                    </div>
                                )}

                                {expense.violationDetails && (
                                    <div className="flex items-center gap-2 text-red-400 text-xs bg-red-500/10 p-2 rounded border border-red-500/20 max-w-fit mt-2">
                                        <AlertCircle size={14} className="shrink-0" />
                                        <span>Violations: {expense.violationDetails}</span>
                                    </div>
                                )}
                            </div>

                            <div className="flex flex-row md:flex-col gap-3 w-full md:w-auto">
                                <button
                                    onClick={() => openModal(expense, 'APPROVE')}
                                    className="flex-1 md:w-32 py-2 px-4 rounded-xl bg-green-500/10 text-green-400 hover:bg-green-500 hover:text-white border border-green-500/30 transition-all flex items-center justify-center gap-2 font-medium"
                                >
                                    <CheckCircle size={18} /> Approve
                                </button>
                                <button
                                    onClick={() => openModal(expense, 'REJECT')}
                                    className="flex-1 md:w-32 py-2 px-4 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500 hover:text-white border border-red-500/30 transition-all flex items-center justify-center gap-2 font-medium"
                                >
                                    <XCircle size={18} /> Reject
                                </button>
                            </div>
                        </motion.div>
                    ))
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
            </motion.div>

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
                                {actionType === 'APPROVE' ? <CheckCircle className="text-green-400" /> : <XCircle className="text-red-400" />}
                                {actionType} Expense
                            </h3>
                            <p className="text-gray-300 mb-6">
                                Are you sure you want to {actionType.toLowerCase()} this expense for <span className="text-white font-bold">₹{selectedExpense.amount}</span>?
                            </p>

                            <textarea
                                className="w-full glass-input neon-focus rounded-xl h-24 mb-6 resize-none bg-dark-base/50"
                                placeholder="Add comments..."
                                value={comment}
                                onChange={(e) => setComment(e.target.value)}
                            />

                            <div className="flex justify-end gap-3">
                                <button onClick={() => setSelectedExpense(null)} className="px-4 py-2 text-gray-400 hover:text-white transition-colors text-sm font-medium">Cancel</button>
                                <button
                                    onClick={handleAction}
                                    disabled={actionLoading}
                                    className={`
                                        px-6 py-2 rounded-xl text-white font-bold shadow-lg transition-all
                                        ${actionType === 'APPROVE' ? 'bg-green-500 hover:bg-green-600 shadow-green-500/20' : 'bg-red-500 hover:bg-red-600 shadow-red-500/20'}
                                    `}
                                >
                                    {actionLoading ? 'Processing...' : 'Confirm'}
                                </button>
                            </div>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default Approvals;
