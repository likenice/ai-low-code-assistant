package com.seeyon.ai.ocrprocess.form.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InformationRecordRequest{
    // 任务id
    private String taskId;
    // 生成的表单ID
    private String generatedFormId;
    // 用户校对后结果
    private String userCorrectedAnalysis;
    // 原识别结果
    private String correctionDiff;
    // 是否经过校对
    private Boolean isCorrected;
    // 用户校对耗时 (毫秒)
    private Integer durationCorrection;
    // 选用模板ID
    private String selectedTemplateId;
    // 模版页面Schema
    private String templateSchema;
    // 页面转换耗时 (毫秒)
    private Integer durationPage;
    // udc耗时  (毫秒)
    private Integer durationUdc;
    // 表单名称
    private String formName;


}
