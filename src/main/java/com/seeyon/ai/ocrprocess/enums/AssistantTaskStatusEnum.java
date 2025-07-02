package com.seeyon.ai.ocrprocess.enums;
import com.seeyon.boot.enums.Messageable;

public enum AssistantTaskStatusEnum implements Messageable {
    SUCCESS(0),
    ERROR(1),
    CANCEL(2);
    private int code;

    private AssistantTaskStatusEnum(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }
}
