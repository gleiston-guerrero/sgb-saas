package com.uteq.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuración del planificador de tareas periódicas de respaldos y jobs.
 */
@Configuration
public class SchedulerConfig {
    /**
     * Crea el planificador con un pool de 4 hilos demonio para los jobs programados.
     *
     * @return planificador de tareas con prefijo {@code sgb-backup-scheduler-}
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sgb-backup-scheduler-");
        scheduler.setThreadPriority(Thread.NORM_PRIORITY);
        scheduler.setDaemon(true);
        return scheduler;
    }
}