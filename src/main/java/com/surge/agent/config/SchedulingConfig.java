package com.surge.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;

@Configuration
public class SchedulingConfig implements SchedulingConfigurer {
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // Set a pool of 5 threads.
        // Now Monitor and Trade Loop can run at the EXACT same time.
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(5));
    }
}