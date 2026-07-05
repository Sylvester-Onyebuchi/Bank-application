package com.sylvester.bankapp.redis;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {
    private final RateLimitingService rateLimitingService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();
        RateLimitType type =
                uri.contains("/login") ? RateLimitType.LOGIN :
                        uri.contains("/transfer") ? RateLimitType.TRANSFER :
                                RateLimitType.GENERAL;

        String key = "rate:" + type + ":ip:" + clientIp;

        Bucket bucket = rateLimitingService.resolveBucket2(key, type);
        var probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()){
            response.addHeader("X-Rate-Limit-Remaining",String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        }else  {
            var waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds",String.valueOf(waitForRefill));
            response.setContentType("application/json");
            String jsonResponse = """
                    {
                        "status": %s,
                        "error": "Too Many Requests",
                        "message": "You have exhausted your API request quota",
                        "retryAfterSeconds": %s
                    
                    }
                    """.formatted(HttpStatus.TOO_MANY_REQUESTS.value(), waitForRefill);
            response.getWriter().write(jsonResponse);

        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            return request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
