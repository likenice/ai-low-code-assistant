package com.seeyon.ai.ocrprocess.enums;
import com.seeyon.boot.enums.Messageable;

public enum AssistantTypeEnum implements Messageable {
    UDC(0),
    EDOC(1);
    private int code;

    private AssistantTypeEnum(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }
}
