package com.justine.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    public JwtCookieFilter(JwtUtils jwtUtils, CustomUserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractAccessToken(request);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtUtils.isTokenValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String userId = jwtUtils.extractSubject(token);
                List<String> roles = jwtUtils.extractRoles(token);

                boolean isStaff = roles.stream().anyMatch(r ->
                        r.equalsIgnoreCase("ADMIN") ||
                                r.equalsIgnoreCase("MANAGER") ||
                                r.equalsIgnoreCase("RECEPTIONIST")
                );

                UserDetails userDetails = userDetailsService.loadUserById(Long.parseLong(userId), isStaff);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("[JWT_COOKIE_FILTER] Authenticated userId={} roles={}", userId, roles);
            }
        } catch (Exception e) {
            log.warn("[JWT_COOKIE_FILTER] Token invalid: {}", e.getMessage());
            clearInvalidCookies(request, response);
        }

        filterChain.doFilter(request, response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (JwtUtils.getAccessCookieName().equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private void clearInvalidCookies(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader("Set-Cookie", jwtUtils.clearAccessTokenCookie(request).toString());
        response.addHeader("Set-Cookie", jwtUtils.clearRefreshTokenCookie(request).toString());
    }
}
