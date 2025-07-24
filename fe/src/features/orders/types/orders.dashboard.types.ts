import {ORDER} from "@/core/config/constants";

export interface OrdersDashboardDisplay {
    orderId: string,
    orderDescription: string
    userName: string;
    totalAmount: number;
    createdAt: string;
    orderStatus: keyof typeof ORDER.STATUS;
}

export interface GetAllOrdersResponse {
    status: number;
    msg: string;
    data: OrdersDashboardDisplay[];
}