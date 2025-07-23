import axios from 'axios';
import {API} from '@/core/config/constants';

export const axiosClient = axios.create({
    baseURL: API.BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor for auth tokens
// axiosClient.interceptors.request.use((config) => {
//     const token = localStorage.getItem('authToken');
//     if (token) {
//         config.headers.Authorization = `Bearer ${token}`;
//     }
//     return config;
// });

// Response interceptor for error handling
axiosClient.interceptors.response.use(
    (response) => response,
    (error) => {
        // Global error handling
        console.error('API Error:', error.response?.data || error.message);
        return Promise.reject(error);
    }
);