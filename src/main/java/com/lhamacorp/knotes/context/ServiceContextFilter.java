package com.lhamacorp.knotes.context;

import com.lhamacorp.knotes.client.AuthClient;
import com.lhamacorp.knotes.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
public class ServiceContextFilter extends OncePerRequestFilter {

    private final AuthClient authClient;

    public ServiceContextFilter(AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Max-Age", "3600");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        boolean isPublicBoardView = "GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().matches(".*/api/boards/[^/]+/public$");

        if (isPublicBoardView) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean requiresAuth = request.getRequestURI().contains("api/boards")
                || request.getRequestURI().contains("api/templates");
        boolean hasAuthHeader = request.getHeader("Authorization") != null;

        if (!requiresAuth && !hasAuthHeader) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UserContext user = extractCurrentUser(request);

            if (requiresAuth && user == null) {
                response.sendError(UNAUTHORIZED.value(), "Unauthorized: User not found");
                return;
            }

            if (user != null) {
                UserContextHolder.set(user);
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                UserContextHolder.clear();
            }
        } catch (UnauthorizedException e) {
            response.sendError(UNAUTHORIZED.value(), "Unauthorized: Invalid token");
        }
    }

    private UserContext extractCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        return token != null ? authClient.current(token) : null;
    }
}