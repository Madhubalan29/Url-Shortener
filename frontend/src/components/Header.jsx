import React from 'react';

export default function Header() {
  return (
    <header className="header">
      <div className="header-brand">
        <div>
          <div className="header-logo">✂️ Snip</div>
          <div className="header-tagline">URL Shortener at Scale</div>
        </div>
      </div>
      <div className="header-badge">
        <span className="dot"></span>
        System Online
      </div>
    </header>
  );
}
