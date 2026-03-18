import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';

const Login = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await login(username, password);
            navigate('/');
        } catch (err) {
            setError('Invalid credentials');
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-dark-base relative overflow-hidden">
            {/* Background Mesh Effect */}
            <div className="absolute inset-0 bg-mesh-gradient opacity-60 pointer-events-none"></div>

            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="glass-card p-10 w-full max-w-md relative z-10"
            >
                <div className="text-center mb-8">
                    <h1 className="text-3xl font-bold bg-clip-text text-transparent bg-primary-gradient mb-2 tracker-wider uppercase">EECAS Enterprise</h1>
                    <p className="text-gray-400 text-sm tracking-widest">EXPENSE & COMPLIANCE SYSTEM</p>
                </div>

                {error && <div className="bg-red-500/10 border border-red-500/50 text-red-400 p-3 rounded mb-6 text-sm text-center">{error}</div>}

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Username</label>
                        <input
                            type="text"
                            className="w-full glass-input neon-focus"
                            placeholder="Enter your ID"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                        />
                    </div>
                    <div>
                        <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Password</label>
                        <input
                            type="password"
                            className="w-full glass-input neon-focus"
                            placeholder="Enter your security code"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>
                    <motion.button
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        type="submit"
                        className="w-full bg-primary-gradient text-white font-bold py-3 rounded-xl shadow-neon transition-all mt-4"
                    >
                        INITIALIZE SESSION
                    </motion.button>
                </form>

                <div className="mt-8 text-center">
                    <span className="text-gray-500 text-sm">New user? </span>
                    <Link to="/signup" className="text-neon-blue hover:text-neon-purple transition-colors text-sm font-medium">Create Identity</Link>
                </div>
            </motion.div>

            {/* Demo Credentials Panel */}
            <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.6 }}
                className="hidden lg:flex flex-col absolute right-6 top-4 bottom-4 w-72 z-10"
            >
                <div className="glass-card p-4 border-l-4 border-l-neon-blue flex flex-col min-h-0 overflow-hidden">
                    <h3 className="text-neon-blue font-bold text-xs mb-3 uppercase tracking-widest flex items-center gap-2 flex-shrink-0">
                        <span className="w-2 h-2 bg-neon-blue rounded-full animate-pulse"></span>
                        Access Matrix
                    </h3>
                    <div className="overflow-y-auto flex-1 pr-1 custom-scrollbar space-y-2">
                        {/* Engineering */}
                        <div>
                            <p className="text-gray-500 text-[9px] uppercase font-bold mb-1 tracking-wider">Engineering</p>
                            <div className="grid grid-cols-2 gap-1">
                                <button onClick={() => { setUsername('employee1@test.com'); setPassword('password'); }} className="text-left text-[10px] text-gray-300 hover:text-white transition-colors py-1 px-2 hover:bg-white/5 rounded truncate">👷 employee1</button>
                                <button onClick={() => { setUsername('manager1@test.com'); setPassword('password'); }} className="text-left text-[10px] text-gray-300 hover:text-white transition-colors py-1 px-2 hover:bg-white/5 rounded truncate">👨‍💼 manager1</button>
                            </div>
                        </div>
                        {/* Marketing */}
                        <div>
                            <p className="text-gray-500 text-[9px] uppercase font-bold mb-1 tracking-wider">Marketing</p>
                            <div className="grid grid-cols-2 gap-1">
                                <button onClick={() => { setUsername('employee4@test.com'); setPassword('password'); }} className="text-left text-[10px] text-gray-300 hover:text-white transition-colors py-1 px-2 hover:bg-white/5 rounded truncate">📣 employee4</button>
                                <button onClick={() => { setUsername('manager2@test.com'); setPassword('password'); }} className="text-left text-[10px] text-gray-300 hover:text-white transition-colors py-1 px-2 hover:bg-white/5 rounded truncate">🚀 manager2</button>
                            </div>
                        </div>
                        {/* HR */}
                        <div>
                            <p className="text-gray-500 text-[9px] uppercase font-bold mb-1 tracking-wider">HR</p>
                            <div className="grid grid-cols-2 gap-1">
                                <button onClick={() => { setUsername('employee6@test.com'); setPassword('password'); }} className="text-left text-[10px] text-gray-300 hover:text-white transition-colors py-1 px-2 hover:bg-white/5 rounded truncate">🤝 employee6</button>
                                <button onClick={() => { setUsername('manager3@test.com'); setPassword('password'); }} className="text-left text-[10px] text-gray-300 hover:text-white transition-colors py-1 px-2 hover:bg-white/5 rounded truncate">👑 manager3</button>
                            </div>
                        </div>
                        {/* Finance & Audit */}
                        <div>
                            <p className="text-gray-500 text-[9px] uppercase font-bold mb-1 tracking-wider">Finance & Audit</p>
                            <div className="grid grid-cols-2 gap-1">
                                <button onClick={() => { setUsername('finance1@test.com'); setPassword('password'); }} className="text-left text-[10px] text-gray-300 hover:text-white transition-colors py-1 px-2 hover:bg-white/5 rounded truncate">💰 finance1</button>
                                <button onClick={() => { setUsername('finance2@test.com'); setPassword('password'); }} className="text-left text-[10px] text-gray-300 hover:text-white transition-colors py-1 px-2 hover:bg-white/5 rounded truncate">💳 finance2</button>
                                <button onClick={() => { setUsername('auditor1@test.com'); setPassword('password'); }} className="text-left text-[10px] text-gray-300 hover:text-white transition-colors py-1 px-2 hover:bg-white/5 rounded truncate">🔍 auditor1</button>
                            </div>
                        </div>
                        {/* Root Access */}
                        <div>
                            <p className="text-gray-500 text-[9px] uppercase font-bold mb-1 tracking-wider">Root Access</p>
                            <button onClick={() => { setUsername('admin@test.com'); setPassword('password'); }} className="w-full text-left text-[10px] text-white bg-primary-gradient py-1.5 px-2 rounded-lg font-bold shadow-neon-small">
                                🔑 admin@test.com (Admin)
                            </button>
                        </div>
                        {/* Tip */}
                        <p className="text-gray-600 text-[9px] text-center pt-1 pb-1">↑ click any account to auto-fill · password: <span className="text-gray-500">password</span></p>
                    </div>
                </div>
            </motion.div>

        </div>
    );
};

export default Login;
