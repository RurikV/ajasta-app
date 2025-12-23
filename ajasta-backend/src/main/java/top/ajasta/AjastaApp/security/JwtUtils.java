package top.ajasta.AjastaApp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
@Slf4j
public class JwtUtils {


    private static final long EXPIRATION_TIME = 30L * 24 * 60 * 60 * 1000; // 30 days in ms
    private SecretKey key;

    @Value("${secreteJwtString}")
    private String secreteJwtString;

    @PostConstruct
    private void init() {
        byte[] keyByte = secreteJwtString.getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(keyByte, "HmacSHA256");
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimsTFunction) {
        return claimsTFunction.apply(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }

    // New overload: include both user-agent hash and session id (sid) claim
    public String generateToken(String email, String uaHash, String sessionId) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .claim("ua", uaHash == null ? "" : uaHash)
                .claim("sid", sessionId == null ? "" : sessionId)
                .signWith(key)
                .compact();
    }

    public String getUserAgentHashFromToken(String token) {
        return extractClaims(token, claims -> claims.get("ua", String.class));
    }

    public String getSessionIdFromToken(String token) {
        return extractClaims(token, claims -> claims.get("sid", String.class));
    }

    public boolean isUserAgentValid(String token, String currentUserAgent) {
        try {
            String expected = getUserAgentHashFromToken(token);
            if (expected == null || expected.isEmpty()) return false; // require UA binding
            String actual = hashUserAgent(currentUserAgent);
            return expected.equals(actual);
        } catch (Exception e) {
            return false;
        }
    }

    public String hashUserAgent(String ua) {
        if (ua == null) return "";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(ua.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}











