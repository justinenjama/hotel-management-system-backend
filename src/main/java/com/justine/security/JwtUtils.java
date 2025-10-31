package com.justine.security;

import com.justine.model.Guest;
import com.justine.model.Staff;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long jwtRefreshExpirationMs;

    @Value("${app.jwt.issuer}")
    private String jwtIssuer;

    private static final String ACCESS_COOKIE_NAME = "ACCESS_TOKEN";
    private static final String REFRESH_COOKIE_NAME = "REFRESH_TOKEN";

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // --- TOKEN GENERATION ---
    public String generateToken(Guest guest) {
        Map<String, Object> claims = Map.of("roles", List.of(guest.getRole()));
        return buildToken(claims, guest.getId().toString(), jwtExpirationMs);
    }

    public String generateToken(Staff staff) {
        Map<String, Object> claims = Map.of("roles", List.of(staff.getRole().name()));
        return buildToken(claims, staff.getId().toString(), jwtExpirationMs);
    }

    public String generateRefreshToken(Guest guest) {
        Map<String, Object> claims = Map.of("roles", List.of(guest.getRole()));
        return buildToken(claims, guest.getId().toString(), jwtRefreshExpirationMs);
    }

    public String generateRefreshToken(Staff staff) {
        Map<String, Object> claims = Map.of("roles", List.of(staff.getRole().name()));
        return buildToken(claims, staff.getId().toString(), jwtRefreshExpirationMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long exp) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtIssuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + exp))
                .signWith(getSignInKey())
                .compact();
    }

    // --- VALIDATION ---
    public boolean isTokenValid(String token) {
        try { return !isTokenExpired(token); }
        catch (Exception e) { return false; }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof List<?> list)
            return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        return List.of();
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> fn) {
        return fn.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
    }

    // --- COOKIES ---
    public ResponseCookie generateAccessTokenCookie(String token) {
        return baseCookie(ACCESS_COOKIE_NAME, token, jwtExpirationMs / 1000, "/");
    }

    public ResponseCookie generateRefreshTokenCookie(String token) {
        return baseCookie(REFRESH_COOKIE_NAME, token, jwtRefreshExpirationMs / 1000, "/auth/refresh");
    }

    public ResponseCookie clearAccessTokenCookie() {
        return baseCookie(ACCESS_COOKIE_NAME, "", 0, "/");
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return baseCookie(REFRESH_COOKIE_NAME, "", 0, "/auth/refresh");
    }

    private ResponseCookie baseCookie(String name, String val, long maxAge, String path) {
        boolean isDev = true; // or inject a property
        return ResponseCookie.from(name, val)
                .httpOnly(true)
                .secure(!isDev) // false in dev, true in prod
                .sameSite(isDev ? "Lax" : "None")
                .path(path)
                .maxAge(maxAge)
                .build();
    }


    public static String getAccessCookieName() { return ACCESS_COOKIE_NAME; }
    public static String getRefreshCookieName() { return REFRESH_COOKIE_NAME; }

    /**
     * Extracts the JWT from the request cookies or Authorization header
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        // 1. Try from cookie first
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (ACCESS_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        // 2. Fallback: Bearer token in Authorization header
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * Get the 'sub' claim from JWT (user ID)
     */
    public String getSubject(String token) {
        return extractSubject(token);
    }

    /**
     * Get the 'roles' claim from JWT as a list of strings
     */
    public List<String> getRoles(String token) {
        return extractRoles(token);
    }
}
