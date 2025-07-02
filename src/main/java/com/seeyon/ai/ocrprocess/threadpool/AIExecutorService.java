package com.seeyon.ai.ocrprocess.threadpool;

import com.seeyon.ai.common.exception.ErrorCode;
import com.seeyon.ai.common.exception.PlatformException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class AIExecutorService implements ExecutorService {

    private ThreadPoolTaskExecutor defaultExecutor;

    public AIExecutorService(ThreadPoolTaskExecutor taskExecutorService) {
        this.defaultExecutor = taskExecutorService;
    }

    @Override
    public void shutdown() {
        defaultExecutor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        defaultExecutor.shutdown();
        return null;
    }

    @Override
    public boolean isShutdown() {
        throw new PlatformException(ErrorCode.MANAGE_NOT_SUPPORT);
    }

    @Override
    public boolean isTerminated() {
        throw new PlatformException(ErrorCode.MANAGE_NOT_SUPPORT);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new PlatformException(ErrorCode.MANAGE_NOT_SUPPORT);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return defaultExecutor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        throw new PlatformException(ErrorCode.MANAGE_NOT_SUPPORT);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return defaultExecutor.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new PlatformException(ErrorCode.MANAGE_NOT_SUPPORT);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw new PlatformException(ErrorCode.MANAGE_NOT_SUPPORT);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new PlatformException(ErrorCode.MANAGE_NOT_SUPPORT);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new PlatformException(ErrorCode.MANAGE_NOT_SUPPORT);
    }

    @Override
    public void execute(Runnable command) {
        defaultExecutor.execute(command);
    }
}
