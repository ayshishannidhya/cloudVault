import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auditAPI } from '../services/api';
import Navbar from '../components/Navbar';

const ACTION_CONFIG = {
  USER_REGISTER: { icon: '👤', label: 'Register', color: '#4ade80' },
  USER_LOGIN:    { icon: '🔑', label: 'Login',    color: '#4ade80' },
  FILE_UPLOAD:   { icon: '📤', label: 'Upload',   color: '#4ade80' },
  FILE_DOWNLOAD: { icon: '📥', label: 'Download', color: '#60a5fa' },
  FILE_DELETE:   { icon: '🗑️', label: 'Delete',   color: '#f87171' },
  FILE_SHARE:    { icon: '🔗', label: 'Share',    color: '#c084fc' },
  FILE_REVOKE:   { icon: '🚫', label: 'Revoke',   color: '#fb923c' },
  ACCESS_DENIED: { icon: '⛔', label: 'Denied',   color: '#f87171' },
};

function formatTimestamp(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return dateStr;
  return date.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: true,
  });
}

export default function AuditLogPage() {
  const navigate = useNavigate();
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');

  useEffect(() => {
    loadLogs();
  }, []);

  const loadLogs = async () => {
    try {
      setLoading(true);
      const res = await auditAPI.getMyLogs();
      // Sort newest first
      const sorted = (res.data || []).sort(
        (a, b) => new Date(b.timestamp) - new Date(a.timestamp)
      );
      setLogs(sorted);
    } catch (error) {
      console.error('Failed to load audit logs:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredLogs = filter === 'ALL'
    ? logs
    : logs.filter((log) => log.action === filter);

  const actionCounts = logs.reduce((acc, log) => {
    acc[log.action] = (acc[log.action] || 0) + 1;
    return acc;
  }, {});

  return (
    <div className="dashboard">
      <Navbar />

      <main className="dashboard-main">
        {/* Header */}
        <div className="audit-header">
          <div>
            <button className="audit-back-btn" onClick={() => navigate('/')}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="15,18 9,12 15,6" />
              </svg>
              Dashboard
            </button>
            <h2>Audit Log</h2>
            <p className="dashboard-subtitle">Security activity trail for your account</p>
          </div>
          <div className="dashboard-stats">
            <div className="stat-card">
              <span className="stat-number">{logs.length}</span>
              <span className="stat-label">Total Events</span>
            </div>
          </div>
        </div>

        {/* Filter Tabs */}
        <div className="audit-filters">
          <button
            className={`audit-filter-chip ${filter === 'ALL' ? 'active' : ''}`}
            onClick={() => setFilter('ALL')}
          >
            All ({logs.length})
          </button>
          {Object.entries(ACTION_CONFIG).map(([key, cfg]) =>
            actionCounts[key] ? (
              <button
                key={key}
                className={`audit-filter-chip ${filter === key ? 'active' : ''}`}
                onClick={() => setFilter(key)}
                style={filter === key ? { borderColor: cfg.color, color: cfg.color } : {}}
              >
                {cfg.icon} {cfg.label} ({actionCounts[key]})
              </button>
            ) : null
          )}
        </div>

        {/* Log List */}
        {loading ? (
          <div className="loading-container">
            <div className="loading-spinner"></div>
            <p>Loading audit logs...</p>
          </div>
        ) : filteredLogs.length === 0 ? (
          <div className="audit-empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
              <polyline points="14,2 14,8 20,8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
            </svg>
            <p>No audit events found</p>
          </div>
        ) : (
          <div className="audit-list">
            {filteredLogs.map((log) => {
              const cfg = ACTION_CONFIG[log.action] || { icon: '📋', label: log.action, color: '#a0a0a0' };
              return (
                <div className="audit-item" key={log.id}>
                  <div className="audit-icon" style={{ color: cfg.color }}>
                    {cfg.icon}
                  </div>
                  <div className="audit-info">
                    <div className="audit-summary">
                      <span className="audit-user">{log.username}</span>
                      <span className="audit-dot">·</span>
                      <span className="audit-action-label" style={{ color: cfg.color }}>
                        {cfg.label}
                      </span>
                      {log.filename && (
                        <>
                          <span className="audit-dot">·</span>
                          <span className="audit-filename">{log.filename}</span>
                        </>
                      )}
                      {log.targetUsername && (
                        <>
                          <span className="audit-dot">·</span>
                          <span className="audit-target">→ {log.targetUsername}</span>
                        </>
                      )}
                    </div>
                    <div className="audit-meta">
                      <span className="audit-time">{formatTimestamp(log.timestamp)}</span>
                      {log.ipAddress && (
                        <>
                          <span className="audit-dot">·</span>
                          <span className="audit-ip">{log.ipAddress}</span>
                        </>
                      )}
                      {!log.success && (
                        <span className="audit-failed">FAILED</span>
                      )}
                    </div>
                    {log.details && (
                      <div className="audit-details">{log.details}</div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}

