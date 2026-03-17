import React, { useEffect, useState, useCallback } from 'react';
import { adminApi } from '../services/api';
import { analyticsApi } from '../services/analyticsApi';
import { motion } from 'framer-motion';
import {
    Users, Shield, Settings, Database, Lock,
    ToggleLeft, ToggleRight, AlertTriangle, Eye,
    Award, BarChart2, TrendingUp, Activity, AlertOctagon
} from 'lucide-react';
import RuleBuilder from '../components/RuleBuilder';

// ─── Shared Tab Button ────────────────────────────────────────────────────────
const TabButton = ({ id, icon: Icon, label, active, onClick }) => (
    <button
        onClick={() => onClick(id)}
        className={`flex items-center gap-2 px-5 py-2.5 rounded-xl transition-all font-medium text-sm
            ${active === id
                ? 'bg-primary-gradient text-white shadow-neon'
                : 'text-gray-400 hover:text-white hover:bg-white/5'}`}
    >
        <Icon size={16} />
        {label}
    </button>
);

// ─── Role Badge ───────────────────────────────────────────────────────────────
const colors = {
    ROLE_ADMIN: 'bg-red-900/40 text-red-300 border-red-700/40',
    ROLE_MANAGER: 'bg-yellow-900/40 text-yellow-300 border-yellow-700/40',
    ROLE_AUDITOR: 'bg-purple-900/40 text-purple-300 border-purple-700/40',
    ROLE_FINANCE: 'bg-blue-900/40 text-blue-300 border-blue-700/40',
    ROLE_EMPLOYEE: 'bg-green-900/40 text-green-300 border-green-700/40',
};
const RoleBadge = ({ role }) => {
    const name = role?.name || String(role);
    return (
        <span className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded-full border ${colors[name] || 'bg-white/10 text-gray-300 border-white/20'}`}>
            {name.replace('ROLE_', '')}
        </span>
    );
};

// ─── Status Badge ─────────────────────────────────────────────────────────────
const StatusBadge = ({ active }) => active
    ? <span className="text-[10px] bg-green-900/40 text-green-300 border border-green-700/40 px-2 py-0.5 rounded-full font-bold">ACTIVE</span>
    : <span className="text-[10px] bg-red-900/40 text-red-300 border border-red-700/40 px-2 py-0.5 rounded-full font-bold">DISABLED</span>;

// ─── ANALYTICS DASHBOARD ──────────────────────────────────────────────────────
const AnalyticsTab = () => {
    const [overview, setOverview] = useState(null);
    const [trends, setTrends] = useState([]);
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            analyticsApi.getEnterpriseOverview(),
            analyticsApi.getExecutiveTrends(),
            analyticsApi.getAdminAlerts()
        ]).then(([resO, resT, resA]) => {
            setOverview(resO.data);
            setTrends(resT.data || []);
            setAlerts(resA.data || []);
        }).catch(console.error).finally(() => setLoading(false));
    }, []);

    if (loading) return <div className="text-gray-400 text-sm py-10 text-center">Loading enterprise intelligence...</div>;

    return (
        <div className="space-y-6">
            {alerts.length > 0 && (
                <div className="bg-red-500/20 border border-red-500/50 rounded-xl p-4 flex flex-col gap-2 shadow-neon">
                    <div className="flex items-center gap-2 text-red-400 font-bold text-sm uppercase tracking-wider">
                        <AlertOctagon size={18} /> Enterprise Governance Alerts
                    </div>
                    <ul className="list-disc pl-5 text-red-300 text-xs space-y-1">
                        {alerts.map((a, i) => <li key={i}>{a}</li>)}
                    </ul>
                </div>
            )}

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="glass-card p-4 border-l-2 border-green-500">
                    <p className="text-[10px] text-gray-500 font-bold uppercase tracking-wider mb-1">Global Compliance</p>
                    <p className="text-2xl font-black text-white">{overview?.globalComplianceScore?.toFixed(1) || 100}</p>
                </div>
                <div className="glass-card p-4 border-l-2 border-red-500">
                    <p className="text-[10px] text-gray-500 font-bold uppercase tracking-wider mb-1">Enterprise Risk Index</p>
                    <p className="text-2xl font-black text-red-400">{overview?.globalRiskIndex?.toFixed(1) || 0}</p>
                </div>
                <div className="glass-card p-4 border-l-2 border-yellow-500">
                    <p className="text-[10px] text-gray-500 font-bold uppercase tracking-wider mb-1">Escalation Rate</p>
                    <p className="text-2xl font-black text-yellow-400">{((overview?.averageEscalationRate || 0) * 100).toFixed(1)}%</p>
                </div>
                <div className="glass-card p-4 border-l-2 border-purple-500">
                    <p className="text-[10px] text-gray-500 font-bold uppercase tracking-wider mb-1">Avg Resolution Time</p>
                    <p className="text-2xl font-black text-purple-400">{overview?.averageResolutionTimeHours?.toFixed(1) || 0} hrs</p>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="glass-card p-6">
                    <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4 flex items-center gap-2"><TrendingUp size={16} /> Enterprise Risk Trend 30D</h3>
                    <div className="h-48 flex items-end gap-1 border-l border-b border-white/10 p-2">
                        {trends.length === 0 ? <p className="text-gray-500 text-xs w-full text-center pb-20">No historical data.</p> : trends.map((day, i) => (
                            <div key={i} className="flex-1 bg-gradient-to-t from-red-900/50 to-red-500/50 rounded-t group relative hover:from-red-500 hover:to-red-400 transition-all" style={{ height: `${Math.min(day.globalRiskIndex * 5, 100)}%` }}>
                                <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-black/80 px-2 py-1 rounded text-[10px] text-white opacity-0 group-hover:opacity-100 whitespace-nowrap z-10">
                                    {day.recordDate}: {day.globalRiskIndex?.toFixed(1)}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="glass-card p-6">
                    <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4 flex items-center gap-2"><Activity size={16} /> Override Frequency Snapshot</h3>
                    <div className="flex flex-col justify-center h-48 space-y-4">
                        <div className="text-center">
                            <p className="text-5xl font-black text-white drop-shadow-[0_0_15px_rgba(255,255,255,0.3)]">
                                {overview?.totalOverrides || 0}
                            </p>
                            <p className="text-xs text-gray-400 uppercase font-bold mt-2">Total Manager Overrides</p>
                        </div>
                        <div className="w-full bg-white/5 rounded-full h-4 overflow-hidden relative border border-white/10">
                            <div className="bg-yellow-500 h-full w-[15%]" />
                        </div>
                        <p className="text-[10px] text-gray-500 text-center">Overrides represent approximately 15% of all approvals.</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

// ─── USER MANAGEMENT ─────────────────────────────────────────────────────────
const UserTab = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [actioning, setActioning] = useState(null);

    const ASSIGNABLE_ROLES = ['ROLE_EMPLOYEE', 'ROLE_MANAGER', 'ROLE_FINANCE', 'ROLE_AUDITOR'];

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const res = await adminApi.getUsers();
            const usersData = res.data.content || res.data;
            setUsers(Array.isArray(usersData) ? usersData : []);
        } catch (e) { console.error(e); }
        finally { setLoading(false); }
    }, []);

    useEffect(() => { load(); }, [load]);

    const toggleStatus = async (u) => {
        setActioning(u.id);
        try {
            await adminApi.setUserStatus(u.id, !u.active);
            setUsers(prev => prev.map(x => x.id === u.id ? { ...x, active: !x.active } : x));
        } catch (e) { console.error(e); }
        finally { setActioning(null); }
    };

    const changeRole = async (id, role) => {
        setActioning(id);
        try {
            await adminApi.changeUserRole(id, role);
            await load();
        } catch (e) { console.error(e); }
        finally { setActioning(null); }
    };

    if (loading) return <div className="text-gray-400 text-sm py-10 text-center">Loading users...</div>;

    return (
        <div className="space-y-4">
            <p className="text-[11px] text-gray-500">All role changes are audit logged. ROLE_ADMIN cannot be assigned via this interface.</p>
            <div className="overflow-x-auto">
                <table className="min-w-full">
                    <thead className="bg-white/5 border-b border-white/10">
                        <tr>
                            {['ID', 'Username', 'Email', 'Department', 'Role', 'Status', 'Actions'].map(h => (
                                <th key={h} className="text-left py-3 px-4 text-[11px] font-bold text-gray-400 uppercase tracking-wider">{h}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                        {users.map(u => (
                            <tr key={u.id} className="hover:bg-white/5 transition">
                                <td className="py-3 px-4 text-gray-500 text-xs">{u.id}</td>
                                <td className="py-3 px-4 text-white font-medium text-sm">{u.username}</td>
                                <td className="py-3 px-4 text-gray-400 text-xs">{u.email}</td>
                                <td className="py-3 px-4 text-gray-400 text-xs">{u.departmentName || '—'}</td>
                                <td className="py-3 px-4">
                                    {/* Block role reassignment for ADMIN accounts */}
                                    {u.roles?.[0] === 'ROLE_ADMIN'
                                        ? <RoleBadge role={u.roles[0]} />
                                        : (
                                            <select
                                                disabled={actioning === u.id}
                                                value={u.roles?.[0] || 'ROLE_EMPLOYEE'}
                                                onChange={e => changeRole(u.id, e.target.value)}
                                                className="bg-transparent border border-white/10 text-gray-300 text-xs rounded-lg px-2 py-1 focus:outline-none focus:border-neon-blue cursor-pointer"
                                            >
                                                {ASSIGNABLE_ROLES.map(r => <option key={r} value={r} className="bg-gray-900">{r.replace('ROLE_', '')}</option>)}
                                            </select>
                                        )
                                    }
                                </td>
                                <td className="py-3 px-4"><StatusBadge active={u.active || u.isActive} /></td>
                                <td className="py-3 px-4">
                                    <button
                                        disabled={actioning === u.id || u.roles?.[0] === 'ROLE_ADMIN'}
                                        onClick={() => toggleStatus(u)}
                                        title={u.active ? 'Disable Account' : 'Enable Account'}
                                        className="p-1 rounded-lg hover:bg-white/10 transition text-gray-400 hover:text-white disabled:opacity-30"
                                    >
                                        {u.active ? <ToggleRight size={20} className="text-green-400" /> : <ToggleLeft size={20} />}
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

// ─── DEPARTMENT MANAGEMENT ────────────────────────────────────────────────────
const DepartmentTab = () => {
    const [depts, setDepts] = useState([]);
    const [newName, setNewName] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        adminApi.getDepartments().then(r => setDepts(r.data || [])).catch(console.error).finally(() => setLoading(false));
    }, []);

    const create = async () => {
        if (!newName.trim()) return;
        try {
            const res = await adminApi.createDept({ name: newName.trim() });
            setDepts(prev => [...prev, res.data]);
            setNewName('');
        } catch (e) { console.error(e); }
    };

    if (loading) return <div className="text-gray-400 text-sm py-10 text-center">Loading departments...</div>;

    return (
        <div className="space-y-6">
            <div className="flex gap-3">
                <input
                    value={newName} onChange={e => setNewName(e.target.value)}
                    placeholder="New department name..."
                    className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-white text-sm focus:outline-none focus:border-neon-blue placeholder-gray-600"
                />
                <button onClick={create} className="px-4 py-2 bg-primary-gradient text-white rounded-xl text-sm font-bold hover:opacity-90 transition">Create</button>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {depts.length === 0 && <p className="text-gray-500 text-sm col-span-3">No departments configured.</p>}
                {depts.map(d => (
                    <div key={d.id} className="p-4 bg-white/5 border border-white/10 rounded-xl hover:bg-white/10 transition">
                        <div className="flex justify-between items-center">
                            <span className="text-white font-medium text-sm">{d.name}</span>
                            <span className="text-[10px] text-gray-500 bg-white/5 px-2 py-0.5 rounded">ID: {d.id}</span>
                        </div>
                        <p className="text-gray-500 text-xs mt-1">{d.description || 'No description'}</p>
                    </div>
                ))}
            </div>
        </div>
    );
};

// ─── POLICY ENGINE ────────────────────────────────────────────────────────────
const PolicyTab = () => {
    const [rules, setRules] = useState([]);
    const [loading, setLoading] = useState(true);
    const [editingRule, setEditingRule] = useState(null); // rule object or 'new'

    useEffect(() => {
        adminApi.getRules().then(r => setRules(r.data || [])).catch(console.error).finally(() => setLoading(false));
    }, []);

    const handleSaveRule = async (ruleData) => {
        try {
            if (editingRule === 'new') {
                const res = await adminApi.createRule(ruleData);
                setRules(prev => [...prev, res.data]);
            } else {
                const res = await adminApi.updateRule(editingRule.id, ruleData);
                setRules(prev => prev.map(r => r.id === editingRule.id ? res.data : r));
            }
            setEditingRule(null);
        } catch (e) {
            console.error("Failed to save rule", e);
            alert("Error saving rule: " + (e.response?.data?.message || e.message));
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm("Are you sure you want to delete this rule?")) return;
        try {
            await adminApi.deleteRule(id);
            setRules(prev => prev.filter(r => r.id !== id));
        } catch (e) { console.error(e); }
    };

    const handleToggle = async (id) => {
        try {
            const res = await adminApi.toggleRule(id);
            setRules(prev => prev.map(r => r.id === id ? res.data : r));
        } catch (e) { console.error(e); }
    };

    if (loading) return <div className="text-gray-400 text-sm py-10 text-center">Loading policy rules...</div>;

    if (editingRule) {
        return (
            <div className="space-y-4">
                <RuleBuilder
                    initialRule={editingRule === 'new' ? null : editingRule}
                    onSave={handleSaveRule}
                    onCancel={() => setEditingRule(null)}
                />
            </div>
        );
    }

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center">
                <p className="text-[11px] text-gray-500">All policy changes create an immutable audit record with old vs new values.</p>
                <button onClick={() => setEditingRule('new')} className="px-3 py-1.5 bg-white/10 text-white rounded-lg text-xs hover:bg-white/20 transition">
                    + New Rule
                </button>
            </div>

            <div className="grid gap-4">
                {rules.length === 0 && <p className="text-gray-500 text-sm">No compliance rules configured.</p>}
                {rules.map(r => (
                    <div key={r.id} className="p-4 rounded-xl bg-white/5 border border-white/10 flex justify-between items-start hover:bg-white/10 transition group">
                        <div className="flex-1 min-w-0 pr-4">
                            <div className="flex flex-wrap items-center gap-2 mb-1">
                                <h3 className="font-bold text-white text-sm max-w-[200px] truncate" title={r.ruleName}>{r.ruleName}</h3>
                                <span className={`text-[10px] px-2 py-0.5 rounded border uppercase tracking-wider font-bold
                                    ${r.isActive ? 'bg-green-500/20 text-green-400 border-green-500/40' : 'bg-gray-500/20 text-gray-400 border-gray-500/40'}`}>
                                    {r.isActive ? 'ACTIVE' : 'INACTIVE'}
                                </span>
                                <span className={`text-[10px] px-2 py-0.5 rounded border uppercase tracking-wider font-bold
                                    ${r.action === 'BLOCK' ? 'bg-red-500/20 text-red-400 border-red-500/40' :
                                        r.action === 'FLAG' ? 'bg-orange-500/20 text-orange-400 border-orange-500/40' :
                                            'bg-yellow-500/20 text-yellow-400 border-yellow-500/40'}`}>
                                    {r.action}
                                </span>
                                <div className="text-[10px] text-gray-600 flex items-center gap-2 flex-grow justify-end">
                                    <button onClick={() => setEditingRule(r)} className="text-indigo-400 hover:text-indigo-300 opacity-0 group-hover:opacity-100 transition-opacity">Edit</button>
                                    <button onClick={() => handleDelete(r.id)} className="text-red-400 hover:text-red-300 opacity-0 group-hover:opacity-100 transition-opacity">Delete</button>
                                </div>
                            </div>
                            <p className="text-xs text-gray-400 truncate w-full" title={r.description}>{r.description}</p>
                            {r.evaluationJson && (
                                <div className="mt-2 text-[11px] font-mono bg-black/30 p-2 rounded text-neon-blue border border-white/5 max-h-24 overflow-y-auto w-full max-w-full overflow-x-hidden break-words whitespace-pre-wrap">
                                    {r.evaluationJson}
                                </div>
                            )}
                        </div>
                        <div className="pt-1 flex-shrink-0">
                            <button
                                onClick={() => handleToggle(r.id)}
                                title={r.isActive ? 'Disable Rule' : 'Enable Rule'}
                                className="p-1 rounded-lg hover:bg-white/10 transition text-gray-400 hover:text-white"
                            >
                                {r.isActive ? <ToggleRight size={20} className="text-green-400" /> : <ToggleLeft size={20} />}
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

// ─── CONFIGURATIONS (Phase 1) ───────────────────────────────────────────────────
const ConfigTab = () => {
    const [configs, setConfigs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [editing, setEditing] = useState(null);
    const [editValue, setEditValue] = useState('');

    useEffect(() => {
        adminApi.getConfigs().then(r => setConfigs(r.data || [])).catch(console.error).finally(() => setLoading(false));
    }, []);

    const saveConfig = async (key) => {
        try {
            await adminApi.updateConfig(key, editValue);
            setConfigs(configs.map(c => c.configKey === key ? { ...c, configValue: editValue, lastUpdatedAt: new Date().toISOString() } : c));
            setEditing(null);
        } catch (e) {
            alert('Failed to update config: ' + e.message);
        }
    };

    if (loading) return <div className="text-gray-400 text-sm py-10 text-center">Loading system configs...</div>;

    return (
        <div className="space-y-4">
            <p className="text-[11px] text-gray-500">Manage global SLA thresholds, high-risk scoring, and systemic constraints.</p>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {configs.map(c => (
                    <div key={c.configKey} className="p-4 bg-white/5 border border-white/10 rounded-xl hover:bg-white/10 transition flex flex-col justify-between">
                        <div>
                            <h3 className="text-white font-bold text-sm mb-1">{c.configKey.replace(/_/g, ' ')}</h3>
                            <p className="text-[10px] text-gray-500 mb-4 tracking-wider">LST UPD: {c.lastUpdatedAt ? new Date(c.lastUpdatedAt).toLocaleString() : 'System Boot'}</p>
                        </div>
                        {editing === c.configKey ? (
                            <div className="flex gap-2">
                                <input value={editValue} onChange={e => setEditValue(e.target.value)} className="w-full bg-black/50 border border-neon-blue rounded px-2 text-white text-sm focus:outline-none" />
                                <button onClick={() => saveConfig(c.configKey)} className="text-green-400 text-xs font-bold px-2 bg-green-900/40 rounded hover:bg-green-500 hover:text-white transition">Save</button>
                                <button onClick={() => setEditing(null)} className="text-gray-400 text-xs px-2 bg-white/10 rounded hover:bg-white/20 transition">Esc</button>
                            </div>
                        ) : (
                            <div className="flex justify-between items-end">
                                <div className="text-2xl font-mono text-neon-blue font-bold">{c.configValue}</div>
                                <button onClick={() => { setEditing(c.configKey); setEditValue(c.configValue); }} className="text-xs text-gray-400 hover:text-white transition underline decoration-white/30 underline-offset-2">Edit</button>
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
};

// ─── POLICY RECOMMENDATIONS (Phase 7) ─────────────────────────────────────────
const RecommendationTab = () => {
    const [recs, setRecs] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        adminApi.getRecommendations().then(r => setRecs(r.data || [])).catch(console.error).finally(() => setLoading(false));
    }, []);

    const actionRec = async (id, status) => {
        try {
            await adminApi.updateRecommendation(id, status);
            setRecs(recs.map(r => r.id === id ? { ...r, status } : r));
        } catch (e) {
            console.error(e);
        }
    };

    if (loading) return <div className="text-gray-400 text-sm py-10 text-center">Loading AI recommendations...</div>;

    return (
        <div className="space-y-4">
            <p className="text-[11px] text-gray-500">Anomaly Engine insights based on historical fraud patterns and volume breaches.</p>
            {recs.length === 0 ? <p className="text-gray-500 text-sm">No actionable recommendations at this time.</p> : (
                <div className="space-y-3">
                    {recs.map(r => (
                        <div key={r.id} className="p-5 border border-purple-500/30 bg-purple-900/10 rounded-xl relative overflow-hidden group hover:border-purple-500/60 transition">
                            <div className="absolute top-0 right-0 p-3 flex gap-2">
                                <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded border ${r.status === 'PENDING' ? 'bg-yellow-900/50 text-yellow-400 border-yellow-700/50' : r.status === 'IMPLEMENTED' ? 'bg-green-900/50 text-green-400 border-green-700/50' : 'bg-gray-800 text-gray-400 border-gray-600'}`}>{r.status}</span>
                            </div>
                            <div className="flex items-center gap-2 mb-2">
                                <div className="w-2 h-2 rounded-full bg-purple-500 animate-pulse"></div>
                                <h3 className="text-white font-bold text-sm">Violation Spike Detected: {r.violationType}</h3>
                            </div>
                            <p className="text-gray-300 text-xs mb-4 max-w-2xl">{r.suggestedRuleAdjustment}</p>

                            <div className="flex justify-between items-end border-t border-white/5 pt-3">
                                <div>
                                    <p className="text-[10px] text-gray-500 font-mono tracking-wider">OCCURRENCES: <span className="text-white text-sm">{r.occurrenceCount}</span></p>
                                    <p className="text-[10px] text-gray-500 font-mono tracking-wider">GENERATED: {new Date(r.generatedAt).toLocaleString()}</p>
                                </div>

                                {r.status === 'PENDING' && (
                                    <div className="flex gap-2">
                                        <button onClick={() => actionRec(r.id, 'IMPLEMENTED')} className="text-xs bg-green-500/10 text-green-400 border border-green-500/30 px-3 py-1.5 rounded-lg hover:bg-green-500 hover:text-white transition font-medium">Mark Implemented</button>
                                        <button onClick={() => actionRec(r.id, 'DISMISSED')} className="text-xs bg-white/5 text-gray-400 border border-white/10 px-3 py-1.5 rounded-lg hover:bg-white/10 transition font-medium">Dismiss</button>
                                    </div>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

// ─── SECURITY MONITOR ─────────────────────────────────────────────────────────
const SecurityTab = () => {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('');

    const SECURITY_EVENTS = ['ADMIN_ROLE_CHANGE', 'ADMIN_USER_STATUS', 'ADMIN_RULE_CREATE', 'ADMIN_DEPT_CREATE', 'FAILED_LOGIN', 'ACCESS_DENIED', 'POLICY_VIOLATION'];

    useEffect(() => {
        adminApi.getLogs(0, 100).then(r => {
            const all = r.data.content || [];
            // Filter to security-relevant admin actions only
            const secLogs = all.filter(l => SECURITY_EVENTS.some(e => l.action?.startsWith('ADMIN_') || l.action === e));
            setLogs(secLogs.length > 0 ? secLogs : all); // fallback to all if no admin actions yet
        }).catch(console.error).finally(() => setLoading(false));
    }, []);

    const filtered = filter.trim()
        ? logs.filter(l => l.action?.toLowerCase().includes(filter.toLowerCase()) ||
            l.performedBy?.toLowerCase().includes(filter.toLowerCase()) ||
            l.entityType?.toLowerCase().includes(filter.toLowerCase()))
        : logs;

    const severity = (action) => {
        if (action?.includes('ROLE_CHANGE') || action?.includes('STATUS')) return 'text-red-400 border-red-800/50 bg-red-900/10';
        if (action?.includes('RULE') || action?.includes('POLICY')) return 'text-yellow-400 border-yellow-800/50 bg-yellow-900/10';
        return 'text-gray-400 border-white/10 bg-white/5';
    };

    if (loading) return <div className="text-gray-400 text-sm py-10 text-center">Loading security events...</div>;

    return (
        <div className="space-y-4">
            <div className="flex gap-3">
                <input
                    value={filter} onChange={e => setFilter(e.target.value)}
                    placeholder="Filter by action, user, or entity..."
                    className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-white text-sm focus:outline-none focus:border-neon-blue placeholder-gray-600"
                />
                <span className="text-xs text-gray-500 self-center whitespace-nowrap">{filtered.length} events</span>
            </div>

            {/* Severity Legend */}
            <div className="flex gap-3 text-[10px]">
                <span className="text-red-400">🔴 HIGH — Role / Status changes</span>
                <span className="text-yellow-400">🟡 MED — Policy modifications</span>
                <span className="text-gray-400">⚪ LOW — System events</span>
            </div>

            <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
                {filtered.length === 0 && <p className="text-gray-500 text-sm">No security events recorded yet. Events appear here when admin actions are performed.</p>}
                {filtered.map((l, i) => (
                    <div key={l.id || i} className={`rounded-lg px-4 py-3 border flex items-start justify-between gap-4 ${severity(l.action)}`}>
                        <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 mb-0.5">
                                <AlertTriangle size={12} />
                                <p className="text-sm font-bold uppercase tracking-wide">{l.action}</p>
                            </div>
                            <p className="text-[11px] opacity-80">
                                By: <strong>{l.performedBy}</strong> [{l.performerRole?.replace('ROLE_', '')}] · Entity: {l.entityType} #{l.entityId}
                            </p>
                            {(l.oldValue && l.oldValue !== 'NULL') && (
                                <p className="text-[10px] opacity-60 mt-0.5">
                                    Before: {l.oldValue} → After: {l.newValue}
                                </p>
                            )}
                        </div>
                        <span className="text-[10px] opacity-60 shrink-0 text-right">
                            {l.timestamp ? new Date(l.timestamp).toLocaleString() : '—'}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
};

// ─── SYSTEM LOGS ──────────────────────────────────────────────────────────────
const LogsTab = () => {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [actionF, setActionF] = useState('');
    const [userF, setUserF] = useState('');

    useEffect(() => {
        adminApi.getLogs(0, 100).then(r => setLogs(r.data.content || [])).catch(console.error).finally(() => setLoading(false));
    }, []);

    const filtered = logs.filter(l =>
        (!actionF || l.action?.toLowerCase().includes(actionF.toLowerCase())) &&
        (!userF || l.performedBy?.toLowerCase().includes(userF.toLowerCase()))
    );

    if (loading) return <div className="text-gray-400 text-sm py-10 text-center">Loading system logs...</div>;

    return (
        <div className="space-y-4">
            <p className="text-[11px] text-gray-500">Read-only system log view. No edit or delete operations permitted.</p>
            <div className="flex gap-3">
                <input value={actionF} onChange={e => setActionF(e.target.value)} placeholder="Filter by action..."
                    className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-white text-sm focus:outline-none focus:border-neon-blue placeholder-gray-600" />
                <input value={userF} onChange={e => setUserF(e.target.value)} placeholder="Filter by user..."
                    className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-white text-sm focus:outline-none focus:border-neon-blue placeholder-gray-600" />
            </div>
            <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
                {filtered.length === 0 && <p className="text-gray-500 text-sm">No log entries match the filter.</p>}
                {filtered.map((l, i) => (
                    <div key={l.id || i} className="bg-white/5 rounded-lg px-4 py-3 flex items-start justify-between gap-4">
                        <div className="flex-1 min-w-0">
                            <p className="text-white text-xs font-bold uppercase">{l.action}</p>
                            <p className="text-gray-400 text-[11px] mt-0.5">
                                {l.performedBy} [{l.performerRole?.replace('ROLE_', '')}] · {l.entityType} #{l.entityId}
                            </p>
                            {(l.oldValue && l.oldValue !== 'NULL') && (
                                <p className="text-[10px] text-gray-600 mt-0.5 truncate">
                                    {l.oldValue} → {l.newValue}
                                </p>
                            )}
                        </div>
                        <span className="text-[10px] text-gray-500 shrink-0">
                            {l.timestamp ? new Date(l.timestamp).toLocaleString() : '—'}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
};

// ─── ROOT ADMIN PAGE ─────────────────────────────────────────────────────────
const Admin = () => {
    const [executiveMode, setExecutiveMode] = useState(false);
    const [activeTab, setActiveTab] = useState('users');

    const allTabs = [
        { id: 'analytics', icon: BarChart2, label: 'Analytics Dashboard', execOnly: true },
        { id: 'users', icon: Users, label: 'User Management' },
        { id: 'depts', icon: Database, label: 'Departments' },
        { id: 'policy', icon: Shield, label: 'Policy Engine' },
        { id: 'configs', icon: Settings, label: 'System Configs' },
        { id: 'recommendations', icon: Eye, label: 'Recommendations', execIncluded: true },
        { id: 'security', icon: AlertTriangle, label: 'Security Monitor', execIncluded: true },
        { id: 'logs', icon: Lock, label: 'System Logs' },
    ];

    const tabs = allTabs.filter(t => executiveMode ? (t.execOnly || t.execIncluded) : !t.execOnly);

    // Watcher to reset tab if switching mode
    useEffect(() => {
        if (executiveMode && !['analytics', 'recommendations', 'security'].includes(activeTab)) {
            setActiveTab('analytics');
        } else if (!executiveMode && activeTab === 'analytics') {
            setActiveTab('users');
        }
    }, [executiveMode, activeTab]);

    return (
        <div className="space-y-6">
            <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
                {/* Governance Identity Banner */}
                <div className="glass-card px-5 py-3 flex items-center justify-between border border-red-500/20 mb-4">
                    <div>
                        <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-0.5">Role</p>
                        <p className="text-white text-sm font-bold">System Administrator</p>
                    </div>
                    <div className="text-center">
                        <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-0.5">Scope</p>
                        <p className="text-red-400 text-sm font-bold">Enterprise Governance</p>
                    </div>
                    <div className="text-right">
                        <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-0.5">Restrictions</p>
                        <p className="text-gray-400 text-xs">No expense submission · No approvals · No overrides</p>
                    </div>
                </div>
                <div className="flex justify-between items-start">
                    <div>
                        <h1 className="text-2xl font-bold text-white flex items-center gap-3">
                            {executiveMode ? (
                                <><BarChart2 className="text-neon-purple" /> Executive Governance Portal</>
                            ) : (
                                <><Shield className="text-neon-blue" /> Admin Control Center</>
                            )}
                        </h1>
                        <p className="text-gray-400 text-sm mt-1">
                            {executiveMode
                                ? "C-Suite view of enterprise risk, compliance trends, and AI recommendations."
                                : "Manage users, departments, compliance policies, and system-wide security events."}
                        </p>
                    </div>

                    <button
                        onClick={() => setExecutiveMode(!executiveMode)}
                        className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold uppercase tracking-widest transition-all border ${executiveMode ? 'bg-neon-purple/20 text-neon-purple border-neon-purple/50 shadow-[0_0_15px_rgba(168,85,247,0.4)]' : 'bg-white/5 text-gray-400 border-white/10 hover:bg-white/10 hover:text-white'}`}
                    >
                        <Award size={16} />
                        {executiveMode ? 'Executive Mode Active' : 'Enter Executive Mode'}
                    </button>
                </div>
            </motion.div>

            {/* Tabs */}
            <div className="flex flex-wrap gap-2 p-1 bg-white/5 rounded-2xl border border-white/10 w-fit">
                {tabs.map(t => <TabButton key={t.id} {...t} active={activeTab} onClick={setActiveTab} />)}
            </div>

            {/* Tab Content */}
            <motion.div
                key={activeTab}
                initial={{ opacity: 0, x: 10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.2 }}
                className="glass-card p-6 min-h-[400px]"
            >
                {activeTab === 'analytics' && <AnalyticsTab />}
                {activeTab === 'users' && <UserTab />}
                {activeTab === 'depts' && <DepartmentTab />}
                {activeTab === 'policy' && <PolicyTab />}
                {activeTab === 'configs' && <ConfigTab />}
                {activeTab === 'recommendations' && <RecommendationTab />}
                {activeTab === 'security' && <SecurityTab />}
                {activeTab === 'logs' && <LogsTab />}
            </motion.div>
        </div>
    );
};

export default Admin;
