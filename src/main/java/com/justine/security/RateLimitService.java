package com.justine.security;

import com.justine.security.events.RateLimitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ApplicationEventPublisher eventPublisher;

    // In-memory rate store
    private final Map<String, List<LocalDateTime>> requestHistory = new ConcurrentHashMap<>();

    // Track consecutive-day offenders
    private final Map<String, Integer> consecutiveDayViolations = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_HOUR = 50;
    private static final int MAX_CONSECUTIVE_DAYS = 3;

    public boolean allowRequest(String ip, String action) {
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> timestamps = requestHistory.computeIfAbsent(ip, k -> new ArrayList<>());

        // Clean up old entries (older than 1 hour)
        timestamps.removeIf(time -> time.isBefore(now.minusHours(1)));
        timestamps.add(now);

        if (timestamps.size() > MAX_REQUESTS_PER_HOUR) {
            log.warn("Rate limit hit: IP={} action={} count={}", ip, action, timestamps.size());
            recordViolation(ip);
            return false;
        }

        return true;
    }

    private void recordViolation(String ip) {
        int count = consecutiveDayViolations.getOrDefault(ip, 0) + 1;
        consecutiveDayViolations.put(ip, count);

        if (count >= MAX_CONSECUTIVE_DAYS) {
            log.warn("IP {} exceeded daily limit for {} consecutive days. Triggering admin alert.", ip, count);
            eventPublisher.publishEvent(new RateLimitEvent(this, ip, count));
            consecutiveDayViolations.put(ip, 0); // reset after alert
        }
    }
}
