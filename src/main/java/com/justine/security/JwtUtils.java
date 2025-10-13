package com.justine.security;

import com.justine.model.Guest;
import com.justine.model.Staff;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
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

    // --------------------- TOKEN GENERATION --------------------- //
    public String generateToken(Guest guest) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of(guest.getRole()));
        return generateToken(claims, guest.getId().toString(), jwtExpirationMs);
    }

    public String generateToken(Staff staff) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of(staff.getRole().name()));
        return generateToken(claims, staff.getId().toString(), jwtExpirationMs);
    }

    public String generateRefreshToken(Guest guest) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of(guest.getRole()));
        return generateToken(claims, guest.getId().toString(), jwtRefreshExpirationMs);
    }

    public String generateRefreshToken(Staff staff) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of(staff.getRole().name()));
        return generateToken(claims, staff.getId().toString(), jwtRefreshExpirationMs);
    }

    private String generateToken(Map<String, Object> claims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtIssuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignInKey())
                .compact();
    }

    // --------------------- TOKEN VALIDATION --------------------- //
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // --------------------- CLAIMS --------------------- //
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof List<?> roleList) {
            return roleList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    // --------------------- COOKIE HELPERS --------------------- //
    public Cookie generateAccessTokenCookie(String token) {
        Cookie cookie = new Cookie(ACCESS_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // set false for dev
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpirationMs / 1000));
        return cookie;
    }

    public Cookie generateRefreshTokenCookie(String token) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // set false for dev
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge((int) (jwtRefreshExpirationMs / 1000));
        return cookie;
    }

    public Cookie clearAccessTokenCookie() {
        Cookie cookie = new Cookie(ACCESS_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }

    public Cookie clearRefreshTokenCookie() {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(0);
        return cookie;
    }

    public static String getAccessCookieName() { return ACCESS_COOKIE_NAME; }
    public static String getRefreshCookieName() { return REFRESH_COOKIE_NAME; }
}
