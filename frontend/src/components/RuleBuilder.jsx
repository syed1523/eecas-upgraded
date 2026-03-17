import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
    Plus, Trash2, Save, Play, RefreshCw,
    ShieldAlert, Settings, AlertOctagon, CheckCircle2
} from 'lucide-react';

const FIELD_OPTIONS = [
    { value: 'amount', label: 'Expense Amount', type: 'numeric' },
    { value: 'category', label: 'Expense Category', type: 'string' },
    { value: 'riskLevel', label: 'Risk Level', type: 'string' },
    { value: 'flagCount', label: 'Number of Flags', type: 'numeric' },
    { value: 'status', label: 'Status', type: 'string' }
];

const OPERATOR_OPTIONS = {
    numeric: [
        { value: '>', label: 'Greater than (>)' },
        { value: '>=', label: 'Greater or equal (>=)' },
        { value: '<', label: 'Less than (<)' },
        { value: '<=', label: 'Less or equal (<=)' },
        { value: '==', label: 'Equals (==)' },
        { value: '!=', label: 'Not equals (!=)' }
    ],
    string: [
        { value: '==', label: 'Equals (==)' },
        { value: '!=', label: 'Not equals (!=)' },
        { value: 'CONTAINS', label: 'Contains text' }
    ]
};

const CATEGORY_PRESETS = [
    'Meals', 'Travel', 'Lodging', 'Software', 'Equipment', 'Office Supplies'
];
const RISK_PRESETS = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

