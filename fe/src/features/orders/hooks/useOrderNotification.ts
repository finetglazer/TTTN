// 'use client';
//
// import { useState } from 'react';
//
// interface OrderNotificationData {
//     orderId?: string;
//     customerName?: string;
// }
//
// export const useOrderNotification = () => {
//     const [isNotificationOpen, setIsNotificationOpen] = useState(false);
//     const [notificationData, setNotificationData] = useState<OrderNotificationData | undefined>();
//
//     const showNotification = (data?: OrderNotificationData) => {
//         setNotificationData(data);
//         setIsNotificationOpen(true);
//     };
//
//     const hideNotification = () => {
//         setIsNotificationOpen(false);
//         setNotificationData(undefined);
//     };
//
//     return {
//         isNotificationOpen,
//         notificationData,
//         showNotification,
//         hideNotification,
//     };
// };