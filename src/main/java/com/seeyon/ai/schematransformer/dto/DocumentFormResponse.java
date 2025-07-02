package com.seeyon.ai.schematransformer.dto;

public class DocumentFormResponse {
    private Content content;

    public static class Content {
        private boolean isDefault;
        private String appId;
    }
}