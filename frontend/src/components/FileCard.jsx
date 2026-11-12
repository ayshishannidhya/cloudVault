import { formatFileSize } from '../crypto/cryptoUtils';

export default function FileCard({ file, onDownload, onShare, onDelete, downloading }) {
  const getFileIcon = (contentType) => {
    if (!contentType) return '📄';
    if (contentType.startsWith('image/')) return '🖼️';
    if (contentType.startsWith('video/')) return '🎬';
    if (contentType.startsWith('audio/')) return '🎵';
    if (contentType.includes('pdf')) return '📕';
    if (contentType.includes('word') || contentType.includes('document')) return '📝';
    if (contentType.includes('sheet') || contentType.includes('excel')) return '📊';
    if (contentType.includes('presentation') || contentType.includes('powerpoint')) return '📎';
    if (contentType.includes('zip') || contentType.includes('compressed')) return '🗜️';
    if (contentType.includes('text')) return '📃';
    return '📄';
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    return date.toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  };

  const isDownloading = downloading === file.fileId;

  return (
    <div className="file-card">
      <div className="file-card-icon">{getFileIcon(file.contentType)}</div>
      <div className="file-card-info">
        <h4 className="file-card-name" title={file.originalFilename}>
          {file.originalFilename}
        </h4>
        <div className="file-card-meta">
          <span className="file-size">{formatFileSize(file.fileSize)}</span>
          <span className="file-dot">·</span>
          <span className="file-date">{formatDate(file.uploadedAt)}</span>
        </div>
        {!file.owner && (
          <div className="file-shared-by">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            Shared by {file.ownerUsername}
          </div>
        )}
      </div>
      <div className="file-card-actions">
        <button
          className="btn-icon btn-download"
          onClick={() => onDownload(file)}
          disabled={isDownloading}
          title="Download & Decrypt"
        >
          {isDownloading ? (
            <div className="btn-spinner"></div>
          ) : (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
              <polyline points="7,10 12,15 17,10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
          )}
        </button>
        {file.owner && (
          <>
            <button className="btn-icon btn-share" onClick={() => onShare(file)} title="Share File">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="18" cy="5" r="3" />
                <circle cx="6" cy="12" r="3" />
                <circle cx="18" cy="19" r="3" />
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
              </svg>
            </button>
            <button className="btn-icon btn-delete" onClick={() => onDelete(file)} title="Delete File">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="3,6 5,6 21,6" />
                <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
              </svg>
            </button>
          </>
        )}
      </div>
      <div className="file-card-badge">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
          <path d="M7 11V7a5 5 0 0110 0v4" />
        </svg>
        AES-256 Encrypted
      </div>
    </div>
  );
}
