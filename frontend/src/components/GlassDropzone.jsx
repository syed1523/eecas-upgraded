import React, { useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { motion, AnimatePresence } from 'framer-motion';
import { UploadCloud, File, X } from 'lucide-react';

const GlassDropzone = ({ onFileChange, file }) => {
    const onDrop = useCallback(acceptedFiles => {
        if (acceptedFiles?.length > 0) {
            onFileChange({ target: { files: acceptedFiles } });
        }
    }, [onFileChange]);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        accept: {
            'image/*': ['.jpeg', '.jpg', '.png'],
            'application/pdf': ['.pdf']
        },
        maxFiles: 1
    });

    const removeFile = (e) => {
        e.stopPropagation();
        onFileChange({ target: { files: [] } });
    };

    return (
        <div className="w-full">
            <div
                {...getRootProps()}
                className={`
                    relative group cursor-pointer transition-all duration-300
                    h-40 rounded-2xl border-2 border-dashed
                    flex items-center justify-center
                    overflow-hidden
                    ${isDragActive
                        ? 'border-neon-blue bg-neon-blue/10'
                        : 'border-white/20 hover:border-white/40 hover:bg-white/5'}
                `}
            >
                <input {...getInputProps()} />

                <AnimatePresence mode="wait">
                    {file ? (
                        <motion.div
                            key="file-preview"
                            initial={{ opacity: 0, scale: 0.9 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0.9 }}
                            className="flex flex-col items-center z-10 p-4 w-full h-full justify-center bg-dark-surface/50 backdrop-blur-md"
                        >
                            <File size={48} className="text-neon-blue mb-2" />
                            <p className="text-white font-medium text-sm truncate max-w-[80%]">{file.name}</p>
                            <p className="text-gray-400 text-xs">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                            <button
                                onClick={removeFile}
                                className="mt-3 p-1 rounded-full bg-red-500/20 text-red-400 hover:bg-red-500 hover:text-white transition-colors"
                            >
                                <X size={16} />
                            </button>
                        </motion.div>
                    ) : (
                        <motion.div
                            key="upload-prompt"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            className="text-center p-6 z-10"
                        >
                            <div className={`
                                w-12 h-12 rounded-full bg-white/5 flex items-center justify-center mx-auto mb-3
                                transition-transform duration-300 ${isDragActive ? 'scale-110 text-neon-blue' : 'text-gray-400'}
                            `}>
                                <UploadCloud size={24} />
                            </div>
                            <p className="text-gray-300 font-medium">
                                {isDragActive ? "Drop verification file here" : "Upload Receipt"}
                            </p>
                            <p className="text-gray-500 text-xs mt-1">
                                Drag & drop or click to browse (PDF, PNG, JPG)
                            </p>
                        </motion.div>
                    )}
                </AnimatePresence>

                {/* Background Glow Effect */}
                <div className={`absolute inset-0 transition-opacity duration-300 ${isDragActive ? 'opacity-100' : 'opacity-0'}`}>
                    <div className="absolute inset-0 bg-neon-blue/5 blur-xl"></div>
                </div>
            </div>
        </div>
    );
};

export default GlassDropzone;
