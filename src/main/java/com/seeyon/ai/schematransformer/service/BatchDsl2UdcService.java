package com.seeyon.ai.schematransformer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.model.DslTransformParams;
import com.seeyon.ai.schematransformer.util.Dsl2UdcTemplateUtil;
import com.seeyon.ai.schematransformer.util.DslTransformUtil;
import com.seeyon.boot.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * DSL到UDC批量转换服务
 * 
 * @author AI Assistant
 */
@Service
public class BatchDsl2UdcService {
//    @Autowired
//    MetaDataInfoService metaDataInfoService;
//
//    @Autowired
//    private ExecutorService executorService;

    // 参照映射池
//    private static final Map<String, String> referceFullNameMap = new HashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 转换DSL为UDC页面格式
     * 
     * @param params 转换参数
     * @return 转换后的DSL
     */
    public ObjectNode transformDsl(DslTransformParams params) {
        try {
            // 1. 获取参数
            JsonNode originDsl = params.getDsl();
//            String entityId = params.getEntityId();
            JsonNode appInfo = params.getAppInfo();
            String tempSchema = params.getTempSchema();
//            ArrayNode tableData = params.getTableData();
//            Map<String, String> referceFullNameMap = params.getReferceFullNameMap();
            boolean isDoc = params.isDoc(); //true : 公文 ; false: 表单;
            ArrayNode paramsEntityLists = params.getEntityList();

            // 2. 初始化DSL
            ObjectNode dsl = originDsl.deepCopy();
            dsl.put("id", "page");
            JsonNode tempSchemaObj = DslTransformUtil.parseJson(tempSchema);
            JsonNode tempSchemaChildById = tempSchemaObj.get("childById");

            // 3. 初始化结果对象
            ObjectNode newDsl = objectMapper.createObjectNode();

            ArrayNode entityLists = null;


            // 5. 获取实体数据
            if (isDoc) {
                entityLists = objectMapper.createArrayNode();
                for (JsonNode node : paramsEntityLists) {
                    entityLists.add(node);
                }
            } else {
                throw new BusinessException("isDoc=false,udc表单类型不支持,目前只支持公文表单!");
            }


            //数据清洗
            Dsl2UdcTemplateUtil.removeNullNode(dsl);

            //数据格式初始化 dsl , 修改id和type格式
            Dsl2UdcTemplateUtil.changeAllIdAndType(dsl);

            // 6. 设置基本属性  设置数据源
            if ( !entityLists.isEmpty()) {
                Map<String, JsonNode> rootChildrenAndChildById = Dsl2UdcTemplateUtil.getRootChildrenAndChildById(dsl,entityLists, tempSchemaChildById,  appInfo,   isDoc);
                JsonNode rootChildByIdNode = rootChildrenAndChildById.get("childById");
                JsonNode childrenNode = rootChildrenAndChildById.get("children");

                newDsl.set("children", childrenNode);
                newDsl.set("childById", rootChildByIdNode);
            }

            // 8. 去除多余节点
            cleanupNodes(newDsl);

            // 9. 格式化页面DSL
            
            return formatPageDsl(newDsl,entityLists,appInfo,tempSchemaObj);

        } catch (Exception e) {
            throw new RuntimeException("Transform DSL failed", e);
        }
    }



