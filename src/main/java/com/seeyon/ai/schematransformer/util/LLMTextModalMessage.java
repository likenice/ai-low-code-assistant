package com.seeyon.ai.schematransformer.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LLMTextModalMessage {


    String role;
    String content;

    public LLMTextModalMessage(LLMMessageType role, String text){
        this.role = role.getValue();
        this.content = text;
    }
}
