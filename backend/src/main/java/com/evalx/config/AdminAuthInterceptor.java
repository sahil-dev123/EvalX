package com.evalx.config;

import com.evalx.controller.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // Pre-flight CORS requests should always pass
        if (HttpMethod.OPTIONS.name().equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // Let evaluation and public GET endpoints pass
        if (path.startsWith("/api/evaluation") || path.startsWith("/api/auth")
                || HttpMethod.GET.name().equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // All other POST/PUT/DELETE/PATCH under /api are considered Admin operations
        if (path.startsWith("/api/")) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (AuthController.ADMIN_TOKEN.equals(token)) {
                    return true;
                }
            }

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized: Admin token required\"}");
            return false;
        }

        return true;
    }
}
