package com.justine.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Read token from cookies
        String token = null;
        if (request.getCookies() != null) {
            Optional<Cookie> cookieOpt = Arrays.stream(request.getCookies())
                    .filter(c -> JwtUtils.getAccessCookieName().equals(c.getName()))
                    .findFirst();
            if (cookieOpt.isPresent()) token = cookieOpt.get().getValue();
        }

        // 2. Validate token
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtils.isTokenValid(token)) {
                String userId = jwtUtils.extractSubject(token);
                boolean isStaff = jwtUtils.extractRoles(token).stream().anyMatch(r -> r.startsWith("MANAGER") || r.startsWith("STAFF"));
                UserDetails userDetails = userDetailsService.loadUserById(Long.parseLong(userId), isStaff);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
