package com.rohit.ChatApplication.Schedule;

import com.rohit.ChatApplication.config.RateLimit.RateLimitConfig;
import com.rohit.ChatApplication.service.RateLimit.RateLimitAlgorithm;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;



import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@Service
public class ScheduleManagerService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleManagerService.class);

    private final Map<String, RateLimitSchedule> schedules = new HashMap<>();

    private Map<String , RateLimitConfig> activeConfigs = new HashMap<>();
    private ScheduledExecutorService  transitionExecutor;

    public ScheduleManagerService(){
        this.transitionExecutor = Executors.newScheduledThreadPool(2  );
    }

    @Scheduled(fixedRate = 60000)
    public void evaluateSchedule(){

        List<RateLimitSchedule> rateLimitSchedules = findActiveSchedules();

        for(RateLimitSchedule schedule : rateLimitSchedules){
            if( schedule.getActiveLimits() != null){
                activeConfigs.put(schedule.getKey() ,schedule.getActiveLimits());
                logger.debug("Applied active schedule '{}' for pattern '{}'",
                        schedule.getName(), schedule.getKey());
            }
        }
        logger.debug("Active schedules: {}",rateLimitSchedules.size());
    }


    public List<RateLimitSchedule> findActiveSchedules(){
        Instant time = Instant.now();
        List<RateLimitSchedule> res = new ArrayList<>();

        for(RateLimitSchedule value : schedules.values()){

            if(!value.isEnabled()){
                continue;
            }

            if(isScheduleActive(value ,time)){
                res.add(value);
            }
        }

        res.sort(Comparator.comparingInt(RateLimitSchedule::getPriority).reversed());

        return res;
    }

    private boolean isScheduleActive(RateLimitSchedule rateLimitSchedule , Instant time){

        switch(rateLimitSchedule.getType()){
            case ONE_TIME -> {
                return isOneTimeActive(rateLimitSchedule, time);
            }
            case RECURRING ->
            {
                return isRecurringActive(rateLimitSchedule , time);
            }
            case EVENT_DRIVEN -> {
                return isEventDrivenActive(rateLimitSchedule, time);
            }default -> {
                return false;
            }
        }
    }

    private boolean isOneTimeActive(RateLimitSchedule rateLimitSchedule , Instant time){
        Instant start = rateLimitSchedule.getStartTime();
        Instant end = rateLimitSchedule.getEndTime();

        if(start == null || end == null){
            return false;
        }
        return !time.isBefore(start) && time.isBefore(end) ;
    }

    private boolean isRecurringActive(RateLimitSchedule rateLimitSchedule, Instant time){
        String cronExp = rateLimitSchedule.getCronExpression();

        if(cronExp == null || cronExp.trim().isEmpty()){
            return false;
        }

        try{
            CronExpression cron = CronExpression.parse(cronExp);
            ZonedDateTime zonedTime = time.atZone(rateLimitSchedule.getTimeZone());
            ZonedDateTime nextExecution = cron.next(zonedTime);

            if(nextExecution == null) return false;

            long minutesUntilNext = java.time.Duration.between(zonedTime, nextExecution).toMinutes();
            return minutesUntilNext == 0;

        } catch (Exception e) {
            logger.error("Invalid cron expression '{}' for schedule '{}'", cronExp,
                    rateLimitSchedule.getName(), e);
            return false;
        }
    }

    private boolean isEventDrivenActive(RateLimitSchedule rateLimitSchedule , Instant time){
        Instant start = rateLimitSchedule.getStartTime();
        Instant end = rateLimitSchedule.getEndTime();

        if (start == null || end == null) {
            return false;
        }

        return !time.isBefore(start) && time.isBefore(end);
    }

    public RateLimitConfig getActiveConfig(String Key){

        RateLimitConfig exactMatch = this.activeConfigs.get(Key);
        if(exactMatch != null) return exactMatch;
        for(Map.Entry<String, RateLimitConfig> configEntry : activeConfigs.entrySet()){
            String pattern = configEntry.getKey();

            if(isValidPattern(Key,pattern)){
                return configEntry.getValue();
            }

        }
        return null;
    }

    public boolean isValidPattern(String Key , String Pattern){
        if(Pattern.equals("*")) return true;

        if(!Pattern.contains("*")) return Key.equals(Pattern);

        String regex = Pattern
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("|", "\\|")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("*", ".*");

        return Key.matches("^" + regex + "$");
    }

//    04162283604

    public void createSchedule(RateLimitSchedule rateLimitSchedule){

        if (rateLimitSchedule.getName() == null || rateLimitSchedule.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Schedule name is required");
        }

        validateSchedule(rateLimitSchedule);
        schedules.put(rateLimitSchedule.getKey() ,rateLimitSchedule);
        logger.info("Created RateLimitSchedule {}" ,rateLimitSchedule.getName());
    }

    public void updateSchedule(String name , RateLimitSchedule rateLimitSchedule){
        if (!schedules.containsKey(name)) {
            throw new IllegalArgumentException("Schedule not found: " + name);
        }

        rateLimitSchedule.setName(name);
        validateSchedule(rateLimitSchedule);
        schedules.put(name, rateLimitSchedule);
        logger.info("Updated schedule: {}", name);
    }

    public void deleteSchedule(String name) {
        RateLimitSchedule removed = schedules.remove(name);
        if (removed != null) {
            logger.info("Deleted schedule: {}", name);
        }
    }
    public RateLimitSchedule getSchedule(String name) {
        return schedules.get(name);
    }


    public List<RateLimitSchedule> getAllSchedules() {
        return new ArrayList<>(schedules.values());
    }


    public void activateSchedule(String name) {
        RateLimitSchedule schedule = schedules.get(name);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + name);
        }

        schedule.setEnabled(true);
        logger.info("Activated schedule: {}", name);
    }


    public void deactivateSchedule(String name) {
        RateLimitSchedule schedule = schedules.get(name);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + name);
        }

        schedule.setEnabled(false);
        logger.info("Deactivated schedule: {}", name);
    }

    public void validateSchedule(RateLimitSchedule schedule){
        if (schedule.getType() == null) {
            throw new IllegalArgumentException("Schedule type is required");
        }

        if (schedule.getKey() == null || schedule.getKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Key pattern is required");
        }

        switch (schedule.getType()) {
            case RECURRING:
                if (schedule.getCronExpression() == null || schedule.getCronExpression().trim().isEmpty()) {
                    throw new IllegalArgumentException("Cron expression is required for recurring schedules");
                }
                // Validate cron expression
                try {
                    CronExpression.parse(schedule.getCronExpression());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid cron expression: " + e.getMessage(), e);
                }
                break;

            case ONE_TIME:
            case EVENT_DRIVEN:
                if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
                    throw new IllegalArgumentException("Start time and end time are required for one-time and event-driven schedules");
                }
                if (!schedule.getEndTime().isAfter(schedule.getStartTime())) {
                    throw new IllegalArgumentException("End time must be after start time");
                }
                break;
        }

        if (schedule.getActiveLimits() == null) {
            throw new IllegalArgumentException("Active limits configuration is required");
        }
    }

    public static RateLimitConfig createConfig(Integer capacity , Integer refillRate,
                                               String algorithm ){

        if (capacity == null || refillRate == null) {
            throw new IllegalArgumentException("Capacity and refill rate are required");
        }

         RateLimitAlgorithm algo = algorithm != null
                ? RateLimitAlgorithm.valueOf(algorithm.toUpperCase())
                : RateLimitAlgorithm.TOKEN_BUCKET;

        return new RateLimitConfig(capacity, refillRate, 60000, algo);

    }










}
