package com.deltaforce.manager.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class GameMonitorConfig {

    @Value("${game-monitor.tesseract.datapath:tessdata}")
    private String tesseractDatapath;

    @Value("${game-monitor.tesseract.language:eng}")
    private String tesseractLanguage;

    @Bean
    public Tesseract tesseract() {
        Tesseract tess = new Tesseract();
        tess.setDatapath(tesseractDatapath);
        tess.setLanguage(tesseractLanguage);
        return tess;
    }

    @Bean("gameMonitorExecutor")
    public Executor gameMonitorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("game-ocr-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
