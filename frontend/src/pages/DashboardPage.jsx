import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { fileAPI } from '../services/api';
import { decryptFileComplete } from '../crypto/cryptoUtils';
import Navbar from '../components/Navbar';
import FileUpload from '../components/FileUpload';
import FileList from '../components/FileList';
import ShareModal from '../components/ShareModal';

export default function DashboardPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('my-files');
  const [myFiles, setMyFiles] = useState([]);
  const [sharedFiles, setSharedFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(null);
  const [shareFile, setShareFile] = useState(null);

  const loadFiles = useCallback(async () => {
    try {
      setLoading(true);
      const [myRes, sharedRes] = await Promise.all([
        fileAPI.getMyFiles(),
        fileAPI.getSharedWithMe(),
      ]);
      setMyFiles(myRes.data);
      setSharedFiles(sharedRes.data);
    } catch (error) {
      console.error('Failed to load files:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadFiles();
  }, [loadFiles]);

  const handleDownload = async (file) => {
    try {
      setDownloading(file.fileId);

      // Step 1: Download encrypted file from server
      const response = await fileAPI.download(file.fileId);

      // Get encrypted AES key and IV from response headers
      const encryptedAesKey = response.headers['x-encrypted-aes-key'];
      const iv = response.headers['x-iv'];

      if (!encryptedAesKey || !iv) {
        throw new Error('Missing encryption headers from server');
      }

      // Step 2: Decrypt file using Web Crypto API (client-side)
      const decryptedData = await decryptFileComplete(response.data, encryptedAesKey, iv);

      // Step 3: Trigger download of decrypted file
      const blob = new Blob([decryptedData]);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = file.originalFilename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Download/Decrypt failed:', error);
      alert('Failed to download file: ' + (error.message || 'Unknown error'));
    } finally {
      setDownloading(null);
    }
  };

  const handleDelete = async (file) => {
    if (!confirm(`Delete "${file.originalFilename}"? This cannot be undone.`)) return;

    try {
      await fileAPI.deleteFile(file.fileId);
      loadFiles();
    } catch (error) {
      alert('Failed to delete file: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleUploadSuccess = () => {
    loadFiles();
  };

  const currentFiles = activeTab === 'my-files' ? myFiles : sharedFiles;

  return (
    <div className="dashboard">
      <Navbar />

      <main className="dashboard-main">
        <div className="dashboard-header">
          <div>
            <h2>Welcome back, {user?.username}!</h2>
            <p className="dashboard-subtitle">Your files are encrypted with AES-256-GCM + RSA-2048</p>
          </div>
          <div className="dashboard-stats">
            <div className="stat-card">
              <span className="stat-number">{myFiles.length}</span>
              <span className="stat-label">My Files</span>
            </div>
            <div className="stat-card">
              <span className="stat-number">{sharedFiles.length}</span>
              <span className="stat-label">Shared</span>
            </div>
          </div>
        </div>

        <FileUpload onUploadSuccess={handleUploadSuccess} />

        <div className="tabs">
          <button
            className={`tab ${activeTab === 'my-files' ? 'active' : ''}`}
            onClick={() => setActiveTab('my-files')}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z" />
            </svg>
            My Files ({myFiles.length})
          </button>
          <button
            className={`tab ${activeTab === 'shared' ? 'active' : ''}`}
            onClick={() => setActiveTab('shared')}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 00-3-3.87" />
              <path d="M16 3.13a4 4 0 010 7.75" />
            </svg>
            Shared With Me ({sharedFiles.length})
          </button>
        </div>

        {loading ? (
          <div className="loading-container">
            <div className="loading-spinner"></div>
            <p>Loading encrypted files...</p>
          </div>
        ) : (
          <FileList
            files={currentFiles}
            title={activeTab === 'my-files' ? 'My Encrypted Files' : 'Files Shared With Me'}
            emptyMessage={
              activeTab === 'my-files'
                ? 'No files yet. Upload your first encrypted file above!'
                : 'No files have been shared with you yet.'
            }
            onDownload={handleDownload}
            onShare={(file) => setShareFile(file)}
            onDelete={handleDelete}
            downloading={downloading}
          />
        )}
      </main>

      {shareFile && (
        <ShareModal
          file={shareFile}
          onClose={() => {
            setShareFile(null);
            loadFiles();
          }}
        />
      )}
    </div>
  );
}
