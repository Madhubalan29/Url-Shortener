import React from 'react';

export default function StatsCards({ urls, totalClicks }) {
  const activeUrls = urls?.length || 0;

  const stats = [
    { icon: '🔗', value: activeUrls, label: 'Active URLs' },
    { icon: '📊', value: totalClicks, label: 'Total Clicks' },
    { icon: '⚡', value: activeUrls > 0 ? Math.round(totalClicks / activeUrls) : 0, label: 'Avg. Clicks/URL' },
    { icon: '🌍', value: '—', label: 'Top Country' },
  ];

  const formatNumber = (num) => {
    if (typeof num !== 'number') return num;
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toString();
  };

  return (
    <div className="stats-grid">
      {stats.map((stat, i) => (
        <div className="stat-card" key={i}>
          <div className="stat-card-icon">{stat.icon}</div>
          <div className="stat-card-value">{formatNumber(stat.value)}</div>
          <div className="stat-card-label">{stat.label}</div>
        </div>
      ))}
    </div>
  );
}
