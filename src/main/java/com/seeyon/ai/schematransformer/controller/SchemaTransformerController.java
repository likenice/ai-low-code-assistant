package com.seeyon.ai.schematransformer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.common.base.BaseController;
import com.seeyon.ai.common.dto.AppProperties;
import com.seeyon.ai.schematransformer.appservice.Dsl2UdcAppService;
import com.seeyon.ai.schematransformer.dto.SchemaTransformerParams;
import com.seeyon.ai.schematransformer.model.DslTransformParams;
import com.seeyon.ai.schematransformer.service.*;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.boot.transport.SingleRequest;
import com.seeyon.boot.transport.SingleResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping( "ai-low-code-assistant/form/assistant")
@Slf4j
//@Tag(name = "Schema转换接口")
public class SchemaTransformerController extends BaseController {
    @Autowired
    private AppProperties appProperties;


//    @Autowired
//    private MetaDataInfoService metaDataInfoService;
    @Autowired
    private BatchDsl2UdcService batchDsl2UdcService;

    @PostMapping("/convert_gw")
//    @Operation(summary = "将UDC模板和OCR布局转换为DSL Schema(入参:layoutSchema,template  )")
    public JsonNode convertLayoutByTemplateGW(@RequestBody SchemaTransformerParams params) {
        String layoutSchema = params.getLayoutSchema();
        String template = params.getTemplate();
        Boolean isUseOcrLayout = params.getIsUseOcrLayout() ;
        if(isUseOcrLayout == null){ //默认以ocr布局为主
            isUseOcrLayout = false;
        }
        ObjectMapper objectMapper = new ObjectMapper();

        try {

            JsonNode  layoutSchemaNode;
            JsonNode templateNode ;
            if(layoutSchema == null){
                layoutSchemaNode = params.getLayoutSchemaNode();
            } else{
                layoutSchemaNode = objectMapper.readTree(layoutSchema);
            }
            if(template == null){
                templateNode = params.getTemplateNode();
            }else{
                templateNode = objectMapper.readTree(template);
            }

            return SchemaTransformerGW.convertLayoutByTemplate(layoutSchemaNode, templateNode, isUseOcrLayout);
        } catch (Exception e) {
            log.error("Schema转换失败", e);
            throw new RuntimeException("Schema转换失败: " + e.getMessage());
        }
    }

//    @PostMapping("/convert_gw_jsonnode")
////    @Operation(summary = "将UDC模板和OCR布局转换为DSL Schema(入参:layoutSchemaNode,templateNode  )")
//    public JsonNode converGWtLayoutByTemplateGWNode(@RequestBody JsonNode params) {
//
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        try {
////            String template = params.getTemplate();
//            JsonNode layoutSchemaNode = params.get("");
//            JsonNode templateNode = params.get("template");
//
//            return SchemaTransformerGW.convertLayoutByTemplate(layoutSchemaNode, templateNode);
//        } catch (Exception e) {
//            log.error("Schema转换失败", e);
//            throw new RuntimeException("Schema转换失败: " + e.getMessage());
//        }
//    }


    @PostMapping("/convert")
//    @Operation(summary = "将UDC模板和OCR布局转换为DSL Schema")
    public JsonNode  convertLayoutByTemplate(@RequestBody  SchemaTransformerParams params) {
        String layoutSchema = params.getLayoutSchema();
        String template = params.getTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode layoutSchemaNode = params.getLayoutSchemaNode();
        JsonNode templateNode = params.getTemplateNode();
        try {
            if (StringUtils.isNotBlank(layoutSchema)) {
                layoutSchemaNode = objectMapper.readTree(layoutSchema);
            }
            if (StringUtils.isNotBlank(template)) {
                templateNode = objectMapper.readTree(template);
            }


            return SchemaTransformer.convertLayoutByTemplate(layoutSchemaNode, templateNode);
        } catch (Exception e) {
            log.error("Schema转换失败", e);
            throw new RuntimeException("Schema转换失败: " + e.getMessage());
        }
    }

    @PostMapping("/dsl")
//    @Operation(summary = "将udc转为DSL Schema")
    public JsonNode getDslLayoutByTemplate(@RequestBody SchemaTransformerParams params) {

        String template = params.getTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode layoutSchemaNode = params.getLayoutSchemaNode();
        JsonNode templateNode = params.getTemplateNode();
        try {
            if (StringUtils.isNotBlank(template)) {
                templateNode = objectMapper.readTree(template);
            }

            ObjectNode templateDslSchema = SchemaTransformer.convertUdc2Dsl(templateNode);
            //生成结果以模版中、最大字体；如果没有最大字体，在顶部增加一个标准默认标题容器（程萌提供规范）；
            SchemaTransformerBase.initTitleNodeName(templateDslSchema);


            return templateDslSchema;
        } catch (Exception e) {
            log.error("Schema转换失败", e);
            throw new RuntimeException("Schema转换失败: " + e.getMessage());
        }
    }

    @PostMapping("/ocr")
//    @Operation(summary = "将ocr格式化")
    public JsonNode getOcrLayoutByTemplate(@RequestBody SchemaTransformerParams params) {

        String layoutSchema = params.getLayoutSchema();
        JsonNode layoutSchemaNode = params.getLayoutSchemaNode();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            if (layoutSchemaNode == null) {
                layoutSchemaNode = objectMapper.readTree(layoutSchema);
            }
            JsonNode groupsNode = layoutSchemaNode.get("groups");
            if (groupsNode != null && groupsNode.isArray()) {
                for (JsonNode groupNode : groupsNode) {
                    SchemaTransformerJsonUtil.fixComponents((ObjectNode) groupNode);
                }
            }

            return  layoutSchemaNode;
        } catch (Exception e) {
            log.error("Schema转换失败", e);
            throw new RuntimeException("Schema转换失败: " + e.getMessage());
        }
    }

    @GetMapping("/llm")
//    @Operation(summary = "大模型接口测试")
    public String getLLMDemo(@RequestParam(required = false) String content) {

        try {
            LLMMemoryCall llmMemoryCall = new LLMMemoryCall(appProperties);
            String callResult = llmMemoryCall.call("", content);
            return callResult;
        } catch (Exception e) {
            throw new RuntimeException("失败: " + e.getMessage());
        }
    }

    @GetMapping("/sys_config")
    public JsonNode getSysConfig() {
        Map result = sysConfig(null);
        ObjectMapper objectMapper = new ObjectMapper();
        return   objectMapper.valueToTree(result);

    }

    @Autowired
    Dsl2UdcAppService dsl2UdcAppService;

    @PostMapping("/dsl2Udc")
    public SingleResponse<JsonNode> dsl2Udc(@RequestBody DslTransformParams params) {

        return dsl2UdcAppService.batchOcr2Udc(SingleRequest.from(params));
    }


    @PostMapping("/udc2Dsl")
    public SingleResponse<JsonNode> udc2Dsl(@RequestBody JsonNode udcSchema) {
        ObjectNode templateDslSchema = SchemaTransformer.convertUdc2Dsl(udcSchema);

        return dsl2UdcAppService.udc2Dsl(SingleRequest.from(udcSchema));
    }



    private Map sysConfig(JsonNode params) {
        Map result = new HashMap<>();
        result.put("enableUdc", appProperties.isEnableUdc());
        result.put("enableGongwen", appProperties.isEnableGongwen());

        return result;
    }

    private class Result {
    }
}