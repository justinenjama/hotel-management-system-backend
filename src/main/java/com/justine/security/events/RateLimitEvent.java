package com.justine.security.events;

import org.springframework.context.ApplicationEvent;

public class RateLimitEvent extends ApplicationEvent {
    private final String ip;
    private final int consecutiveDays;

    public RateLimitEvent(Object source, String ip, int consecutiveDays) {
        super(source);
        this.ip = ip;
        this.consecutiveDays = consecutiveDays;
    }

    public String getIp() {
        return ip;
    }

    public int getConsecutiveDays() {
        return consecutiveDays;
    }
}
