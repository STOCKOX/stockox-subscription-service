package stockox_subscription_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor

public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");
            String token = jwtUtil.extractFromHeader(authHeader);

            if (token == null || !jwtUtil.isValid(token) || !jwtUtil.isAccessToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String userId   = jwtUtil.extractUserId(token);
            UUID tenantId   = jwtUtil.extractTenantId(token);
            String role     = jwtUtil.extractRole(token);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Store tenantId in request so controllers can read it easily
                request.setAttribute("tenantId", tenantId);
                request.setAttribute("userId", UUID.fromString(userId));

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT validated — userId={} tenantId={} role={}", userId, tenantId, role);
            }

        } catch (Exception e) {
            log.error("JWT filter error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
