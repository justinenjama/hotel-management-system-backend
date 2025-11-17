package com.justine.security;

import com.justine.model.Guest;
import com.justine.model.Staff;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
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

    // ---------------- TOKEN GENERATION ----------------
    public String generateToken(Guest guest) {
        return buildToken(Map.of("roles", List.of(guest.getRole())), guest.getId().toString(), jwtExpirationMs);
    }

    public String generateToken(Staff staff) {
        return buildToken(Map.of("roles", List.of(staff.getRole().name())), staff.getId().toString(), jwtExpirationMs);
    }

    public String generateRefreshToken(Guest guest) {
        return buildToken(Map.of("roles", List.of(guest.getRole())), guest.getId().toString(), jwtRefreshExpirationMs);
    }

    public String generateRefreshToken(Staff staff) {
        return buildToken(Map.of("roles", List.of(staff.getRole().name())), staff.getId().toString(), jwtRefreshExpirationMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtIssuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignInKey())
                .compact();
    }

    // ---------------- TOKEN VALIDATION ----------------
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date exp = extractClaim(token, Claims::getExpiration);
        return exp.before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException e) {
            throw new RuntimeException("Invalid JWT signature", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JWT", e);
        }
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

    // ---------------- COOKIE MANAGEMENT ----------------
    public ResponseCookie generateAccessTokenCookie(String token, HttpServletRequest request) {
        return buildCookie(ACCESS_COOKIE_NAME, token, jwtExpirationMs / 1000, "/", request);
    }

    public ResponseCookie generateRefreshTokenCookie(String token, HttpServletRequest request) {
        return buildCookie(REFRESH_COOKIE_NAME, token, jwtRefreshExpirationMs / 1000, "/auth/refresh", request);
    }

    public ResponseCookie clearAccessTokenCookie(HttpServletRequest request) {
        return buildCookie(ACCESS_COOKIE_NAME, "", 0, "/", request);
    }

    public ResponseCookie clearRefreshTokenCookie(HttpServletRequest request) {
        return buildCookie(REFRESH_COOKIE_NAME, "", 0, "/auth/refresh", request);
    }

    private ResponseCookie buildCookie(String name, String value, long maxAge, String path, HttpServletRequest request) {
        boolean isSecure = request.isSecure(); // HTTPS only
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(isSecure ? "None" : "Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    // ---------------- TOKEN EXTRACTION ----------------
    public String extractTokenFromRequest(HttpServletRequest request) {
        // 1️⃣ Check cookies first
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (ACCESS_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        // 2️⃣ Fallback: Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    public String getSubject(String token) {
        return extractSubject(token);
    }

    public List<String> getRoles(String token) {
        return extractRoles(token);
    }

    // ---------------- CONSTANTS ----------------
    public static String getAccessCookieName() {
        return ACCESS_COOKIE_NAME;
    }

    public static String getRefreshCookieName() {
        return REFRESH_COOKIE_NAME;
    }
}
