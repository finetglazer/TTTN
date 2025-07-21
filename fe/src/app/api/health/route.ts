import { NextRequest, NextResponse } from 'next/server';
import { API, HTTP_STATUS, MESSAGES } from '@/core/config/constants';

export async function GET(request: NextRequest) {
    try {
        // Basic health check logic
        const healthStatus = {
            status: API.HEALTH_STATUS.HEALTHY,
            timestamp: new Date().toISOString(),
            uptime: process.uptime(),
            environment: process.env.NODE_ENV || 'development',
            version: process.env.npm_package_version || API.DEFAULT_VERSION
        };

        return NextResponse.json(healthStatus, {
            status: HTTP_STATUS.OK,
            headers: {
                'Content-Type': API.CONTENT_TYPES.JSON,
                'Cache-Control': API.CACHE_CONTROL.NO_CACHE
            }
        });
    } catch (error) {
        return NextResponse.json(
            {
                status: API.HEALTH_STATUS.UNHEALTHY,
                error: MESSAGES.ERROR.INTERNAL_SERVER,
                timestamp: new Date().toISOString()
            },
            { status: HTTP_STATUS.ERROR }
        );
    }
}

/*
Add BE service health check logic here if needed.
 */

// Optional: Add other HTTP methods if needed
export async function HEAD(request: NextRequest) {
    return new NextResponse(null, { status: HTTP_STATUS.OK });
}