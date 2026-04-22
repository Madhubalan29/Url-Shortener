import axios from 'axios';

const getApiBase = () => {
  const url = import.meta.env.VITE_API_URL;
  if (!url) return '/api/v1';
  return url.endsWith('/') ? `${url}api/v1` : `${url}/api/v1`;
};

const API_BASE = getApiBase();

/**
 * Utility to fix short URLs if the backend returns localhost but we know the actual API URL.
 */
export const formatShortUrl = (shortUrl) => {
  if (!shortUrl) return '';
  const url = import.meta.env.VITE_API_URL;
  if (!url) return shortUrl;
  
  // If the URL contains localhost, replace it with the configured VITE_API_URL
  if (shortUrl.includes('localhost')) {
    const parts = shortUrl.split('/');
    const shortCode = parts[parts.length - 1];
    const baseUrl = url.endsWith('/') ? url.slice(0, -1) : url;
    return `${baseUrl}/${shortCode}`;
  }
  return shortUrl;
};

/**
 * Create an Axios instance with API key header injection.
 */
const createClient = (apiKey) => {
  return axios.create({
    baseURL: API_BASE,
    headers: {
      'Content-Type': 'application/json',
      ...(apiKey ? { 'X-API-Key': apiKey } : {}),
    },
  });
};

// ─── Auth ──────────────────────────────────────────

export const createApiKey = async (name = 'Default') => {
  const res = await axios.post(`${API_BASE}/auth/keys`, { name });
  return res.data;
};

// ─── URLs ──────────────────────────────────────────

export const shortenUrl = async (apiKey, longUrl, customAlias, ttlDays) => {
  const client = createClient(apiKey);
  const body = { longUrl };
  if (customAlias) body.customAlias = customAlias;
  if (ttlDays) body.ttlDays = parseInt(ttlDays, 10);
  const res = await client.post('/shorten', body);
  return res.data;
};

export const listUrls = async (apiKey, page = 0, size = 20) => {
  const client = createClient(apiKey);
  const res = await client.get(`/urls?page=${page}&size=${size}`);
  return res.data;
};

export const getUrlDetails = async (apiKey, shortCode) => {
  const client = createClient(apiKey);
  const res = await client.get(`/urls/${shortCode}`);
  return res.data;
};

export const deleteUrl = async (apiKey, shortCode) => {
  const client = createClient(apiKey);
  await client.delete(`/urls/${shortCode}`);
};

// ─── Analytics ─────────────────────────────────────

export const getAnalytics = async (apiKey, shortCode) => {
  const client = createClient(apiKey);
  const res = await client.get(`/analytics/${shortCode}`);
  return res.data;
};
