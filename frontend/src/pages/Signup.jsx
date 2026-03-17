import React, { useState } from 'react';
import api from '../services/api';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';

const Signup = () => {
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        role: 'user',
        departmentId: '1'
    });
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage('');
        setError('');

        try {
            await api.post('/auth/signup', {
                ...formData,
                role: [formData.role]
            });
            setMessage('Identity Verified. Redirecting...');
            setTimeout(() => navigate('/login'), 2000);
        } catch (err) {
            setError(err.response?.data?.message || 'Registration failed');
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-dark-base relative overflow-hidden">
            {/* Background Mesh Effect */}
            <div className="absolute inset-0 bg-mesh-gradient opacity-60 pointer-events-none"></div>

            <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.5 }}
                className="glass-card p-10 w-full max-w-md relative z-10"
            >
                <h2 className="text-3xl font-bold mb-6 text-center bg-clip-text text-transparent bg-primary-gradient uppercase tracking-widest">New Identity</h2>

                {message && <div className="bg-green-500/10 border border-green-500/50 text-green-400 p-3 rounded mb-4 text-center text-sm">{message}</div>}
                {error && <div className="bg-red-500/10 border border-red-500/50 text-red-400 p-3 rounded mb-4 text-center text-sm">{error}</div>}

                <form onSubmit={handleSubmit} className="space-y-5">
                    <div>
                        <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Username</label>
                        <input name="username" required minLength="3" maxLength="20" className="w-full glass-input neon-focus" placeholder="Create your ID" onChange={handleChange} />
                    </div>
                    <div>
                        <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Email</label>
                        <input name="email" type="email" required maxLength="50" className="w-full glass-input neon-focus" placeholder="contact@example.com" onChange={handleChange} />
                    </div>
                    <div>
                        <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Password</label>
                        <input name="password" type="password" required minLength="6" maxLength="40" className="w-full glass-input neon-focus" placeholder="••••••••" onChange={handleChange} />
                    </div>
                    <div>
                        <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Clearance Level</label>
                        <select name="role" className="w-full glass-input neon-focus bg-dark-surface/80" onChange={handleChange} value={formData.role}>
                            <option value="user" className="bg-dark-surface p-2">Employee</option>
                            <option value="manager" className="bg-dark-surface p-2">Manager</option>
                            <option value="finance" className="bg-dark-surface p-2">Finance</option>
                            <option value="auditor" className="bg-dark-surface p-2">Auditor</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-gray-400 text-xs uppercase tracking-wider mb-2">Assigned Department</label>
                        <select name="departmentId" className="w-full glass-input neon-focus bg-dark-surface/80" onChange={handleChange} value={formData.departmentId}>
                            <option value="1" className="bg-dark-surface p-2">Engineering</option>
                            <option value="2" className="bg-dark-surface p-2">Finance</option>
                            <option value="3" className="bg-dark-surface p-2">Compliance</option>
                            <option value="4" className="bg-dark-surface p-2">IT</option>
                            <option value="5" className="bg-dark-surface p-2">Sales</option>
                            <option value="6" className="bg-dark-surface p-2">Marketing</option>
                            <option value="7" className="bg-dark-surface p-2">HR</option>
                            <option value="8" className="bg-dark-surface p-2">UNASSIGNED</option>
                        </select>
                    </div>
                    <motion.button
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        type="submit"
                        className="w-full bg-primary-gradient text-white font-bold py-3 rounded-xl shadow-neon mt-4"
                    >
                        ESTABLISH CONNECTION
                    </motion.button>
                </form>
                <div className="mt-6 text-center">
                    <span className="text-gray-500 text-sm">Already established? </span>
                    <Link to="/login" className="text-neon-blue hover:text-neon-purple transition-colors text-sm font-medium">Access Terminal</Link>
                </div>
            </motion.div>
        </div>
    );
};

export default Signup;
