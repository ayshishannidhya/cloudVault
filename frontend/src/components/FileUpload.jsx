import { useState, useRef } from 'react';
import { fileAPI } from '../services/api';

export default function FileUpload({ onUploadSuccess }) {
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [message, setMessage] = useState(null);
  const fileInputRef = useRef(null);

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const files = e.dataTransfer.files;
    if (files.length > 0) {
      uploadFile(files[0]);
    }
  };

  const handleFileSelect = (e) => {
    const files = e.target.files;
    if (files.length > 0) {
      uploadFile(files[0]);
    }
  };

  const uploadFile = async (file) => {
    if (file.size > 50 * 1024 * 1024) {
      setMessage({ type: 'error', text: 'File size exceeds 50MB limit' });
      return;
    }

    setUploading(true);
    setProgress(0);
    setMessage(null);

    try {
      // Simulate progress for better UX
      const progressInterval = setInterval(() => {
        setProgress((prev) => Math.min(prev + 10, 90));
      }, 200);

      const response = await fileAPI.upload(file);

      clearInterval(progressInterval);
      setProgress(100);

      setMessage({
        type: 'success',
        text: `"${response.data.filename}" encrypted & uploaded successfully!`,
      });

      if (onUploadSuccess) {
        onUploadSuccess(response.data);
      }

      // Reset after delay
      setTimeout(() => {
        setProgress(0);
        setMessage(null);
        setUploading(false);
      }, 3000);
    } catch (error) {
      setProgress(0);
      setUploading(false);
      setMessage({
        type: 'error',
        text: error.response?.data?.error || 'Upload failed. Please try again.',
      });
    }
  };

  return (
    <div className="upload-section">
      <div
        className={`upload-dropzone ${isDragging ? 'dragging' : ''} ${uploading ? 'uploading' : ''}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => !uploading && fileInputRef.current?.click()}
      >
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileSelect}
          style={{ display: 'none' }}
        />

        {uploading ? (
          <div className="upload-progress">
            <div className="progress-ring">
              <svg viewBox="0 0 100 100">
                <circle className="progress-bg" cx="50" cy="50" r="42" />
                <circle
                  className="progress-fill"
                  cx="50" cy="50" r="42"
                  style={{ strokeDashoffset: 264 - (264 * progress) / 100 }}
                />
              </svg>
              <span className="progress-text">{progress}%</span>
            </div>
            <p className="upload-status">Encrypting & uploading...</p>
          </div>
        ) : (
          <div className="upload-prompt">
            <div className="upload-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
                <polyline points="17,8 12,3 7,8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
            </div>
            <p className="upload-title">Drop file here or click to browse</p>
            <p className="upload-subtitle">Files are encrypted with AES-256-GCM before upload</p>
            <p className="upload-limit">Max file size: 50MB</p>
          </div>
        )}
      </div>

      {message && (
        <div className={`upload-message ${message.type}`}>
          {message.type === 'success' ? (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
              <polyline points="22,4 12,14.01 9,11.01" />
            </svg>
          ) : (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="15" y1="9" x2="9" y2="15" />
              <line x1="9" y1="9" x2="15" y2="15" />
            </svg>
          )}
          <span>{message.text}</span>
        </div>
      )}
    </div>
  );
}
