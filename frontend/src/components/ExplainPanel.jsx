import React from 'react';
import { motion } from 'framer-motion';
import { Activity, AlertTriangle, Clock, BarChart2, FileText, ShieldAlert } from 'lucide-react';

const riskConfig = {
    CRITICAL: { bg: 'bg-red-500/20', border: 'border-red-500/50', text: 'text-red-300', glow: 'drop-shadow-[0_0_8px_rgba(248,113,113,0.5)]' },
    HIGH: { bg: 'bg-orange-500/20', border: 'border-orange-500/50', text: 'text-orange-300', glow: 'drop-shadow-[0_0_8px_rgba(249,115,22,0.5)]' },
    MEDIUM: { bg: 'bg-yellow-500/20', border: 'border-yellow-500/50', text: 'text-yellow-200', glow: 'drop-shadow-[0_0_8px_rgba(250,204,21,0.5)]' },
    LOW: { bg: 'bg-green-500/20', border: 'border-green-500/50', text: 'text-green-300', glow: 'drop-shadow-[0_0_8px_rgba(74,222,128,0.5)]' },
};

const flagIcons = {
    ANOMALY: <Activity size={16} className="text-red-400 shrink-0 mt-0.5" />,
    'POLICY VIOLATION': <ShieldAlert size={16} className="text-orange-400 shrink-0 mt-0.5" />,
    TIMING: <Clock size={16} className="text-blue-400 shrink-0 mt-0.5" />,
    PATTERN: <AlertTriangle size={16} className="text-yellow-400 shrink-0 mt-0.5" />,
    COMPLETENESS: <FileText size={16} className="text-purple-400 shrink-0 mt-0.5" />,
};

const getIcon = (reason) => {
    for (const [key, icon] of Object.entries(flagIcons)) {
        if (reason.startsWith(key)) return icon;
    }
    return <AlertTriangle size={16} className="text-gray-400 shrink-0 mt-0.5" />;
};

const ExplainPanel = ({ expense }) => {
    const risk = riskConfig[expense.riskLevel] || riskConfig.LOW;
    const reasons = expense.flagReasons
        ? expense.flagReasons.split(' | ').filter(r => r.trim())
        : [];
    const flagCount = expense.flagCount || 0;
    const anomalyScore = expense.anomalyScore;

    const recommendation = flagCount >= 3 ? 'BLOCK' : flagCount === 2 ? 'REVIEW' : flagCount === 1 ? 'MONITOR' : 'CLEAR';

    return (
        <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-4"
        >
            {/* Risk Level Banner */}
            <div className={`${risk.bg} border ${risk.border} rounded-xl p-3 flex items-center justify-between`}>
                <div className="flex items-center gap-2">
                    <ShieldAlert size={18} className={risk.text} />
                    <span className={`font-bold text-sm uppercase tracking-wider ${risk.text} ${risk.glow}`}>
                        Risk Level: {expense.riskLevel || 'LOW'}
                    </span>
                </div>
                <span className={`text-xs ${risk.text} font-mono`}>
                    {flagCount} flag{flagCount !== 1 ? 's' : ''} detected
                </span>
            </div>

            {/* Flag Reasons List */}
            {reasons.length > 0 && (
                <div className="space-y-2">
                    {reasons.map((reason, i) => (
                        <div key={i} className="bg-white/5 border border-white/10 rounded-xl p-3 flex items-start gap-3">
                            {getIcon(reason)}
                            <p className="text-sm text-gray-300 leading-relaxed">{reason}</p>
                        </div>
                    ))}
                </div>
            )}

            {/* Z-Score Visualizer */}
            {anomalyScore != null && (
                <div className="bg-white/5 border border-white/10 rounded-xl p-4">
                    <p className="text-[10px] text-gray-500 uppercase tracking-widest font-bold mb-3">Z-Score Distribution</p>
                    <div className="relative h-6 rounded-full overflow-hidden bg-gradient-to-r from-green-500/20 via-green-500/30 to-green-500/20">
                        {/* Red zones */}
                        <div className="absolute left-0 top-0 bottom-0 w-1/6 bg-red-500/30 rounded-l-full" />
                        <div className="absolute right-0 top-0 bottom-0 w-1/6 bg-red-500/30 rounded-r-full" />
                        {/* Orange zones */}
                        <div className="absolute left-[16.67%] top-0 bottom-0 w-[8.33%] bg-orange-500/20" />
                        <div className="absolute right-[16.67%] top-0 bottom-0 w-[8.33%] bg-orange-500/20" />
                        {/* Marker */}
                        <div
                            className="absolute top-0 bottom-0 w-1 bg-white shadow-[0_0_8px_rgba(255,255,255,0.8)]"
                            style={{ left: `${Math.min(Math.max(((anomalyScore + 4) / 8) * 100, 2), 98)}%` }}
                        />
                    </div>
                    <div className="flex justify-between text-[10px] text-gray-500 mt-1 font-mono">
                        <span>-4</span><span>-2</span><span>0</span><span>+2</span><span>+4</span>
                    </div>
                    <p className="text-xs text-gray-400 mt-2">
                        Z-Score: <span className="text-white font-bold">{anomalyScore.toFixed(2)}</span>
                        {' — '}{Math.abs(anomalyScore).toFixed(1)} standard deviations {anomalyScore > 0 ? 'above' : 'below'} average
                    </p>
                </div>
            )}

            {/* Summary */}
            <p className="text-white/60 text-sm italic">
                This expense triggered {flagCount} flag{flagCount !== 1 ? 's' : ''}.
                Recommend: <span className={`font-bold ${recommendation === 'BLOCK' ? 'text-red-400' : recommendation === 'REVIEW' ? 'text-orange-400' : recommendation === 'MONITOR' ? 'text-yellow-400' : 'text-green-400'}`}>{recommendation}</span>
            </p>
        </motion.div>
    );
};

export default ExplainPanel;
