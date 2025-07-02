package com.seeyon.ai.schematransformer.util;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 多模态body结构
 */
@Getter
@Setter
public class LLMMultiModalMessage {


    String role;
    List<LLMContentMessage> content;

    public LLMMultiModalMessage(LLMMessageType role, String text){
        this.role = role.getValue();
        this.content = new ArrayList<>();
        this.content.add(new LLMContentMessage("text",text));
    }
}
