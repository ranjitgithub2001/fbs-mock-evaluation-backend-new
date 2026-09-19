package com.fbs.mock_evaluation_system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil,
            UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
    	if (isPublicUnauthenticatedPath(request)) {
    	    filterChain.doFilter(request, response);
    	    return;
    	}

        // 1 — Extract Authorization header
        String authHeader = request.getHeader("Authorization");

        // 2 — If no token, continue without authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3 — Extract token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        // 4 — Validate token
        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5 — Extract email from token
        String email = jwtUtil.extractEmail(token);

        // 6 — Only set auth if not already authenticated
        if (email != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (UsernameNotFoundException ignored) {
                // Stale JWT for a deleted user: continue unauthenticated.
            }
        }

        filterChain.doFilter(request, response);
    }

    static boolean isPublicUnauthenticatedPath(HttpServletRequest request) {
        String path = servletPath(request);
        if (path.startsWith("/api/auth") || path.startsWith("/auth")) {
            return true;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return path.equals("/api/trainer-requests")
                || path.equals("/api/trainer-requests/")
                || path.equals("/trainer-requests")
                || path.equals("/trainer-requests/");
    }

    private static String servletPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isBlank()) {
            return "";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path;
    }
}
