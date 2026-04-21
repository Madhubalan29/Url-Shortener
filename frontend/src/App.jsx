import React, { useState, useEffect, useCallback } from 'react';
import Header from './components/Header';
import StatsCards from './components/StatsCards';
import ShortenForm from './components/ShortenForm';
import UrlTable from './components/UrlTable';
import AnalyticsChart from './components/AnalyticsChart';
import { createApiKey, listUrls } from './services/api';

const API_KEY_STORAGE = 'snip_api_key';

export default function App() {
  const [apiKey, setApiKey] = useState(() => localStorage.getItem(API_KEY_STORAGE) || '');
  const [keyName, setKeyName] = useState('My App');
  const [newKeyData, setNewKeyData] = useState(null);
  const [urls, setUrls] = useState([]);
  const [selectedUrl, setSelectedUrl] = useState(null);
  const [toasts, setToasts] = useState([]);
  const [setupLoading, setSetupLoading] = useState(false);

  // ─── Toast System ──────────────────────────────────
  const addToast = useCallback((message, type = 'success') => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3000);
  }, []);

  // ─── Load URLs ─────────────────────────────────────
  const loadUrls = useCallback(async () => {
    if (!apiKey) return;
    try {
      const data = await listUrls(apiKey);
      setUrls(data.content || []);
    } catch (err) {
      if (err.response?.status === 401) {
        // API key is invalid — clear it
        localStorage.removeItem(API_KEY_STORAGE);
        setApiKey('');
        addToast('API key is invalid or expired. Please create a new one.', 'error');
      }
    }
  }, [apiKey, addToast]);

  useEffect(() => {
    loadUrls();
  }, [loadUrls]);

  // ─── API Key Setup ─────────────────────────────────
  const handleCreateKey = async () => {
    setSetupLoading(true);
    try {
      const data = await createApiKey(keyName || 'My App');
      setNewKeyData(data);
      setApiKey(data.apiKey);
      localStorage.setItem(API_KEY_STORAGE, data.apiKey);
      addToast('API key created successfully!', 'success');
    } catch (err) {
      addToast('Failed to create API key. Is the backend running?', 'error');
    } finally {
      setSetupLoading(false);
    }
  };

  const handleUseExistingKey = () => {
    const key = prompt('Enter your existing API key:');
    if (key && key.trim()) {
      setApiKey(key.trim());
      localStorage.setItem(API_KEY_STORAGE, key.trim());
      addToast('API key set!', 'success');
    }
  };

  // ─── Compute Stats ─────────────────────────────────
  const totalClicks = urls.reduce((sum, url) => sum + (url.clickCount || 0), 0);

  // ─── Render: Setup Screen ─────────────────────────
  if (!apiKey) {
    return (
      <div className="app">
        <Header />
        <div className="setup-section">
          <div className="setup-card">
            <h2>Welcome to Snip ✂️</h2>
            <p>Create an API key to start shortening URLs</p>
            <div className="input-group" style={{ marginBottom: '16px' }}>
              <label htmlFor="key-name">Key Name</label>
              <input
                id="key-name"
                type="text"
                placeholder="My App"
                value={keyName}
                onChange={(e) => setKeyName(e.target.value)}
              />
            </div>
            <button
              className="btn btn-primary"
              onClick={handleCreateKey}
              disabled={setupLoading}
              style={{ width: '100%', marginBottom: '12px' }}
            >
              {setupLoading ? <span className="spinner"></span> : '🔑'}
              {setupLoading ? 'Creating...' : 'Generate API Key'}
            </button>
            <button
              className="btn btn-ghost"
              onClick={handleUseExistingKey}
              style={{ width: '100%' }}
            >
              I have an existing key
            </button>

            {newKeyData && (
              <div className="api-key-display">
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>Your API Key:</div>
                <code>{newKeyData.apiKey}</code>
                <div className="warning">⚠️ Save this key — it won't be shown again!</div>
              </div>
            )}
          </div>
        </div>

        {/* Toasts */}
        <div className="toast-container">
          {toasts.map((t) => (
            <div key={t.id} className={`toast toast-${t.type}`}>{t.message}</div>
          ))}
        </div>
      </div>
    );
  }

  // ─── Render: Main Dashboard ────────────────────────
  return (
    <div className="app">
      <Header />
      <div className="app-content">
        <StatsCards urls={urls} totalClicks={totalClicks} />
        <ShortenForm apiKey={apiKey} onUrlCreated={loadUrls} onToast={addToast} />

        {selectedUrl ? (
          <AnalyticsChart
            shortCode={selectedUrl}
            apiKey={apiKey}
            onBack={() => setSelectedUrl(null)}
          />
        ) : (
          <UrlTable
            urls={urls}
            apiKey={apiKey}
            onRefresh={loadUrls}
            onSelectUrl={setSelectedUrl}
            onToast={addToast}
          />
        )}
      </div>

      {/* Toasts */}
      <div className="toast-container">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.type}`}>{t.message}</div>
        ))}
      </div>
    </div>
  );
}
