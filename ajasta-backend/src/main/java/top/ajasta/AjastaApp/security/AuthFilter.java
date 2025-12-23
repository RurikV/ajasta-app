package top.ajasta.AjastaApp.security;

import top.ajasta.AjastaApp.exceptions.CustomAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String  token = getTokenFromRequest(request);

        if (token != null){
            try {
                String email = jwtUtils.getUsernameFromToken(token);

                // Validate that the token belongs to the same User-Agent (bound at login time)
                String currentUA = request.getHeader("User-Agent");
                if (!jwtUtils.isUserAgentValid(token, currentUA)) {
                    log.warn("Invalid User-Agent for token, continuing without authentication");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Enforce session cookie (AJASTA_SID) to match token's sid claim to prevent token pasting
                String sidInToken = jwtUtils.getSessionIdFromToken(token);
                String sidInCookie = null;
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie c : cookies) {
                        if ("AJASTA_SID".equals(c.getName())) {
                            sidInCookie = c.getValue();
                            break;
                        }
                    }
                }
                if (sidInToken == null || sidInToken.isEmpty() || sidInCookie == null || !sidInToken.equals(sidInCookie)) {
                    log.warn("Invalid session context for token, continuing without authentication");
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                if (StringUtils.hasText(email) && jwtUtils.isTokenValid(token, userDetails)){
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch(Exception ex){
                // Token validation failed, but continue without authentication
                // Spring Security will reject if the endpoint requires authentication
                log.warn("Token validation failed: {}, continuing without authentication", ex.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }


    private String getTokenFromRequest(HttpServletRequest request) {
        String tokenWithBearer = request.getHeader("Authorization");
        if (tokenWithBearer != null && tokenWithBearer.startsWith("Bearer ")) {
            return tokenWithBearer.substring(7);
        }
        return null;
    }

}











