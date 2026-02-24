package com.rohit.ChatApplication.Schedule;

import com.rohit.ChatApplication.config.RateLimit.RateLimitConfig;
import lombok.Getter;
import lombok.Setter;


import java.time.Instant;
import java.time.ZoneId;

@Getter
@Setter
public class RateLimitSchedule {
    private String name;
    private String key;
    private String cronExpression;
    private ScheduleType type;
    private ZoneId timeZone;
    private Instant startTime;
    private Instant endTime;
    private RateLimitConfig activeLimits;
    private RateLimitConfig fallBackLimits;
    private int priority;
    private boolean enabled;


    public RateLimitSchedule() {
        this.timeZone = ZoneId.of("IST");
        this.priority = 0;
        this.enabled = true;
    }

    public RateLimitSchedule(String name, String keyPattern, ScheduleType type) {
        this.name = name;
        this.key = keyPattern;
        this.type = type;
        this.timeZone = ZoneId.of("IST");
        this.priority = 0;
        this.enabled = true;
    }


}
