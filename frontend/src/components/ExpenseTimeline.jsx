import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    Clock, CheckCircle, XCircle, AlertTriangle,
    Zap, Info, User, Shield, ArrowRight, Eye, ChevronRight
} from 'lucide-react';
import { auditApi } from '../services/auditApi';

const ExpenseTimeline = ({ expenseId, onClose }) => {
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedStep, setSelectedStep] = useState(null);

    useEffect(() => {
        if (expenseId) {
            fetchHistory();
        }
    }, [expenseId]);

    const fetchHistory = async () => {
        try {
            setLoading(true);
            const response = await auditApi.getExpenseHistory(expenseId);
            setHistory(response.data);
            if (response.data.length > 0) {
                setSelectedStep(response.data[0]);
            }
        } catch (err) {
            console.error("Failed to fetch history:", err);
        } finally {
            setLoading(false);
        }
    };

    const getActionIcon = (action) => {
        const a = action.toUpperCase();
        if (a.includes('SUBMIT')) return <Zap className="w-5 h-5 text-blue-400" />;
        if (a.includes('APPROVE')) return <CheckCircle className="w-5 h-5 text-emerald-400" />;
        if (a.includes('REJECT')) return <XCircle className="w-5 h-5 text-rose-400" />;
        if (a.includes('ANOMALY') || a.includes('VIOLATION')) return <AlertTriangle className="w-5 h-5 text-amber-400" />;
        if (a.includes('INVESTIGATION')) return <Shield className="w-5 h-5 text-purple-400" />;
        return <Clock className="w-5 h-5 text-slate-400" />;
    };

    const parseState = (state) => {
        if (!state) return null;
        let parsedState;
        try {
            parsedState = JSON.parse(state);
        } catch (e) {
            parsedState = state;
        }
        return parsedState;
    };

    let parsedBeforeState;
    if (selectedStep?.beforeState) {
        try {
            parsedBeforeState = JSON.parse(selectedStep.beforeState);
        } catch (e) {
            parsedBeforeState = selectedStep.beforeState;
        }
    }

    let parsedAfterState;
    if (selectedStep?.afterState) {
        try {
            parsedAfterState = JSON.parse(selectedStep.afterState);
        } catch (e) {
            parsedAfterState = selectedStep.afterState;
        }
    }

    return (
        <motion.div
            initial={{ opacity: 0, x: 400 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 400 }}
            className="fixed inset-y-0 right-0 w-[450px] bg-slate-900/95 backdrop-blur-xl border-l border-white/10 shadow-2xl z-50 flex flex-col"
        >
            <div className="p-6 border-b border-white/10 flex justify-between items-center">
                <div>
                    <h2 className="text-xl font-bold text-white flex items-center gap-2">
                        <Clock className="w-5 h-5 text-blue-400" />
                        Expense Life History
                    </h2>
                    <p className="text-sm text-slate-400">Audit Trail for Transaction #{expenseId}</p>
                </div>
                <button
                    onClick={onClose}
                    className="p-2 hover:bg-white/10 rounded-full transition-colors"
                >
                    <XCircle className="w-6 h-6 text-slate-400" />
                </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6 scrollbar-hide">
                {loading ? (
                    <div className="flex flex-col items-center justify-center h-full gap-4">
                        <motion.div
                            animate={{ rotate: 360 }}
                            transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                            className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full"
                        />
                        <p className="text-slate-400">Loading audit records...</p>
                    </div>
                ) : history.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-full text-center p-12">
                        <Info className="w-12 h-12 text-slate-700 mb-4" />
                        <h3 className="text-slate-300 font-medium">No history found</h3>
                        <p className="text-slate-500 text-sm mt-2">This expense hasn't generated any audit logs yet.</p>
                    </div>
                ) : (
                    <div className="space-y-8 relative">
                        {/* Timeline Line */}
                        <div className="absolute left-[19px] top-2 bottom-2 w-0.5 bg-gradient-to-b from-blue-500/50 via-purple-500/50 to-emerald-500/50" />

                        <AnimatePresence>
                            {history.map((step, idx) => (
                                <motion.div
                                    key={step.id}
                                    initial={{ opacity: 0, y: 20 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    transition={{ delay: idx * 0.1 }}
                                    onClick={() => setSelectedStep(step)}
                                    className={`relative pl-12 cursor-pointer group`}
                                >
                                    {/* Timeline Node */}
                                    <div className={`absolute left-0 w-10 h-10 rounded-full flex items-center justify-center transition-all z-10 ${selectedStep?.id === step.id
                                            ? 'bg-blue-600 scale-110 shadow-[0_0_20px_rgba(37,99,235,0.5)]'
                                            : 'bg-slate-800 border border-white/10 group-hover:border-blue-500/50'
                                        }`}>
                                        {getActionIcon(step.action)}
                                    </div>

                                    <div className={`p-4 rounded-2xl border transition-all ${selectedStep?.id === step.id
                                            ? 'bg-white/5 border-blue-500/30'
                                            : 'bg-transparent border-transparent group-hover:bg-white/5'
                                        }`}>
                                        <div className="flex justify-between items-start mb-1">
                                            <span className="text-xs font-bold uppercase tracking-wider text-blue-400">
                                                {step.action.replace(/_/g, ' ')}
                                            </span>
                                            <span className="text-[10px] text-slate-500 font-mono">
                                                {new Date(step.timestamp).toLocaleString()}
                                            </span>
                                        </div>
                                        <h3 className="text-sm font-medium text-slate-200">{step.changeSummary}</h3>
                                        <div className="mt-2 flex items-center gap-2">
                                            <User className="w-3 h-3 text-slate-500" />
                                            <span className="text-xs text-slate-400">{step.performedBy}</span>
                                            <span className="text-[10px] bg-slate-800 text-slate-500 px-1.5 py-0.5 rounded">
                                                {step.performedByRole}
                                            </span>
                                            {step.wasSystemTriggered && (
                                                <span className="text-[10px] bg-purple-500/20 text-purple-400 px-1.5 py-0.5 rounded flex items-center gap-1">
                                                    <Zap className="w-2 h-2" /> SYSTEM
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                </motion.div>
                            ))}
                        </AnimatePresence>
                    </div>
                )}
            </div>

            {/* Change Detail Viewer */}
            <AnimatePresence>
                {selectedStep && !loading && (
                    <motion.div
                        initial={{ opacity: 0, y: 100 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="bg-slate-950 border-t border-white/10 p-6 h-1/3 overflow-y-auto"
                    >
                        <h4 className="text-xs font-bold text-slate-500 uppercase flex items-center gap-2 mb-4 tracking-widest">
                            <Eye className="w-3 h-3" /> State Comparison
                        </h4>

                        {!selectedStep.beforeState && !selectedStep.afterState ? (
                            <div className="text-sm text-slate-500 italic">No detailed state changes captured for this event.</div>
                        ) : (
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <div className="text-[10px] text-slate-600 uppercase font-bold">Before</div>
                                    <div className="p-3 bg-red-500/5 rounded-lg border border-red-500/10 text-[11px] font-mono text-slate-400 whitespace-pre">
                                        {selectedStep.beforeState ?
                                            JSON.stringify(parsedBeforeState, null, 2)
                                            : "NULL"}
                                    </div>
                                </div>
                                <div className="space-y-2">
                                    <div className="text-[10px] text-slate-600 uppercase font-bold">After</div>
                                    <div className="p-3 bg-emerald-500/5 rounded-lg border border-emerald-500/10 text-[11px] font-mono text-slate-200 whitespace-pre">
                                        {selectedStep.afterState ?
                                            JSON.stringify(parsedAfterState, null, 2)
                                            : "NULL"}
                                    </div>
                                </div>
                            </div>
                        )}
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
};

export default ExpenseTimeline;
