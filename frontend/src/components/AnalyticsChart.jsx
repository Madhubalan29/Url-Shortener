import React, { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell
} from 'recharts';
import { getAnalytics } from '../services/api';

const COLORS = ['#6c5ce7', '#00cec9', '#a29bfe', '#00b894', '#fdcb6e', '#ff6b6b', '#74b9ff'];

export default function AnalyticsChart({ shortCode, apiKey, onBack }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!shortCode) return;
    setLoading(true);
    getAnalytics(apiKey, shortCode)
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [shortCode, apiKey]);

  if (loading) {
    return (
      <div className="analytics-section">
        <div className="chart-card" style={{ textAlign: 'center', padding: '60px' }}>
          <div className="spinner" style={{ margin: '0 auto' }}></div>
          <div style={{ color: 'var(--text-muted)', marginTop: '12px' }}>Loading analytics...</div>
        </div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="analytics-section">
        <div className="chart-card" style={{ textAlign: 'center', padding: '60px' }}>
          <div style={{ color: 'var(--text-muted)' }}>No analytics data available</div>
        </div>
      </div>
    );
  }

  // Transform data for charts
  const clicksByDate = data.clicksByDate
    ? Object.entries(data.clicksByDate).map(([date, count]) => ({
        date: date.substring(5), // "MM-DD"
        clicks: count,
      }))
    : [];

  const clicksByDevice = data.clicksByDevice
    ? Object.entries(data.clicksByDevice).map(([device, count]) => ({
        name: device.charAt(0).toUpperCase() + device.slice(1),
        value: count,
      }))
    : [];

  const clicksByCountry = data.clicksByCountry
    ? Object.entries(data.clicksByCountry).slice(0, 6).map(([country, count]) => ({
        country: country || 'Unknown',
        clicks: count,
      }))
    : [];

  const CustomTooltip = ({ active, payload, label }) => {
    if (!active || !payload?.length) return null;
    return (
      <div style={{
        background: 'rgba(17, 17, 24, 0.95)',
        border: '1px solid rgba(255,255,255,0.1)',
        borderRadius: '8px',
        padding: '10px 14px',
        fontSize: '0.8rem',
      }}>
        <div style={{ color: '#8b8b9e', marginBottom: '4px' }}>{label}</div>
        <div style={{ color: '#e8e8ed', fontWeight: 600 }}>
          {payload[0].value} clicks
        </div>
      </div>
    );
  };

  return (
    <div className="analytics-section">
      <div className="section-header">
        <div className="section-title">
          📈 Analytics for <span style={{ color: 'var(--accent-secondary)', fontFamily: 'var(--font-mono)' }}>/{shortCode}</span>
          <span style={{ marginLeft: '12px', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            — {data.totalClicks} total clicks
          </span>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={onBack}>← Back</button>
      </div>

      <div className="chart-grid">
        {/* Clicks over time */}
        <div className="chart-card">
          <div className="chart-title">📅 Clicks Over Time</div>
          {clicksByDate.length > 0 ? (
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={clicksByDate}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                <XAxis dataKey="date" tick={{ fill: '#8b8b9e', fontSize: 12 }} />
                <YAxis tick={{ fill: '#8b8b9e', fontSize: 12 }} />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="clicks" fill="#6c5ce7" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="empty-state">
              <div className="empty-state-text">No click data yet</div>
            </div>
          )}
        </div>

        {/* Device breakdown */}
        <div className="chart-card">
          <div className="chart-title">📱 Device Breakdown</div>
          {clicksByDevice.length > 0 ? (
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie
                  data={clicksByDevice}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={90}
                  paddingAngle={4}
                  dataKey="value"
                  label={({ name, percent }) => `${name} (${(percent * 100).toFixed(0)}%)`}
                >
                  {clicksByDevice.map((_, index) => (
                    <Cell key={index} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="empty-state">
              <div className="empty-state-text">No device data yet</div>
            </div>
          )}
        </div>

        {/* Country breakdown */}
        <div className="chart-card" style={{ gridColumn: 'span 2' }}>
          <div className="chart-title">🌍 Top Countries</div>
          {clicksByCountry.length > 0 ? (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={clicksByCountry} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                <XAxis type="number" tick={{ fill: '#8b8b9e', fontSize: 12 }} />
                <YAxis type="category" dataKey="country" tick={{ fill: '#8b8b9e', fontSize: 12 }} width={80} />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="clicks" fill="#00cec9" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="empty-state">
              <div className="empty-state-text">No country data yet</div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
