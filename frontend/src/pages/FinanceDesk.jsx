import React, { useEffect, useState } from 'react';
import { approvalApi, expenseApi } from '../services/api';
import { motion, AnimatePresence } from 'framer-motion';
import { IndianRupee, CheckCircle2, XCircle, CreditCard, FileCheck, Layers } from 'lucide-react';

const FinanceDesk = () => {
    const [activeTab, setActiveTab] = useState('approvals'); // approvals | payments
    const [approvals, setApprovals] = useState([]);
    const [paymentQueue, setPaymentQueue] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedExpense, setSelectedExpense] = useState(null);
    const [actionType, setActionType] = useState(''); // APPROVE, REJECT, PAY
    const [comment, setComment] = useState('');
    const [processing, setProcessing] = useState(false);
    const [approvalPage, setApprovalPage] = useState(0);
    const [approvalTotalPages, setApprovalTotalPages] = useState(0);
    const [paymentPage, setPaymentPage] = useState(0);
    const [paymentTotalPages, setPaymentTotalPages] = useState(0);

    useEffect(() => {
        if (activeTab === 'approvals') fetchApprovals();
        else fetchPaymentQueue();
    }, [activeTab]);

    const fetchApprovals = async () => {
        setLoading(true);
        try {
            const res = await approvalApi.getPending(approvalPage, 10);
            setApprovals(res.data.content || []);
            setApprovalTotalPages(res.data.totalPages || 0);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const fetchPaymentQueue = async () => {
        setLoading(true);
        try {
            const res = await expenseApi.getAllRequest(paymentPage, 10);
            const content = res.data.content || [];
            const payable = content.filter(e =>
                (e.status === 'APPROVED' || e.status === 'CLEARED' || e.status === 'APPROVED_PENDING_PAYMENT')
            );
            setPaymentQueue(payable);
            setPaymentTotalPages(res.data.totalPages || 0);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleAction = async () => {
        if (!selectedExpense) return;
        setProcessing(true);
        try {
            await new Promise(resolve => setTimeout(resolve, 1000));

            if (actionType === 'PAY') {
                await approvalApi.pay(selectedExpense.id);
                setPaymentQueue(paymentQueue.filter(e => e.id !== selectedExpense.id));
            } else if (actionType === 'APPROVE') {
                await approvalApi.approve(selectedExpense.id, comment);
                setApprovals(approvals.filter(e => e.id !== selectedExpense.id));
            } else if (actionType === 'REJECT') {
                await approvalApi.reject(selectedExpense.id, comment);
                setApprovals(approvals.filter(e => e.id !== selectedExpense.id));
            }
            alert('Action successful');
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

    return (
        <div className="space-y-6">
            <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
                <h1 className="text-2xl font-bold text-white">Finance Desk</h1>
                <p className="text-gray-400 text-sm">Process payments and final financial approvals.</p>
            </motion.div>

            {/* Glass Tabs */}
            <div className="flex p-1 bg-white/5 rounded-xl border border-white/10 w-fit">
                <button
                    onClick={() => setActiveTab('approvals')}
                    className={`
                        px-6 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2
                        ${activeTab === 'approvals'
                            ? 'bg-primary-gradient text-white shadow-neon'
                            : 'text-gray-400 hover:text-white hover:bg-white/5'}
                    `}
                >
                    <FileCheck size={16} />
                    Pending Approvals
                </button>
                <button
                    onClick={() => setActiveTab('payments')}
                    className={`
                        px-6 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2
                        ${activeTab === 'payments'
                            ? 'bg-primary-gradient text-white shadow-neon'
                            : 'text-gray-400 hover:text-white hover:bg-white/5'}
                    `}
                >
                    <CreditCard size={16} />
                    Payment Queue
                </button>
            </div>

            <AnimatePresence mode="wait">
                <motion.div
                    key={activeTab}
                    initial={{ opacity: 0, x: 10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -10 }}
                    transition={{ duration: 0.2 }}
                    className="glass-card p-6 min-h-[400px]"
                >
                    {loading ? <p className="text-gray-400 text-center py-12">Loading financial data...</p> : (
                        activeTab === 'approvals' ? (
                            approvals.length === 0 ? <p className="text-gray-500 text-center py-12">No pending approvals.</p> : (
                                <div>
                                    <div className="space-y-4">
                                        {approvals.map((expense, index) => (
                                            <motion.div
                                                initial={{ opacity: 0, y: 10 }}
                                                animate={{ opacity: 1, y: 0 }}
                                                transition={{ delay: index * 0.05 }}
                                                key={expense.id}
                                                className="border border-white/10 bg-white/5 rounded-xl p-4 flex flex-col md:flex-row justify-between items-center gap-4 hover:bg-white/10 transition group"
                                            >
                                                <div className="w-full md:w-auto">
                                                    <h3 className="font-bold text-lg text-white">{expense.title}</h3>
                                                    <p className="text-sm text-gray-400">User: <span className="text-white">{expense.user?.username}</span></p>
                                                    <p className="text-xs text-gray-500 mt-1 font-mono">{expense.expenseDate}</p>
                                                </div>
                                                <div className="flex items-center justify-between w-full md:w-auto gap-6 border-t md:border-t-0 border-white/10 pt-4 md:pt-0">
                                                    <div className="text-right">
                                                        <div className="text-xl font-bold text-white">₹{expense.amount}</div>
                                                        <div className="text-xs text-gray-500 uppercase tracking-wider">Amount</div>
                                                    </div>
                                                    <div className="flex gap-2">
                                                        <button onClick={() => openModal(expense, 'APPROVE')} className="p-2 rounded-lg bg-green-500/10 text-green-400 hover:bg-green-500 hover:text-white border border-green-500/30 transition">
                                                            <CheckCircle2 size={20} />
                                                        </button>
                                                        <button onClick={() => openModal(expense, 'REJECT')} className="p-2 rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500 hover:text-white border border-red-500/30 transition">
                                                            <XCircle size={20} />
                                                        </button>
                                                    </div>
                                                </div>
                                            </motion.div>
                                        ))}
                                    </div>
                                    {approvalTotalPages > 1 && (
                                        <div className="p-4 border-t border-white/10 flex justify-between items-center mt-4">
                                            <button onClick={() => setApprovalPage(p => Math.max(0, p - 1))} disabled={approvalPage === 0} className="px-4 py-2 border border-white/10 rounded disabled:opacity-50 text-white text-sm">Previous</button>
                                            <span className="text-gray-400 text-sm">Page {approvalPage + 1} of {approvalTotalPages}</span>
                                            <button onClick={() => setApprovalPage(p => Math.min(approvalTotalPages - 1, p + 1))} disabled={approvalPage >= approvalTotalPages - 1} className="px-4 py-2 border border-white/10 rounded disabled:opacity-50 text-white text-sm">Next</button>
                                        </div>
                                    )}
                                </div>
                            )
                        ) : (
                            paymentQueue.length === 0 ? <p className="text-gray-500 text-center py-12">No pending payments.</p> : (
                                <div>
                                    <div className="space-y-4">
                                        {paymentQueue.map((expense, index) => (
                                            <motion.div
                                                initial={{ opacity: 0, y: 10 }}
                                                animate={{ opacity: 1, y: 0 }}
                                                transition={{ delay: index * 0.05 }}
                                                key={expense.id}
                                                className="border border-blue-500/30 bg-blue-500/5 p-4 rounded-xl flex flex-col md:flex-row justify-between items-center gap-4 hover:bg-blue-500/10 transition"
                                            >
                                                <div className="w-full md:w-auto">
                                                    <div className="flex items-center gap-3">
                                                        <h3 className="font-bold text-lg text-white">{expense.title}</h3>
                                                        <span className={`text-xs px-2 py-0.5 rounded border uppercase tracking-wider ${expense.status === 'CLEARED' ? 'bg-green-500/20 text-green-400 border-green-500/40' : 'bg-blue-500/20 text-blue-400 border-blue-500/40'}`}>
                                                            {expense.status}
                                                        </span>
                                                    </div>
                                                    <p className="text-sm text-gray-400 mt-1">Payable to: <span className="font-medium text-white">{expense.user?.username}</span></p>
                                                </div>
                                                <div className="flex items-center justify-between w-full md:w-auto gap-6 border-t md:border-t-0 border-white/10 pt-4 md:pt-0">
                                                    <div className="text-right">
                                                        <div className="text-xl font-bold text-neon-blue">₹{expense.amount}</div>
                                                        <div className="text-xs text-gray-500 uppercase tracking-wider">Payout</div>
                                                    </div>
                                                    <button
                                                        onClick={() => openModal(expense, 'PAY')}
                                                        className="bg-primary-gradient text-white px-6 py-2 rounded-lg font-bold shadow-neon hover:scale-105 transition-transform flex items-center gap-2"
                                                    >
                                                        <IndianRupee size={18} />
                                                        Process
                                                    </button>
                                                </div>
                                            </motion.div>
                                        ))}
                                    </div>
                                    {paymentTotalPages > 1 && (
                                        <div className="p-4 border-t border-white/10 flex justify-between items-center mt-4">
                                            <button onClick={() => setPaymentPage(p => Math.max(0, p - 1))} disabled={paymentPage === 0} className="px-4 py-2 border border-white/10 rounded disabled:opacity-50 text-white text-sm">Previous</button>
                                            <span className="text-gray-400 text-sm">Page {paymentPage + 1} of {paymentTotalPages}</span>
                                            <button onClick={() => setPaymentPage(p => Math.min(paymentTotalPages - 1, p + 1))} disabled={paymentPage >= paymentTotalPages - 1} className="px-4 py-2 border border-white/10 rounded disabled:opacity-50 text-white text-sm">Next</button>
                                        </div>
                                    )}
                                </div>
                            )
                        )
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
                            <h3 className="text-xl font-bold mb-4 text-white">
                                {actionType === 'PAY' ? 'Process Payment' : `${actionType} Request`}
                            </h3>

                            <p className="text-gray-300 mb-6 text-sm leading-relaxed">
                                {actionType === 'PAY'
                                    ? `Confirm payment of `
                                    : `Are you sure you want to ${actionType.toLowerCase()} this request for `}
                                <span className="text-white font-bold">₹{selectedExpense.amount}</span>
                                {actionType === 'PAY' && ` to ${selectedExpense.user?.username}`}?
                            </p>

                            {actionType !== 'PAY' && (
                                <textarea
                                    className="w-full glass-input neon-focus rounded-xl h-24 mb-6 bg-dark-base/50 resize-none"
                                    placeholder="Add comments..."
                                    value={comment}
                                    onChange={(e) => setComment(e.target.value)}
                                />
                            )}

                            <div className="flex justify-end gap-3">
                                <button onClick={closeModal} className="px-4 py-2 text-gray-400 hover:text-white transition-colors text-sm font-medium">Cancel</button>
                                <button
                                    onClick={handleAction}
                                    disabled={processing}
                                    className={`
                                        px-6 py-2 rounded-xl text-white font-bold shadow-lg transition-all
                                        ${actionType === 'PAY' ? 'bg-primary-gradient shadow-neon' :
                                            actionType === 'APPROVE' ? 'bg-green-500 hover:bg-green-600 shadow-green-500/20' :
                                                'bg-red-500 hover:bg-red-600 shadow-red-500/20'}
                                    `}
                                >
                                    {processing ? 'Processing...' : 'Confirm Transaction'}
                                </button>
                            </div>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default FinanceDesk;
