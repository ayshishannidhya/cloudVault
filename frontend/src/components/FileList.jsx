import FileCard from './FileCard';

export default function FileList({ files, title, emptyMessage, onDownload, onShare, onDelete, downloading }) {
  return (
    <div className="file-list-section">
      <h3 className="file-list-title">{title}</h3>
      {files.length === 0 ? (
        <div className="file-list-empty">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1">
            <path d="M13 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V9z" />
            <polyline points="13,2 13,9 20,9" />
          </svg>
          <p>{emptyMessage}</p>
        </div>
      ) : (
        <div className="file-grid">
          {files.map((file) => (
            <FileCard
              key={file.fileId}
              file={file}
              onDownload={onDownload}
              onShare={onShare}
              onDelete={onDelete}
              downloading={downloading}
            />
          ))}
        </div>
      )}
    </div>
  );
}
