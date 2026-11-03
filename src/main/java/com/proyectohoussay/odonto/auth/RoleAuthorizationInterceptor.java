package com.proyectohoussay.odonto.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {
    private final JwtTokenService tokenService;

    public RoleAuthorizationInterceptor(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !(handler instanceof HandlerMethod method)) return true;
        RequireRoles required = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequireRoles.class);
        if (required == null) required = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequireRoles.class);
        if (required == null) return true;

        String authorization = request.getHeader("Authorization");
        String role = authorization != null && authorization.startsWith("Bearer ")
                ? tokenService.roleFrom(authorization.substring(7)) : null;
        if (role == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        if (Arrays.stream(required.value()).noneMatch(role::equals)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        request.setAttribute("authenticatedRole", role);
        return true;
    }
}
