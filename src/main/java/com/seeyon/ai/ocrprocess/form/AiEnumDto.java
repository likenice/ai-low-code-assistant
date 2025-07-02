package com.seeyon.ai.ocrprocess.form;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AiEnumDto implements Serializable {
    private static final long serialVersionUID = 736993491462535306L;
    private Object code;
    private Object name;

    public AiEnumDto(Object code, Object name) {
        this.code = code;
        this.name = name;
    }
}
