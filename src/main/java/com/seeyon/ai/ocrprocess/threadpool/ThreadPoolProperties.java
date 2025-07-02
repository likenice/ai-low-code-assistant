package com.seeyon.ai.ocrprocess.threadpool;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = ThreadPoolProperties.PREFIX)
@Getter
@Setter
@Slf4j
public class ThreadPoolProperties{

    static final String PREFIX = "seeyon.ai.threadpool";

    private int coreSize = 100;
    private int maxSize = 200;
    private int queueSize = 500;

    public ThreadPoolProperties() {
    }

    public String getThreadName() {
        return "ai-threadpool-";
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ThreadPoolProperties)) {
            return false;
        } else {
            ThreadPoolProperties other = (ThreadPoolProperties) o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.getCoreSize() != other.getCoreSize()) {
                return false;
            } else if (this.getMaxSize() != other.getMaxSize()) {
                return false;
            } else {
                return this.getQueueSize() == other.getQueueSize();
            }
        }
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ThreadPoolProperties;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = result * 59 + this.getCoreSize();
        result = result * 59 + this.getMaxSize();
        result = result * 59 + this.getQueueSize();
        return result;
    }

    @Override
    public String toString() {
        return "ThreadPoolProperties(coreSize=" + this.getCoreSize() + ", maxSize=" + this.getMaxSize() + ", queueSize=" + this.getQueueSize() + ")";
    }
}