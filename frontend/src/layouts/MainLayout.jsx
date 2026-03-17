import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import { motion } from 'framer-motion';

const MainLayout = () => {
    return (
        <div className="min-h-screen bg-dark-base relative text-white font-sans overflow-x-hidden">
            {/* Background Mesh Effect */}
            <div className="fixed inset-0 bg-mesh-gradient opacity-60 pointer-events-none z-0"></div>

            <Sidebar />

            <div className="pl-64 relative z-10">
                <motion.main
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5 }}
                    className="p-8 max-w-7xl mx-auto"
                >
                    <Outlet />
                </motion.main>
            </div>
        </div>
    );
};

export default MainLayout;
