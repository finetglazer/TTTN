// Application Configuration Constants

// API & HTTP
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  NOT_FOUND: 404,
  ERROR: 500,
} as const;

export const API = {
  BASE_URL: 'http://localhost:8080', // 🔥 Added base URL
  DEFAULT_VERSION: '1.0.0',
  TIMEOUT: 10000, // 10 seconds
  HEALTH_STATUS: {
    HEALTHY: 'healthy',
    UNHEALTHY: 'unhealthy',
  },
  CONTENT_TYPES: {
    JSON: 'application/json',
  },
  CACHE_CONTROL: {
    NO_CACHE: 'no-cache',
  },
  ENDPOINTS: {
    ORDERS: {
      BASE_URL: '/api/orders',
      CREATE: '/api/orders/create',
      LIST: '/api/orders/all',


    },
    PAYMENTS: {
      BASE_URL: 'api/payments',
      PROCESS: '/api/payments/process',
      STATUS: '/api/payments',
    }
  }
} as const;

// UI & Colors
export const COLORS = {
  PRIMARY_GOLD: '#f6d55c',
  GOLD_HOVER: '#e6c53f',
  DEEP_CHARCOAL: '#1a1a1a',
} as const;

// Business Logic
export const ORDER = {
  TAX_RATE: 0.1,
  DEFAULT_QUANTITY: 1,
  MIN_QUANTITY: 1,
  PRICE_STEP: '0.01',
  STATUS: {
    CREATED: 'CREATED',
    CONFIRMED: 'CONFIRMED',
    CANCELLATION_PENDING: 'CANCELLATION_PENDING', // Add this line
    DELIVERED: 'DELIVERED',
    CANCELLED: 'CANCELLED'
  },
  PAGINATION: {
    ORDERS_PER_PAGE: 10,
  },
} as const;

export const PAYMENT = {
  STATUS: {
    PENDING: 'PENDING',
    CONFIRMED: 'CONFIRMED',
    FAILED: 'FAILED',
    REVERSED: 'REVERSED'
  }
}

// Routes
export const ROUTES = {
  HOME: '/',
  DASHBOARD: '/orders',
  CREATE_ORDER: '/orders/create',
  ABOUT: '/about',
  CONTACT: '/contact',
  PRIVACY: '/privacy',
  TERMS: '/terms',
} as const;

// Navigation Items
export const NAVIGATION = [
  { href: '/', label: 'Home' },
  { href: '/orders', label: 'Orders' },
  { href: '/create-order', label: 'Create Order' },
];

// Messages
export const MESSAGES = {
  SUCCESS: {
    ORDER_CREATED: 'Order created successfully!',
    ORDER_CANCELLED: 'Order cancelled successfully!',
  },
  ERROR: {
    INTERNAL_SERVER: 'Internal server error',
    NETWORK_ERROR: 'Network error. Please check your connection.',
    ORDER_CREATE_FAILED: 'Failed to create order. Please try again.',
    ORDER_CANCEL_FAILED: 'Failed to cancel order. Please try again.',
    VALIDATION_ERROR: 'Please check your input and try again.',
  },
} as const;

// Form Placeholders
export const PLACEHOLDERS = {
  CUSTOMER_NAME: 'Enter customer name',
  EMAIL: 'customer@example.com',
  PHONE: '+1 (555) 123-4567',
  ADDRESS: '123 Main St, City, State',
  PRODUCT_NAME: 'Product name',
  PRICE: '0.00',
} as const;

// Contact Information
export const CONTACT = {
  SUPPORT_EMAIL: 'support@orderportal.com',
  SUPPORT_PHONE: '+1 (555) 123-4567',
  BUSINESS_HOURS: 'Mon-Fri: 9AM - 6PM EST',
} as const;

// Animation & Timing
export const TIMING = {
  TRANSITION_TIMEOUT: 700,
  QUERY_STALE_TIME: 30000,
  ANIMATION_DURATION: {
    FADE_IN: '0.6s',
    PULSE_GLOW: '2s',
  },
} as const;

// Application Metadata
export const APP = {
  TITLE: 'Order Management Portal',
  DESCRIPTION: 'Professional order management system',
} as const;

// Order Status Classes for UI
export const ORDER_STATUS_CLASSES = {
  CREATED: 'bg-blue-100 text-blue-800',
  CONFIRMED: 'bg-green-100 text-green-800',
  DELIVERED: 'bg-purple-100 text-purple-800',
  CANCELLED: 'bg-red-100 text-red-800',
} as const;

// Filter Options
export const FILTER_OPTIONS = [
  { value: 'all', label: 'All Orders' },
  { value: 'CREATED', label: 'Created' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

// src/constants/queryKeys.ts
export const ordersKeys = {
  all: ['orders'] as const,
  list: () => [...ordersKeys.all, 'list'] as const,
  detail: (id: string) => [...ordersKeys.all, 'detail', id] as const,
  status: (id: string) => [...ordersKeys.all, 'status', id] as const,
};

export const paymentsKeys = {
  all: ['payments'] as const,
  byOrder: (orderId: string) => [...paymentsKeys.all, 'order', orderId] as const,
  status: (orderId: string) => [...paymentsKeys.all, 'status', orderId] as const,
};