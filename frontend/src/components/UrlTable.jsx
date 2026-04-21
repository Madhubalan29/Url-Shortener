import React from 'react';
import { deleteUrl } from '../services/api';

export default function UrlTable({ urls, apiKey, onRefresh, onSelectUrl, onToast }) {
  const formatDate = (isoString) => {
    if (!isoString) return '—';
    const d = new Date(isoString);
    return d.toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  };

  const handleDelete = async (shortCode) => {
    if (!confirm(`Delete short URL "/${shortCode}"?`)) return;
    try {
      await deleteUrl(apiKey, shortCode);
      onRefresh?.();
      onToast?.('URL deleted', 'success');
    } catch {
      onToast?.('Failed to delete URL', 'error');
    }
  };

  if (!urls || urls.length === 0) {
    return (
      <div className="urls-section">
        <div className="section-header">
          <div className="section-title">📋 Your URLs</div>
        </div>
        <div className="table-container">
          <div className="empty-state">
            <div className="empty-state-icon">🔗</div>
            <div className="empty-state-text">No URLs yet. Shorten your first URL above!</div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="urls-section">
      <div className="section-header">
        <div className="section-title">📋 Your URLs ({urls.length})</div>
        <button className="btn btn-ghost btn-sm" onClick={onRefresh}>
          🔄 Refresh
        </button>
      </div>
      <div className="table-container">
        <table className="url-table">
          <thead>
            <tr>
              <th>Short Code</th>
              <th>Destination</th>
              <th>Clicks</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {urls.map((url) => (
              <tr key={url.shortCode}>
                <td>
                  <a
                    className="short-code-link"
                    href={url.shortUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    /{url.shortCode}
                  </a>
                </td>
                <td>
                  <div className="long-url-cell" title={url.longUrl}>
                    {url.longUrl}
                  </div>
                </td>
                <td>
                  <span className="click-count">
                    📊 {url.clickCount || 0}
                  </span>
                </td>
                <td>
                  <span className="date-cell">{formatDate(url.createdAt)}</span>
                </td>
                <td>
                  <div className="actions-cell">
                    <button
                      className="btn btn-ghost btn-sm"
                      onClick={() => onSelectUrl?.(url.shortCode)}
                      title="View analytics"
                    >
                      📈
                    </button>
                    <button
                      className="btn btn-danger btn-sm"
                      onClick={() => handleDelete(url.shortCode)}
                      title="Delete"
                    >
                      🗑️
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
