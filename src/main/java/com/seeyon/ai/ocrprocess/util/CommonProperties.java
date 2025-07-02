package com.seeyon.ai.ocrprocess.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component(value = "aiCommonProperties")
@ConfigurationProperties(prefix = "seeyon.ai.common")
public class CommonProperties {

    private String filePath;
    private String maxFileSize = "5M";
    private String maxRequestSize = "500M";
    private Integer minEdgePixels = 600;
    private Integer minAreaPixels = 200000;
    private Double lowBitrateRatio = 0.10;
    private String autoCompressThreshold = "1M";
}
