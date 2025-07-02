package com.seeyon.ai.schematransformer.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LLMContentMessage{

    String type = "text";
    String text;

}

