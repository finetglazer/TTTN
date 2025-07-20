export const API_ENDPOINTS = {
    ORDERS: '/api/orders',
    PAYMENTS: '/api/payments',
    WEBSOCKET: '/ws'
} as const;

export const API_CONFIG = {
    BASE_URL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
    TIMEOUT: 10000,
    RETRY_ATTEMPTS: 3
} as const;