    /**
     * 清理多余节点
     */
    @SuppressWarnings("unchecked")
    private void cleanupNodes(JsonNode newDsl) {
        JsonNode childByIdNode = newDsl.get("childById");
        if (!(childByIdNode instanceof ObjectNode)) {
            return;
        }
        ObjectNode childById = (ObjectNode) childByIdNode;
        List<String> keysToRemove = new ArrayList<>();

        Iterator<String> fieldNames = childById.fieldNames();
        while (fieldNames.hasNext()) {
            String compId = fieldNames.next();
            JsonNode comp = childById.get(compId);

            if (compId.contains("udcGridCell")) {
                keysToRemove.add(compId);
                continue;
            }
            if (compId.contains("udcGrid") && comp instanceof ObjectNode) {
                ((ObjectNode) comp).set("children", objectMapper.createArrayNode());
                continue;
            }
            if (compId.contains("udcDataGrid") && comp instanceof ObjectNode) {
                JsonNode settingsNode = comp.get("settings");
                if (settingsNode instanceof ObjectNode) {
                    ((ObjectNode) settingsNode).remove("gridTemplateColumns");
                }
                continue;
            }

            if (comp instanceof ObjectNode) {
                ((ObjectNode) comp).remove("referGroup");
                JsonNode settingsNode = comp.get("settings");
                if (settingsNode instanceof ObjectNode) {
                    ((ObjectNode) settingsNode).remove("layout");
                }
            }
        }

        for (String key : keysToRemove) {
            childById.remove(key);
        }
    }

    /**
     * 格式化页面DSL
     */
    @SuppressWarnings("unchecked")
    public ObjectNode formatPageDsl(JsonNode dsl,JsonNode entityLists,JsonNode appInfo, JsonNode tempSchemaObj ) {
//        JsonNode dsl = params.get("dsl");
//        JsonNode entityLists = params.get("entityLists");
//        JsonNode appInfo = params.get("appInfo");
//        JsonNode tempSchemaObj = params.get("tempSchemaObj");

        ObjectNode result = objectMapper.createObjectNode();
        
        // 基本信息
        if (entityLists != null && entityLists.isArray() && entityLists.size() > 0) {
            JsonNode firstEntity = entityLists.get(0);
            JsonNode idNode = firstEntity.get("id");
            JsonNode nameNode = firstEntity.get("name");
            String entityId = (idNode != null && !idNode.isNull()) ? idNode.asText() :
                            (nameNode != null  && !nameNode.isNull() ? nameNode.asText() : "");
            result.put("masterEntityIds", entityId);
        }
        
        if (appInfo != null && appInfo.get("id") != null) {
            result.put("appId", appInfo.get("id").asText());
        }
        result.put("urlType", "PC");
        result.putNull("defaultOpenType");
        result.put("handWritten", false);
        result.set("inPortletParams", objectMapper.createArrayNode());
        
        // 设置
        JsonNode settingsNode = tempSchemaObj != null ? tempSchemaObj.get("settings") : null;
        if (settingsNode != null) {
            result.set("settings", settingsNode);
        } else {
            result.set("settings", objectMapper.createObjectNode());
        }
        
        // 其他属性
        result.put("pageLayout", false);
        result.set("outParameters", objectMapper.createArrayNode());
        result.set("pageVariable", objectMapper.createArrayNode());
        result.put("type", "page");
        result.set("eventDefine", objectMapper.createArrayNode());
        result.put("terminal", "PC");

        //caption是做什么的待验证.
        if (entityLists != null && entityLists.isArray() && entityLists.size() > 0) {
            JsonNode captionNode = entityLists.get(0).get("caption");
            if (captionNode != null) {
                result.put("caption", captionNode.asText());
            }
        }
        
        // inParams
        JsonNode inParamsNode = tempSchemaObj != null ? tempSchemaObj.get("inParams") : null;
        result.set("inParams", inParamsNode != null ? inParamsNode : objectMapper.createArrayNode());
        
        result.set("toBeChangedMicorflowIds", objectMapper.createArrayNode());
        
        JsonNode sourceIdNode = tempSchemaObj != null ? tempSchemaObj.get("id") : null;
        result.put("sourceId", sourceIdNode != null ? sourceIdNode.asText() : "");
        
        // 合并DSL (children 和 childById 两个节点).
        if (dsl != null && dsl.isObject()) {
            Iterator<String> fieldNames = dsl.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                result.set(fieldName, dsl.get(fieldName));
            }
        }
        
        return result;
    }





}