import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { expenseApi } from '../services/api';
import { extractReceiptData } from '../services/receiptOcr';
import { motion, AnimatePresence } from 'framer-motion';
import GlassDropzone from '../components/GlassDropzone';
import { Save, Send, ShieldCheck, AlertTriangle, AlertCircle, CheckCircle2, Loader2, ChevronDown, ChevronUp, X, Sparkles, Eye } from 'lucide-react';

/* ── Confidence Badge ─────────────────────────────────── */
const ConfidenceBadge = ({ level }) => {
    if (!level) return null;
    const config = {
        High: { color: 'text-emerald-400', bg: 'bg-emerald-500/15 border-emerald-500/30', icon: ShieldCheck, label: 'High' },
        Medium: { color: 'text-amber-400', bg: 'bg-amber-500/15 border-amber-500/30', icon: AlertTriangle, label: 'Medium' },
        Low: { color: 'text-red-400', bg: 'bg-red-500/15 border-red-500/30', icon: AlertCircle, label: 'Low' },
    };
    const c = config[level] || config.Low;
    const Icon = c.icon;
    return (
        <motion.span
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider border ${c.bg} ${c.color} ml-2`}
        >
            <Icon size={10} />
            {c.label}
        </motion.span>
    );
};

/* ── OCR Step Indicator ───────────────────────────────── */
const OcrStepIndicator = ({ progress }) => {
    const steps = [
        { label: 'Preparing receipt image...', range: [0, 30] },
        { label: 'Gemini AI is reading your receipt...', range: [30, 85] },
        { label: 'Extracting fields...', range: [85, 100] },
    ];

    const getStepStatus = (step) => {
        if (progress >= step.range[1]) return 'done';
        if (progress >= step.range[0]) return 'active';
        return 'pending';
    };

    return (
        <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl p-4 space-y-3"
        >
            {/* Progress bar */}
            <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden relative">
                {progress === -1 ? (
                    <motion.div
                        className="absolute inset-0 bg-gradient-to-r from-blue-500/0 via-blue-500/50 to-blue-500/0"
                        animate={{ x: ['-100%', '100%'] }}
                        transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}
                    />
                ) : (
                    <motion.div
                        className="h-full bg-gradient-to-r from-neon-blue to-purple-500 rounded-full"
                        initial={{ width: '0%' }}
                        animate={{ width: `${progress}%` }}
                        transition={{ duration: 0.3 }}
                    />
                )}
            </div>

            {/* Steps */}
            <div className="space-y-2">
                {steps.map((step, i) => {
                    const status = getStepStatus(step);
                    return (
                        <motion.div
                            key={i}
                            initial={{ opacity: 0, x: -10 }}
                            animate={{ opacity: 1, x: 0 }}
                            transition={{ delay: i * 0.1 }}
                            className="flex items-center gap-2.5 text-xs"
                        >
                            {status === 'done' && (
                                <motion.div initial={{ scale: 0 }} animate={{ scale: 1 }}>
                                    <CheckCircle2 size={14} className="text-emerald-400" />
                                </motion.div>
                            )}
                            {status === 'active' && (
                                <motion.div animate={{ rotate: 360 }} transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}>
                                    <Loader2 size={14} className="text-neon-blue" />
                                </motion.div>
                            )}
                            {status === 'pending' && (
                                <div className="w-3.5 h-3.5 rounded-full border border-white/20" />
                            )}
                            <span className={
                                status === 'done' ? 'text-emerald-400 line-through opacity-70' :
                                    status === 'active' ? 'text-white font-medium' :
                                        'text-gray-500'
                            }>
                                {step.label}
                            </span>
                            {status === 'active' && (
                                <span className="text-neon-blue font-bold ml-auto">{progress}%</span>
                            )}
                        </motion.div>
                    );
                })}
            </div>
            {progress === -1 && (
                <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    className="mt-4 p-3 rounded-lg bg-blue-500/20 border border-blue-500/40 text-blue-200 text-xs flex items-center justify-center gap-2"
                >
                    <Loader2 size={14} className="animate-spin text-blue-400" />
                    <span>API rate limit reached. Retrying in a few seconds...</span>
                </motion.div>
            )}
        </motion.div>
    );
};

/* ── Main Component ───────────────────────────────────── */
const SubmitExpense = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const existingExpense = location.state?.expense;

    const [formData, setFormData] = useState({
        id: existingExpense?.id || null,
        title: existingExpense?.title || '',
        description: existingExpense?.description || '',
        amount: existingExpense?.amount || '',
        currency: existingExpense?.currency || 'INR',
        expenseDate: existingExpense?.expenseDate || '',
        category: existingExpense?.category || '',
        project: existingExpense?.project || '',
    });
    const [file, setFile] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [submitSuccess, setSubmitSuccess] = useState(null);

    // OCR State
    const [ocrProgress, setOcrProgress] = useState(0);
    const [ocrLoading, setOcrLoading] = useState(false);
    const [ocrResult, setOcrResult] = useState(null);
    const [showRawText, setShowRawText] = useState(false);
    const [showOcrNotice, setShowOcrNotice] = useState(false);
    const [showOcrError, setShowOcrError] = useState(false);
    const [showForeignWarning, setShowForeignWarning] = useState(false);
    const [categoryAutoDetected, setCategoryAutoDetected] = useState(false);

    const categories = ['Travel', 'Meals', 'Office Supplies', 'Software', 'Training', 'Other'];

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        if (e.target.name === 'category') setCategoryAutoDetected(false);
    };

    // Convert date from DD-MM-YYYY to YYYY-MM-DD for <input type="date">
    const formatDateForInput = (rawDate) => {
        if (!rawDate) return '';
        // Handle DD-MM-YYYY or DD/MM/YYYY
        const parts = rawDate.split(/[\/\-]/);
        if (parts.length === 3) {
            let [a, b, c] = parts;
            if (c && c.length === 2) c = '20' + c;
            // If first part is 4 digits, it's YYYY-MM-DD already
            if (a.length === 4) return `${a}-${b.padStart(2, '0')}-${c.padStart(2, '0')}`;
            // DD-MM-YYYY → YYYY-MM-DD
            return `${c}-${b.padStart(2, '0')}-${a.padStart(2, '0')}`;
        }
        return rawDate;
    };

    const handleFileChange = async (e) => {
        const selectedFile = e.target.files[0];
        if (!selectedFile) {
            setFile(null);
            return;
        }
        setFile(selectedFile);

        // Only run OCR on image files
        if (selectedFile.type && selectedFile.type.startsWith('image/')) {
            setOcrLoading(true);
            setOcrProgress(0);
            setOcrResult(null);
            setShowOcrNotice(false);
            setShowOcrError(false);
            setShowForeignWarning(false);
            setCategoryAutoDetected(false);
            try {
                const result = await extractReceiptData(selectedFile, setOcrProgress);

                // If Gemini returned an error flag, show error banner
                if (result.error) {
                    setShowOcrError(true);
                    setOcrResult(null);
                    return;
                }

                setOcrResult(result);
                const updates = {};

                // Auto-fill title as "CATEGORY - Vendor"
                if (result.vendor) {
                    const catLabel = result.category || 'Expense';
                    updates.title = `${catLabel} - ${result.vendor}`;
                }
                if (result.currencyDetected === 'FOREIGN') {
                    updates.amount = '';
                    setShowForeignWarning(true);
                } else if (result.amount) {
                    updates.amount = result.amount;
                }
                if (result.date) updates.expenseDate = formatDateForInput(result.date);
                if (result.category && categories.includes(result.category)) {
                    updates.category = result.category;
                    setCategoryAutoDetected(true);
                }
                // Auto-fill description/notes
                if (result.notes) updates.description = result.notes;

                if (Object.keys(updates).length > 0) {
                    setFormData(prev => ({ ...prev, ...updates }));
                    setShowOcrNotice(true);
                }
            } catch (err) {
                console.error('OCR failed:', err);
                setShowOcrError(true);
            } finally {
                setOcrLoading(false);
            }
        }
    };

    const handleSaveDraft = async () => {
        setError('');
        setLoading(true);
        try {
            await expenseApi.saveDraft(formData);
            navigate('/my-expenses');
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to save draft');
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!file && !existingExpense?.receiptPath) {
            setError('Receipt is mandatory for submission');
            return;
        }

        setError('');
        setLoading(true);

        const expensePayload = {
            title: formData.title || "",
            amount: parseFloat(formData.amount),
            category: formData.category,
            description: formData.description || "",
            currency: "INR",
            date: formData.expenseDate || new Date().toISOString().split('T')[0],
            project: formData.project || "",
            status: "PENDING"
        };

        const data = new FormData();
        data.append('expense', new Blob([JSON.stringify(expensePayload)], { type: 'application/json' }));
        if (file) data.append('file', file);

        try {
            const response = await expenseApi.submit(data);
            if (response.data.status === 'BLOCKED') {
                setError('Expense was BLOCKED by compliance policy. Review your entry.');
            } else if (response.data.status === 'FLAGGED' || response.data.flagReasons?.length > 0) {
                setSubmitSuccess({
                    status: 'FLAGGED',
                    title: 'Action Required',
                    message: 'Your expense was submitted but flagged for review: ' + (response.data.violationDetails || 'Policy Violation detected.')
                });
            } else {
                setSubmitSuccess({
                    status: 'SUCCESS',
                    title: 'Success!',
                    message: 'Expense submitted successfully and is pending approval.'
                });
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Submission failed');
        } finally {
            setLoading(false);
        }
    };

    const renderLabel = (text, fieldKey) => (
        <label className="flex items-center text-gray-400 text-xs uppercase tracking-wider mb-2">
            {text}
            {ocrResult?.confidence?.[fieldKey] && (
                <ConfidenceBadge level={ocrResult.confidence[fieldKey]} />
            )}
        </label>
    );

    return (
        <div className="max-w-4xl mx-auto space-y-6">
            <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
                <h1 className="text-2xl font-bold text-white">New Expense Entry</h1>
                <p className="text-gray-400 text-sm">Log a new financial transaction for approval.</p>
            </motion.div>

            {error && <div className="bg-red-500/10 border border-red-500/50 text-red-400 p-4 rounded-xl">{error}</div>}

            {/* Foreign Currency Warning */}
            <AnimatePresence>
                {showForeignWarning && (
                    <motion.div
                        initial={{ y: -10, opacity: 0 }}
                        animate={{ y: 0, opacity: 1 }}
                        exit={{ y: -10, opacity: 0 }}
                        className="flex items-center justify-between bg-red-500/20 border border-red-500/40 text-red-200 px-4 py-3 rounded-xl"
                    >
                        <div className="flex items-center gap-2 text-sm">
                            <AlertCircle size={16} className="text-red-400" />
                            <span>Foreign currency detected on receipt. Only INR expenses are accepted. Please enter amount manually.</span>
                        </div>
                        <button onClick={() => setShowForeignWarning(false)} className="p-1 hover:bg-white/10 rounded-lg transition-colors">
                            <X size={14} />
                        </button>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* OCR Auto-fill Notice */}
            <AnimatePresence>
                {showOcrNotice && (
                    <motion.div
                        initial={{ y: -10, opacity: 0 }}
                        animate={{ y: 0, opacity: 1 }}
                        exit={{ y: -10, opacity: 0 }}
                        className="flex items-center justify-between bg-yellow-400/10 border border-yellow-400/30 text-yellow-200 px-4 py-3 rounded-xl"
                    >
                        <div className="flex items-center gap-2 text-sm">
                            <Sparkles size={16} className="text-yellow-400" />
                            <span>Fields auto-filled from receipt. Please verify before submitting.</span>
                        </div>
                        <button onClick={() => setShowOcrNotice(false)} className="p-1 hover:bg-white/10 rounded-lg transition-colors">
                            <X size={14} />
                        </button>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* OCR Error Notice */}
            <AnimatePresence>
                {showOcrError && (
                    <motion.div
                        initial={{ y: -10, opacity: 0 }}
                        animate={{ y: 0, opacity: 1 }}
                        exit={{ y: -10, opacity: 0 }}
                        className="flex items-center justify-between bg-amber-500/20 border border-amber-500/40 text-amber-200 px-4 py-3 rounded-xl"
                    >
                        <div className="flex items-center gap-2 text-sm">
                            <AlertTriangle size={16} className="text-amber-400" />
                            <span>AI extraction unavailable. Please fill fields manually.</span>
                        </div>
                        <button onClick={() => setShowOcrError(false)} className="p-1 hover:bg-white/10 rounded-lg transition-colors">
                            <X size={14} />
                        </button>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* Submit Success / Flag Banner */}
            <AnimatePresence>
                {submitSuccess && (
                    <motion.div
                        initial={{ opacity: 0, y: -20, scale: 0.95 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        className={`p-8 rounded-2xl border flex flex-col items-center justify-center text-center space-y-4 ${submitSuccess.status === 'FLAGGED'
                            ? 'bg-orange-900/20 border-orange-500/50 shadow-[0_0_30px_rgba(249,115,22,0.15)]'
                            : 'bg-emerald-900/20 border-emerald-500/50 shadow-[0_0_30px_rgba(16,185,129,0.15)]'
                            }`}
                    >
                        {submitSuccess.status === 'FLAGGED' ? (
                            <div className="p-4 rounded-full bg-orange-500/20 mb-2">
                                <AlertTriangle className="w-12 h-12 text-orange-400" />
                            </div>
                        ) : (
                            <div className="p-4 rounded-full bg-emerald-500/20 mb-2">
                                <CheckCircle2 className="w-12 h-12 text-emerald-400" />
                            </div>
                        )}
                        <div>
                            <h3 className={`text-2xl font-bold ${submitSuccess.status === 'FLAGGED' ? 'text-orange-400' : 'text-emerald-400'}`}>
                                {submitSuccess.title}
                            </h3>
                            <p className="text-gray-300 mt-3 max-w-lg mx-auto leading-relaxed text-sm">
                                {submitSuccess.message}
                            </p>
                        </div>
                        <button
                            onClick={() => navigate('/my-expenses')}
                            className="mt-6 px-8 py-3 bg-white/10 hover:bg-white/20 text-white rounded-xl transition-colors font-bold border border-white/10"
                        >
                            Continue to My Expenses
                        </button>
                    </motion.div>
                )}
            </AnimatePresence>

            {!submitSuccess && (
                <motion.form
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: 0.1 }}
                    onSubmit={handleSubmit}
                    className="glass-card p-8 grid grid-cols-1 md:grid-cols-2 gap-8"
                >
                    {/* Left Column: Details */}
                    <div className="space-y-6">
                        <div>
                            {renderLabel('Title', 'vendor')}
                            <input name="title" required className="w-full glass-input neon-focus" placeholder="e.g. Client Dinner" onChange={handleChange} value={formData.title} />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="flex items-center text-gray-400 text-xs uppercase tracking-wider mb-2">
                                    Amount
                                    {ocrResult && (
                                        ocrResult.currencyDetected === 'FOREIGN' || ocrResult.confidence?.amount === 'Low' ? (
                                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider border bg-red-500/15 border-red-500/30 text-red-400 ml-2">
                                                Could not detect INR amount
                                            </span>
                                        ) : (
                                            <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider border ml-2 ${ocrResult.confidence?.amount === 'High'
                                                ? 'bg-emerald-500/15 border-emerald-500/30 text-emerald-400'
                                                : 'bg-amber-500/15 border-amber-500/30 text-amber-400'
                                                }`}>
                                                ₹ INR — {ocrResult.confidence?.amount}
                                            </span>
                                        )
                                    )}
                                </label>
                                <div className="flex items-center bg-dark-surface/50 border border-white/10 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-neon-blue focus-within:border-transparent transition-all w-full">
                                    <div className="bg-white/10 backdrop-blur-sm px-3 py-2 border-r border-white/20 text-green-400 font-semibold flex items-center justify-center">
                                        ₹
                                    </div>
                                    <input name="amount" type="number" step="0.01" required className="w-full bg-transparent outline-none px-4 py-2 text-white placeholder-gray-500" placeholder="0.00" onChange={handleChange} value={formData.amount} />
                                </div>
                            </div>
                            <div>
                                <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Currency</label>
                                <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-white/5 border border-white/10 text-green-400 font-semibold">
                                    <span>₹</span>
                                    <span>INR — Indian Rupee</span>
                                </div>
                            </div>
                        </div>

                        <div>
                            {renderLabel('Date', 'date')}
                            <input name="expenseDate" type="date" required className="w-full glass-input neon-focus text-gray-400 dark-date-picker" onChange={handleChange} value={formData.expenseDate} />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="flex items-center text-gray-400 text-xs uppercase tracking-wider mb-2">
                                    Category
                                    {ocrResult?.confidence?.category && (
                                        <ConfidenceBadge level={ocrResult.confidence.category} />
                                    )}
                                    {categoryAutoDetected && (
                                        <motion.span
                                            initial={{ opacity: 0, scale: 0.8 }}
                                            animate={{ opacity: 1, scale: 1 }}
                                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider border bg-purple-500/15 border-purple-500/30 text-purple-400 ml-1"
                                        >
                                            <Sparkles size={8} />
                                            Auto-detected
                                        </motion.span>
                                    )}
                                </label>
                                <select name="category" required className="w-full glass-input neon-focus bg-dark-surface/80" onChange={handleChange} value={formData.category}>
                                    <option value="">Select...</option>
                                    {categories.map(c => <option key={c} value={c}>{c}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Project</label>
                                <input name="project" className="w-full glass-input neon-focus" placeholder="e.g. Q4 Migration" onChange={handleChange} value={formData.project} />
                            </div>
                        </div>

                        <div>
                            <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Description</label>
                            <textarea name="description" className="w-full glass-input neon-focus h-24 resize-none" placeholder="Additional details..." onChange={handleChange} value={formData.description}></textarea>
                        </div>
                    </div>

                    {/* Right Column: Upload & Actions */}
                    <div className="space-y-4 flex flex-col">
                        <div>
                            <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Receipt Verification</label>
                            <GlassDropzone onFileChange={handleFileChange} file={file} />
                            {existingExpense?.receiptPath && !file && (
                                <p className="text-xs text-green-400 mt-2">✓ Using existing receipt</p>
                            )}
                        </div>

                        {/* OCR Step Progress */}
                        <AnimatePresence>
                            {ocrLoading && <OcrStepIndicator progress={ocrProgress} />}
                        </AnimatePresence>

                        {/* OCR Result Summary + Raw Text Viewer */}
                        <AnimatePresence>
                            {ocrResult && !ocrLoading && (
                                <motion.div
                                    initial={{ opacity: 0, y: -10 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    exit={{ opacity: 0, y: -10 }}
                                    className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl p-3 space-y-2"
                                >
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center gap-2">
                                            <Sparkles size={14} className="text-purple-400" />
                                            <span className="text-purple-300 text-xs font-medium">Extracted by Gemini Flash AI ✨</span>
                                        </div>
                                        <button
                                            type="button"
                                            onClick={() => setShowRawText(!showRawText)}
                                            className="flex items-center gap-1 text-gray-400 hover:text-white text-[10px] uppercase tracking-wider transition-colors px-2 py-1 rounded-lg hover:bg-white/5"
                                        >
                                            <Eye size={10} />
                                            {showRawText ? 'Hide' : 'View'} Details
                                            {showRawText ? <ChevronUp size={10} /> : <ChevronDown size={10} />}
                                        </button>
                                    </div>

                                    {/* Collapsible Details Viewer */}
                                    <AnimatePresence>
                                        {showRawText && ocrResult.rawText && (
                                            <motion.div
                                                initial={{ height: 0, opacity: 0 }}
                                                animate={{ height: 'auto', opacity: 1 }}
                                                exit={{ height: 0, opacity: 0 }}
                                                transition={{ duration: 0.2 }}
                                                className="overflow-hidden"
                                            >
                                                <textarea
                                                    readOnly
                                                    value={ocrResult.rawText}
                                                    className="w-full h-24 bg-black/30 backdrop-blur-md border border-white/10 rounded-lg p-3 text-gray-300 text-[11px] font-mono resize-none focus:outline-none"
                                                />
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </motion.div>
                            )}
                        </AnimatePresence>

                        <div className="flex-1"></div>

                        <div className="flex gap-4 pt-4 border-t border-white/10">
                            <motion.button
                                type="button"
                                whileHover={{ scale: 1.02 }}
                                whileTap={{ scale: 0.98 }}
                                onClick={handleSaveDraft}
                                disabled={loading}
                                className="flex-1 py-3 rounded-xl border border-white/10 hover:bg-white/5 text-gray-300 font-medium flex items-center justify-center gap-2 transition-colors"
                            >
                                <Save size={18} />
                                Save Draft
                            </motion.button>

                            <motion.button
                                type="submit"
                                whileHover={{ scale: 1.02 }}
                                whileTap={{ scale: 0.98 }}
                                disabled={loading}
                                className="flex-1 py-3 rounded-xl bg-primary-gradient shadow-neon text-white font-bold flex items-center justify-center gap-2"
                            >
                                {loading ? 'Processing...' : (
                                    <>
                                        <Send size={18} />
                                        Submit
                                    </>
                                )}
                            </motion.button>
                        </div>
                    </div>
                </motion.form>
            )}
        </div>
    );
};

export default SubmitExpense;
