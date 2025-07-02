package com.seeyon.ai.schematransformer.enums;

public enum StereotypeEnum {
    Bill("bill");
//    Entity("Entity"),
//    None("None");

    private final String value;

    StereotypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 