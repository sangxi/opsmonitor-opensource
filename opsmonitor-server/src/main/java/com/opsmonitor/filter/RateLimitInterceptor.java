package com.opsmonitor.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求速率限制拦截器，防止暴力破解等攻击
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    // 存储IP地址的请求次数，key为IP地址，value为请求次数
    private static ConcurrentHashMap<String, AtomicInteger> requestCountMap = new ConcurrentHashMap<>();
    // 存储IP地址的最后请求时间，key为IP地址，value为时间戳
    private static ConcurrentHashMap<String, Long> lastRequestTimeMap = new ConcurrentHashMap<>();
    
    // 时间窗口（毫秒）
    private static final long TIME_WINDOW = 60 * 1000;
    // 时间窗口内允许的最大请求次数
    private static final int MAX_REQUEST_COUNT = 100;
    // 登录接口的时间窗口内允许的最大请求次数
    private static final int MAX_LOGIN_REQUEST_COUNT = 5;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ipAddress = getClientIpAddress(request);
        String requestUri = request.getRequestURI();
        
        long currentTime = System.currentTimeMillis();
        
        // 检查并重置时间窗口
        resetTimeWindow(ipAddress, currentTime);
        
        // 检查请求速率
        if (requestUri.startsWith("/opsmonitor/login/login")) {
            // 登录接口的速率限制更严格
            if (!checkRateLimit(ipAddress, MAX_LOGIN_REQUEST_COUNT)) {
                response.setStatus(429); // SC_TOO_MANY_REQUESTS
                response.getWriter().write("请求过于频繁，请稍后再试");
                return false;
            }
        } else {
            // 其他接口的速率限制
            if (!checkRateLimit(ipAddress, MAX_REQUEST_COUNT)) {
                response.setStatus(429); // SC_TOO_MANY_REQUESTS
                response.getWriter().write("请求过于频繁，请稍后再试");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取客户端IP地址
     * @param request HttpServletRequest
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    /**
     * 检查并重置时间窗口
     * @param ipAddress IP地址
     * @param currentTime 当前时间
     */
    private void resetTimeWindow(String ipAddress, long currentTime) {
        Long lastRequestTime = lastRequestTimeMap.get(ipAddress);
        if (lastRequestTime == null || currentTime - lastRequestTime > TIME_WINDOW) {
            // 时间窗口已过期，重置请求次数和最后请求时间
            requestCountMap.put(ipAddress, new AtomicInteger(0));
            lastRequestTimeMap.put(ipAddress, currentTime);
        }
    }
    
    /**
     * 检查请求速率是否超过限制
     * @param ipAddress IP地址
     * @param maxCount 最大请求次数
     * @return 是否允许请求
     */
    private boolean checkRateLimit(String ipAddress, int maxCount) {
        AtomicInteger requestCount = requestCountMap.get(ipAddress);
        if (requestCount == null) {
            requestCount = new AtomicInteger(0);
            requestCountMap.put(ipAddress, requestCount);
        }
        
        int count = requestCount.incrementAndGet();
        return count <= maxCount;
    }
}
