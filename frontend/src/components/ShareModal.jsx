import { useState, useEffect } from 'react';
import { shareAPI } from '../services/api';

export default function ShareModal({ file, onClose }) {
  const [recipientUsername, setRecipientUsername] = useState('');
  const [sharing, setSharing] = useState(false);
  const [message, setMessage] = useState(null);
  const [sharedUsers, setSharedUsers] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(true);

  useEffect(() => {
    loadSharedUsers();
  }, [file.fileId]);

  const loadSharedUsers = async () => {
    try {
      setLoadingUsers(true);
      const response = await shareAPI.getSharedUsers(file.fileId);
      setSharedUsers(response.data);
    } catch (error) {
      console.error('Failed to load shared users:', error);
    } finally {
      setLoadingUsers(false);
    }
  };

  const handleShare = async (e) => {
    e.preventDefault();
    if (!recipientUsername.trim()) return;

    setSharing(true);
    setMessage(null);

    try {
      await shareAPI.shareFile(file.fileId, recipientUsername.trim());
      setMessage({ type: 'success', text: `Shared with ${recipientUsername}!` });
      setRecipientUsername('');
      loadSharedUsers();
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.error || 'Failed to share file',
      });
    } finally {
      setSharing(false);
    }
  };

  const handleRevoke = async (username) => {
    try {
      await shareAPI.revokeAccess(file.fileId, username);
      setMessage({ type: 'success', text: `Access revoked for ${username}` });
      loadSharedUsers();
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.error || 'Failed to revoke access',
      });
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="18" cy="5" r="3" />
              <circle cx="6" cy="12" r="3" />
              <circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
            Share File
          </h3>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>

        <div className="modal-file-info">
          <span className="modal-filename">{file.originalFilename}</span>
        </div>

        <form onSubmit={handleShare} className="share-form">
          <div className="share-input-group">
            <input
              type="text"
              placeholder="Enter recipient's username"
              value={recipientUsername}
              onChange={(e) => setRecipientUsername(e.target.value)}
              disabled={sharing}
              className="share-input"
            />
            <button type="submit" className="btn-primary" disabled={sharing || !recipientUsername.trim()}>
              {sharing ? (
                <div className="btn-spinner"></div>
              ) : (
                'Share'
              )}
            </button>
          </div>
        </form>

        <div className="share-info-badge">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0110 0v4" />
          </svg>
          Server handles encryption — zero effort for you
        </div>

        {message && (
          <div className={`share-message ${message.type}`}>
            {message.text}
          </div>
        )}

        <div className="shared-users-section">
          <h4>Shared With</h4>
          {loadingUsers ? (
            <p className="loading-text">Loading...</p>
          ) : sharedUsers.length === 0 ? (
            <p className="empty-text">Not shared with anyone yet</p>
          ) : (
            <ul className="shared-users-list">
              {sharedUsers.map((user) => (
                <li key={user.userId} className="shared-user-item">
                  <div className="shared-user-info">
                    <div className="shared-user-avatar">
                      {user.username.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <span className="shared-user-name">{user.username}</span>
                      <span className="shared-user-email">{user.email}</span>
                    </div>
                  </div>
                  <button
                    className="btn-revoke"
                    onClick={() => handleRevoke(user.username)}
                    title="Revoke access"
                  >
                    Revoke
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
