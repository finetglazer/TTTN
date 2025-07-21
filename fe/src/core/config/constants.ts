// Application Configuration Constants

// API & HTTP
export const HTTP_STATUS = {
  OK: 200,
  ERROR: 500,
} as const;

export const API = {
  DEFAULT_VERSION: '1.0.0',
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
    DELIVERED: 'DELIVERED',
    CANCELLED: 'CANCELLED',
  },
  PAGINATION: {
    ORDERS_PER_PAGE: 10,
  },
} as const;

// Routes
export const ROUTES = {
  HOME: '/',
  ORDERS: '/orders',
  CREATE_ORDER: '/create-order',
  ABOUT: '/about',
  CONTACT: '/contact',
  PRIVACY: '/privacy',
  TERMS: '/terms',
} as const;

// Navigation Items
export const NAVIGATION = [
  { href: '/', label: 'Home' },
  { href: '/dashboard', label: 'Orders' },
  { href: '/create-order', label: 'Create Order' },
];

// Messages
export const MESSAGES = {
  SUCCESS: {
    ORDER_CREATED: 'Order created successfully!',
  },
  ERROR: {
    INTERNAL_SERVER: 'Internal server error',
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