const RuleBuilder = ({ initialRule = null, onSave, onCancel }) => {
    const [ruleName, setRuleName] = useState(initialRule?.ruleName || '');
    const [description, setDescription] = useState(initialRule?.description || '');
    const [action, setAction] = useState(initialRule?.action || 'FLAG');
    const [logic, setLogic] = useState('AND');
    const [conditions, setConditions] = useState([
        { id: Date.now(), field: 'amount', operator: '>', value: '' }
    ]);
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        if (initialRule?.evaluationJson) {
            try {
                const parsed = JSON.parse(initialRule.evaluationJson);
                if (parsed.logic) setLogic(parsed.logic);
                if (parsed.conditions && parsed.conditions.length > 0) {
                    setConditions(parsed.conditions.map((c, i) => ({ ...c, id: Date.now() + i })));
                }
            } catch (e) {
                console.error("Failed to parse existing JSON", e);
            }
        }
    }, [initialRule]);

    const handleAddCondition = () => {
        setConditions([...conditions, { id: Date.now(), field: 'amount', operator: '>', value: '' }]);
    };

    const handleRemoveCondition = (idToRemove) => {
        setConditions(conditions.filter(c => c.id !== idToRemove));
    };

    const handleConditionChange = (id, prop, val) => {
        setConditions(conditions.map(c => {
            if (c.id === id) {
                const result = { ...c, [prop]: val };
                // Reset operator if field type changes
                if (prop === 'field') {
                    const oldType = FIELD_OPTIONS.find(f => f.value === c.field)?.type;
                    const newType = FIELD_OPTIONS.find(f => f.value === val)?.type;
                    if (oldType !== newType) {
                        result.operator = '==';
                        result.value = '';
                    }
                }
                return result;
            }
            return c;
        }));
    };

    const getEvaluationJson = () => {
        return JSON.stringify({
            logic,
            conditions: conditions.map(({ field, operator, value }) => ({ field, operator, value }))
        }, null, 2);
    };

    const handleSave = async (e) => {
        e.preventDefault();
        if (!ruleName.trim()) return;

        setIsSubmitting(true);
        const ruleData = {
            ruleName: ruleName.trim(),
            description: description.trim(),
            action,
            evaluationJson: getEvaluationJson(),
            isActive: initialRule ? initialRule.isActive : true
        };

        try {
            await onSave(ruleData);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-[#111] border border-[#222] rounded-xl overflow-hidden shadow-2xl pb-4 mt-6"
        >
            <div className="bg-gradient-to-r from-[#1E293B] to-[#0F172A] p-4 flex justify-between items-center border-b border-[#333]">
                <div className="flex items-center gap-3">
                    <div className="p-2 bg-indigo-500/20 text-indigo-400 rounded-lg">
                        <Settings className="w-5 h-5" />
                    </div>
                    <div>
                        <h2 className="text-lg font-bold text-white">
                            {initialRule ? 'Edit Compliance Rule' : 'Visual Rule Builder'}
                        </h2>
                        <p className="text-xs text-gray-400">Dynamically evaluate expenses without redeploying.</p>
                    </div>
                </div>
            </div>

            <form onSubmit={handleSave} className="p-6 space-y-8">

                {/* Basic Details */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-1">Rule Name</label>
                            <input
                                type="text"
                                required
                                value={ruleName}
                                onChange={(e) => setRuleName(e.target.value)}
                                placeholder="e.g. Critical High-Value Flight"
                                className="w-full bg-[#1A1A1A] border border-[#333] rounded-lg px-4 py-2 text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-colors"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-1">Description (Optional)</label>
                            <textarea
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                placeholder="Briefly explain the purpose of this rule..."
                                rows="3"
                                className="w-full bg-[#1A1A1A] border border-[#333] rounded-lg px-4 py-2 text-white focus:outline-none focus:border-indigo-500 resize-none transition-colors"
                            />
                        </div>
                    </div>

                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-1">Action to Take</label>
                            <div className="grid grid-cols-3 gap-3">
                                <button
                                    type="button"
                                    onClick={() => setAction('WARN')}
                                    className={`flex flex-col items-center justify-center p-3 rounded-lg border transition-all ${action === 'WARN'
                                            ? 'bg-yellow-500/10 border-yellow-500/50 text-yellow-500'
                                            : 'bg-[#1A1A1A] border-[#333] text-gray-500 hover:border-[#444]'
                                        }`}
                                >
                                    <AlertOctagon className="w-5 h-5 mb-1" />
                                    <span className="text-xs font-semibold">WARN</span>
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setAction('FLAG')}
                                    className={`flex flex-col items-center justify-center p-3 rounded-lg border transition-all ${action === 'FLAG'
                                            ? 'bg-orange-500/10 border-orange-500/50 text-orange-500'
                                            : 'bg-[#1A1A1A] border-[#333] text-gray-500 hover:border-[#444]'
                                        }`}
                                >
                                    <CheckCircle2 className="w-5 h-5 mb-1" />
                                    <span className="text-xs font-semibold">FLAG</span>
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setAction('BLOCK')}
                                    className={`flex flex-col items-center justify-center p-3 rounded-lg border transition-all ${action === 'BLOCK'
                                            ? 'bg-red-500/10 border-red-500/50 text-red-500'
                                            : 'bg-[#1A1A1A] border-[#333] text-gray-500 hover:border-[#444]'
                                        }`}
                                >
                                    <ShieldAlert className="w-5 h-5 mb-1" />
                                    <span className="text-xs font-semibold">BLOCK</span>
                                </button>
                            </div>
                            <p className="text-xs text-gray-500 mt-2">
                                {action === 'BLOCK' && "Prevents the expense from being submitted entirely."}
                                {action === 'FLAG' && "Allows submission but visibly marks it as a policy violation."}
                                {action === 'WARN' && "Requires the employee to provide a mandatory explanation."}
                            </p>
                        </div>
                    </div>
                </div>

                {/* Conditions Builder */}
                <div className="bg-[#1A1A1A] rounded-xl border border-[#333] p-5">
                    <div className="flex justify-between items-center mb-4 pb-4 border-b border-[#333]">
                        <h3 className="font-semibold text-white flex items-center">
                            <RefreshCw className="w-4 h-4 mr-2 text-indigo-400" />
                            Evaluation Conditions
                        </h3>

                        <div className="flex items-center gap-2 bg-[#111] p-1 rounded-lg border border-[#333]">
                            <button
                                type="button"
                                onClick={() => setLogic('AND')}
                                className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${logic === 'AND' ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'
                                    }`}
                            >
                                Match ALL (AND)
                            </button>
                            <button
                                type="button"
                                onClick={() => setLogic('OR')}
                                className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${logic === 'OR' ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'
                                    }`}
                            >
                                Match ANY (OR)
                            </button>
                        </div>
                    </div>

                    <div className="space-y-3">
                        {conditions.map((condition, index) => {
                            const fieldDef = FIELD_OPTIONS.find(f => f.value === condition.field);
                            const fieldType = fieldDef?.type || 'string';
                            const ops = OPERATOR_OPTIONS[fieldType] || OPERATOR_OPTIONS.string;

                            return (
                                <div key={condition.id} className="group flex flex-col md:flex-row gap-3 items-start md:items-center bg-[#111] p-3 rounded-lg border border-[#222] hover:border-[#444] transition-colors">
                                    <span className="bg-[#222] text-xs font-mono px-2 py-1 rounded text-gray-400 min-w-8 text-center hidden md:block">
                                        {index + 1}
                                    </span>

                                    <select
                                        value={condition.field}
                                        onChange={(e) => handleConditionChange(condition.id, 'field', e.target.value)}
                                        className="bg-[#1A1A1A] border border-[#333] text-sm text-white rounded-lg px-3 py-2 outline-none focus:border-indigo-500 flex-1 w-full md:w-auto"
                                    >
                                        {FIELD_OPTIONS.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                                    </select>

                                    <select
                                        value={condition.operator}
                                        onChange={(e) => handleConditionChange(condition.id, 'operator', e.target.value)}
                                        className="bg-[#1A1A1A] border border-[#333] text-sm text-indigo-400 font-mono rounded-lg px-3 py-2 outline-none focus:border-indigo-500 w-full md:w-48"
                                    >
                                        {ops.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                                    </select>

                                    <div className="flex-1 w-full relative">
                                        <input
                                            type={fieldType === 'numeric' ? 'number' : 'text'}
                                            value={condition.value}
                                            required
                                            placeholder={fieldType === 'numeric' ? 'e.g. 5000' : 'e.g. Hotel'}
                                            onChange={(e) => handleConditionChange(condition.id, 'value', e.target.value)}
                                            className="w-full bg-[#1A1A1A] border border-[#333] text-sm text-white rounded-lg px-3 py-2 outline-none focus:border-indigo-500"
                                        />

                                        {/* Presets popover hints */}
                                        {condition.field === 'category' && condition.value === '' && (
                                            <div className="absolute top-10 left-0 bg-[#222] border border-[#333] rounded-lg p-2 shadow-xl z-10 w-64 grid grid-cols-2 gap-1 text-xs invisible group-hover:visible lg:group-focus-within:visible">
                                                {CATEGORY_PRESETS.map(p => (
                                                    <div key={p} className="p-1 hover:bg-[#333] cursor-pointer rounded text-gray-300"
                                                        onClick={() => handleConditionChange(condition.id, 'value', p)}>
                                                        {p}
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                        {condition.field === 'riskLevel' && condition.value === '' && (
                                            <div className="absolute top-10 left-0 bg-[#222] border border-[#333] rounded-lg p-2 shadow-xl z-10 w-48 flex flex-col gap-1 text-xs invisible group-hover:visible lg:group-focus-within:visible">
                                                {RISK_PRESETS.map(p => (
                                                    <div key={p} className="p-1 hover:bg-[#333] cursor-pointer rounded text-gray-300"
                                                        onClick={() => handleConditionChange(condition.id, 'value', p)}>
                                                        {p}
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>

                                    <button
                                        type="button"
                                        onClick={() => handleRemoveCondition(condition.id)}
                                        disabled={conditions.length === 1}
                                        className="p-2 text-gray-500 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors disabled:opacity-50"
                                    >
                                        <Trash2 className="w-4 h-4" />
                                    </button>
                                </div>
                            );
                        })}
                    </div>

                    <button
                        type="button"
                        onClick={handleAddCondition}
                        className="mt-4 flex items-center text-sm text-indigo-400 hover:text-indigo-300 transition-colors px-2 py-1"
                    >
                        <Plus className="w-4 h-4 mr-1" /> Add Condition
                    </button>
                </div>

                {/* Live Preview & Actions */}
                <div className="flex flex-col md:flex-row gap-6 items-end">
                    <div className="flex-1 w-full bg-[#111] p-0 rounded-lg border border-[#222] overflow-hidden group relative">
                        <div className="bg-[#1A1A1A] px-3 py-1 text-xs text-gray-500 border-b border-[#222] flex justify-between">
                            <span className="font-mono">evaluationJson_preview.json</span>
                            <span>Generated in real-time</span>
                        </div>
                        <pre className="p-4 text-xs font-mono text-emerald-400 overflow-x-auto m-0 opacity-80 group-hover:opacity-100 transition-opacity whitespace-pre-wrap">
                            {getEvaluationJson()}
                        </pre>
                    </div>

                    <div className="flex gap-3 w-full md:w-auto">
                        {onCancel && (
                            <button
                                type="button"
                                onClick={onCancel}
                                className="flex-1 md:flex-none px-6 py-2.5 rounded-lg border border-[#333] text-gray-300 hover:bg-[#222] transition-colors"
                            >
                                Cancel
                            </button>
                        )}
                        <button
                            type="submit"
                            disabled={isSubmitting || !ruleName.trim()}
                            className="flex-1 md:flex-none flex items-center justify-center bg-indigo-600 hover:bg-indigo-500 text-white px-6 py-2.5 rounded-lg transition-colors shadow-lg shadow-indigo-500/20 disabled:opacity-50"
                        >
                            {isSubmitting ? (
                                <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                            ) : (
                                <Save className="w-4 h-4 mr-2" />
                            )}
                            {initialRule ? 'Update Rule' : 'Save Rule'}
                        </button>
                    </div>
                </div>

            </form>
        </motion.div>
    );
};

export default RuleBuilder;
