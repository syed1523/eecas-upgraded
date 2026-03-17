import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    Activity, Shield, User, Clock,
    Zap, HardDrive, RefreshCw, ChevronRight, AlertCircle
} from 'lucide-react';
import { auditApi } from '../services/auditApi';

const LiveActivityFeed = () => {
    const [activities, setActivities] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isPolling, setIsPolling] = useState(true);
    const [lastSync, setLastSync] = useState(new Date());

    const user = JSON.parse(localStorage.getItem('user'));
    const roles = user?.roles || [];
    const canSeeFeed = roles.some(r =>
        ['ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_FINANCE', 'ROLE_MANAGER'].includes(r)
    );

    useEffect(() => {
        if (!canSeeFeed) return;

        fetchRecent();

        let interval;
        if (isPolling) {
            interval = setInterval(fetchRecent, 15000); // 15s polling
        }

        return () => clearInterval(interval);
    }, [isPolling, canSeeFeed]);

    const fetchRecent = async () => {
        try {
            const response = await auditApi.getRecentActivity();
            // Only update if we have new data to avoid flicker
            setActivities(response.data.slice(0, 10)); // Top 10
            setLastSync(new Date());
        } catch (err) {
            console.error("Feed sync failed:", err);
        } finally {
            setLoading(false);
        }
    };

    if (!canSeeFeed) return null;

    return (
        <div className="bg-slate-900/40 backdrop-blur-md rounded-3xl border border-white/5 overflow-hidden flex flex-col h-full h-[400px]">
            <div className="p-5 border-b border-white/5 flex justify-between items-center bg-white/5">
                <div className="flex items-center gap-3">
                    <div className="relative">
                        <Activity className="w-5 h-5 text-emerald-400" />
                        <motion.div
                            animate={{ scale: [1, 1.5, 1], opacity: [0.5, 0, 0.5] }}
                            transition={{ duration: 2, repeat: Infinity }}
                            className="absolute inset-0 bg-emerald-400 rounded-full"
                        />
                    </div>
                    <div>
                        <h3 className="text-sm font-bold text-white uppercase tracking-wider">Audit Activity</h3>
                        <div className="text-[10px] text-slate-500 font-mono flex items-center gap-1">
                            <Clock className="w-2 h-2" /> Sync: {lastSync.toLocaleTimeString()}
                        </div>
                    </div>
                </div>
                <button
                    onClick={() => setIsPolling(!isPolling)}
                    className={`p-2 rounded-xl transition-all ${isPolling ? 'bg-emerald-500/10 text-emerald-400' : 'bg-slate-800 text-slate-500'}`}
                    title={isPolling ? "Auto-refreshing" : "Paused"}
                >
                    <RefreshCw className={`w-4 h-4 ${isPolling ? 'animate-spin-slow' : ''}`} />
                </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-hide">
                {loading ? (
                    Array(5).fill(0).map((_, i) => (
                        <div key={i} className="h-16 bg-white/5 rounded-2xl animate-pulse" />
                    ))
                ) : activities.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-full opacity-40">
                        <HardDrive className="w-8 h-8 mb-2" />
                        <p className="text-xs">No recent activity</p>
                    </div>
                ) : (
                    <AnimatePresence initial={false}>
                        {activities.map((item, idx) => (
                            <motion.div
                                key={item.id}
                                initial={{ opacity: 0, x: -20 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ duration: 0.3 }}
                                className="p-3 bg-white/5 hover:bg-white/10 rounded-2xl border border-white/5 transition-all group"
                            >
                                <div className="flex gap-3">
                                    <div className={`mt-1 w-8 h-8 rounded-xl flex items-center justify-center shrink-0 ${item.wasSystemTriggered ? 'bg-purple-500/20 text-purple-400' : 'bg-slate-800 text-slate-400'
                                        }`}>
                                        {item.wasSystemTriggered ? <Zap className="w-4 h-4" /> : <User className="w-4 h-4" />}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex justify-between items-start">
                                            <p className="text-xs font-bold text-slate-200 truncate">
                                                {item.performedBy}
                                            </p>
                                            <span className="text-[9px] text-slate-500 font-mono">
                                                {new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                            </span>
                                        </div>
                                        <p className="text-[11px] text-slate-400 mt-0.5 line-clamp-1">
                                            {item.changeSummary}
                                        </p>
                                        <div className="mt-1 flex items-center gap-2">
                                            <span className="text-[9px] font-bold text-blue-400 uppercase tracking-tighter">
                                                {item.action.replace(/_/g, ' ')}
                                            </span>
                                            {item.entityId && (
                                                <span className="text-[9px] text-slate-600 bg-black/30 px-1 rounded">
                                                    #{item.entityId}
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                    <div className="flex items-center group-hover:translate-x-1 transition-transform">
                                        <ChevronRight className="w-4 h-4 text-slate-700" />
                                    </div>
                                </div>
                            </motion.div>
                        ))}
                    </AnimatePresence>
                )}
            </div>

            <div className="p-3 text-center bg-white/5 border-t border-white/5">
                <button className="text-[10px] text-slate-500 hover:text-white uppercase font-bold tracking-widest transition-colors">
                    View Full Audit Log
                </button>
            </div>
        </div>
    );
};

export default LiveActivityFeed;
