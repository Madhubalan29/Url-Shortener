import React, { useState } from 'react';
import { shortenUrl, formatShortUrl } from '../services/api';

export default function ShortenForm({ apiKey, onUrlCreated, onToast }) {
  const [longUrl, setLongUrl] = useState('');
  const [customAlias, setCustomAlias] = useState('');
  const [ttlDays, setTtlDays] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!longUrl.trim()) return;

    setLoading(true);
    setResult(null);

    try {
      const data = await shortenUrl(apiKey, longUrl.trim(), customAlias.trim() || null, ttlDays || null);
      setResult(data);
      setLongUrl('');
      setCustomAlias('');
      setTtlDays('');
      onUrlCreated?.();
      onToast?.('URL shortened successfully!', 'success');
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to shorten URL';
      onToast?.(msg, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = async () => {
    const displayUrl = formatShortUrl(result?.shortUrl);
    if (!displayUrl) return;
    try {
      await navigator.clipboard.writeText(displayUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      onToast?.('Failed to copy', 'error');
    }
  };

  return (
    <div className="shorten-section">
      <div className="section-title">✨ Shorten a URL</div>
      <form className="shorten-form" onSubmit={handleSubmit}>
        <div className="form-row">
          <div className="input-group" style={{ flex: 3 }}>
            <label htmlFor="long-url">Destination URL</label>
            <input
              id="long-url"
              type="url"
              placeholder="https://example.com/your-very-long-url-here"
              value={longUrl}
              onChange={(e) => setLongUrl(e.target.value)}
              required
            />
          </div>
          <div className="input-group" style={{ flex: 1 }}>
            <label htmlFor="custom-alias">Custom Alias (optional)</label>
            <input
              id="custom-alias"
              type="text"
              placeholder="my-link"
              value={customAlias}
              onChange={(e) => setCustomAlias(e.target.value)}
              pattern="[a-zA-Z0-9\-_]+"
              title="Only letters, numbers, hyphens, and underscores"
            />
          </div>
          <div className="input-group" style={{ flex: 0.6 }}>
            <label htmlFor="ttl-days">TTL (days)</label>
            <input
              id="ttl-days"
              type="number"
              placeholder="365"
              value={ttlDays}
              onChange={(e) => setTtlDays(e.target.value)}
              min="1"
              max="3650"
            />
          </div>
        </div>
        <div className="form-row" style={{ justifyContent: 'flex-end' }}>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || !longUrl.trim()}
          >
            {loading ? <span className="spinner"></span> : '⚡'}
            {loading ? 'Shortening...' : 'Shorten URL'}
          </button>
        </div>

        {result && (
          <div className="result-card">
            <div className="result-label">Your shortened URL</div>
            <div className="result-url">
              <a href={formatShortUrl(result.shortUrl)} target="_blank" rel="noopener noreferrer">
                {formatShortUrl(result.shortUrl)}
              </a>
              <button
                type="button"
                className={`copy-btn ${copied ? 'copied' : ''}`}
                onClick={handleCopy}
              >
                {copied ? '✓ Copied!' : '📋 Copy'}
              </button>
            </div>
          </div>
        )}
      </form>
    </div>
  );
}
