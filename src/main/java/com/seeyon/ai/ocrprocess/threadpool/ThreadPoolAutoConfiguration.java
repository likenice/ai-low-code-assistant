package com.seeyon.ai.ocrprocess.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties({ThreadPoolProperties.class})
public class ThreadPoolAutoConfiguration {

    @Bean
    public ExecutorService executorService(ThreadPoolProperties properties) {
        ThreadPoolTaskExecutor defaultExecutor = taskExecutor(properties);
        ExecutorService executorService = new AIExecutorService(defaultExecutor);
        return executorService;

    }

    private ThreadPoolTaskExecutor taskExecutor(ThreadPoolProperties properties) {
        ThreadPoolTaskExecutor bean = new ThreadPoolTaskExecutor();
        bean.setCorePoolSize(properties.getCoreSize() != 0 ? properties.getCoreSize() : 100);
        bean.setMaxPoolSize(properties.getMaxSize() != 0 ? properties.getMaxSize() : 200);
        bean.setQueueCapacity(properties.getQueueSize() != 0 ? properties.getQueueSize() : 500);
        bean.setKeepAliveSeconds(300);
        bean.setWaitForTasksToCompleteOnShutdown(true);
        bean.setAllowCoreThreadTimeOut(true);
        bean.setThreadNamePrefix(properties.getThreadName());
        bean.setTaskDecorator(new AsyncTaskDecorator());
        bean.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        bean.initialize();
        return bean;
    }

    @Slf4j
    public static class AsyncTaskDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(Runnable runnable) {
            return runnable;
        }
    }
}