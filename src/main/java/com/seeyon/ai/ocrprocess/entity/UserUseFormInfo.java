package com.seeyon.ai.ocrprocess.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seeyon.ai.ocrprocess.enums.AssistantTaskStatusEnum;
import com.seeyon.ai.ocrprocess.enums.AssistantTypeEnum;
import com.seeyon.boot.annotation.EntityAttribute;
import com.seeyon.boot.annotation.EntityInfo;
import com.seeyon.boot.domain.entity.BaseEntity;
import com.seeyon.boot.enums.DataType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * 标签分类实体
 */
@Data
public class UserUseFormInfo extends BaseEntity {

    private static final long serialVersionUID = 7232626202841302642L;

//    @EntityAttribute(value = "任务ID")
//    @Column(name = "task_id", columnDefinition = "BIGINT(20) COMMENT '任务ID'")
    private Long taskId;

//    @EntityAttribute(value = "创建人员ID", dataType = DataType.BIGINTEGER)
//    @Column(name = "user_id", columnDefinition = "BIGINT(20) NOT NULL COMMENT '创建人员ID'")
    private Long userId = -1L;

//    @EntityAttribute(value = "助手类型", dataType = DataType.ENUM)
//    @Column(name = "assistant_type", columnDefinition = "SMALLINT COMMENT '助手类型'")
    private AssistantTypeEnum assistantType;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    @EntityAttribute(value = "任务创建时间")
//    @Column(name = "creation_time", columnDefinition = "DATETIME COMMENT '任务创建时间'")
//    @ColumnType(jdbcType = JdbcType.TIMESTAMP)
    private Date creationTime;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    @Column(name = "end_time", columnDefinition = "DATETIME COMMENT '任务结束时间'")
//    @EntityAttribute(value = "任务结束时间", dataType = DataType.DATETIME)
//    @ColumnType(jdbcType = JdbcType.TIMESTAMP)
    private Date endTime;
//
//    @EntityAttribute(value = "任务状态", dataType = DataType.ENUM)
//    @Column(name = "status", columnDefinition = "SMALLINT COMMENT '任务状态'")
    private AssistantTaskStatusEnum status;

//    @EntityAttribute(value = "图片StorageKey", dataType = DataType.STRING)
//    @Column(name = "image_path", columnDefinition = "VARCHAR(100) COMMENT '图片StorageKey'")
    private String imagePath;

//    @EntityAttribute(value = "图片信息", dataType = DataType.STRING)
//    @Column(name = "image_info", columnDefinition = "VARCHAR(200) COMMENT '图片信息'")
    private String imageInfo;

//    @EntityAttribute(value = "ocr识别结果StorageKey", dataType = DataType.STRING)
//    @Column(name = "ocr_result", columnDefinition = "VARCHAR(100) COMMENT 'ocr识别结果StorageKey'")
    private String ocrResult;
//    @EntityAttribute(value = "LLM初步分析结果StorageKey", dataType = DataType.STRING)
//    @Column(name = "llm_initial_analysis", columnDefinition = "VARCHAR(100) COMMENT 'LLM初步分析结果StorageKey'")
    private String llmInitialAnalysis;

//    @EntityAttribute(value = "用户校对后结果StorageKey", dataType = DataType.STRING)
//    @Column(name = "user_corrected_analysis", columnDefinition = "VARCHAR(100) COMMENT '用户校对后结果StorageKey'")
    private String userCorrectedAnalysis;

//    @EntityAttribute(value = "校对差异StorageKey", dataType = DataType.STRING)
//    @Column(name = "correction_diff", columnDefinition = "VARCHAR(100) COMMENT '校对差异StorageKey'")
    private String correctionDiff;

//    @EntityAttribute(value = "是否经过校对", dataType = DataType.BOOLEAN)
//    @Column(name = "is_corrected", columnDefinition = "TINYINT(1)  COMMENT '是否经过校对'")
    private Boolean isCorrected = false;

//    @EntityAttribute(value = "OCR耗时 (秒)", dataType = DataType.INTEGER)
//    @Column(name = "duration_ocr", columnDefinition = "INT(11) COMMENT 'OCR耗时 (毫秒)'")
    private Integer durationOcr = 0;
//    @EntityAttribute(value = "页面转换耗时 (秒)", dataType = DataType.INTEGER)
//    @Column(name = "duration_page", columnDefinition = "INT(11)  COMMENT '页面转换耗时 (毫秒)'")
    private Integer durationPage = 0;
//    @EntityAttribute(value = "生成元素数量", dataType = DataType.STRING)
//    @Column(name = "generated_element_count", columnDefinition = "VARCHAR(100) COMMENT '生成元素数量'")
    private String generatedElementCount;
//    @EntityAttribute(value = "udc耗时 (秒)", dataType = DataType.INTEGER)
//    @Column(name = "duration_udc", columnDefinition = "INT(11)  COMMENT 'udc耗时 (毫秒)'")
    private Integer durationUdc= 0;

//    @EntityAttribute(value = "LLM耗时StorageKey", dataType = DataType.STRING)
//    @Column(name = "duration_llm", columnDefinition = "VARCHAR(100) COMMENT 'LLM耗时StorageKey'")
    private String durationLlm;

//    @EntityAttribute(value = "用户校对耗时 (秒)", dataType = DataType.INTEGER)
//    @Column(name = "duration_correction", columnDefinition = "INT(11) COMMENT 'LLM耗时'")
    private Integer durationCorrection= 0;

//    @EntityAttribute(value = "预估手动工时", dataType = DataType.STRING)
//    @Column(name = "estimated_manual_hours", columnDefinition = "VARCHAR(20) COMMENT '预估手动工时'")
    private String estimatedManualHours = "3.2";

//    @Column(name = "selected_templateId", columnDefinition = "VARCHAR(1000) COMMENT '选用模板ID'")
//    @EntityAttribute(value = "选用模板ID", dataType = DataType.DATETIME)
    private String selectedTemplateId;

//    @Column(name = "template_schema", columnDefinition = "VARCHAR(100) COMMENT '模版页面SchemaStorageKey'")
//    @EntityAttribute(value = "模版页面SchemaStorageKey", dataType = DataType.STRING)
    private String templateSchema;


//    @Column(name = "generated_form_id", columnDefinition = "VARCHAR(1000) COMMENT '生成的表单ID'")
//    @EntityAttribute(value = "生成的表单ID", dataType = DataType.INTEGER)
    private String generatedFormId;

//    @Column(name = "form_name", columnDefinition = "VARCHAR(1000) COMMENT '生成的表单名称'")
//    @EntityAttribute(value = "生成的表单名称", dataType = DataType.INTEGER)
    private String formName;

//    @Column(name = "error_message_storage_key", columnDefinition = "VARCHAR(100) COMMENT '错误信息StorageKey'")
//    @EntityAttribute(value = "错误信息StorageKey", dataType = DataType.INTEGER)
    private String errorMessageStorageKey;
//    @Column(name = "finish_message_storage", columnDefinition = "VARCHAR(100) COMMENT '最终结果StorageKey'")
//    @EntityAttribute(value = "最终结果StorageKey", dataType = DataType.INTEGER)
    private String finishMessageStorageKey;


}
