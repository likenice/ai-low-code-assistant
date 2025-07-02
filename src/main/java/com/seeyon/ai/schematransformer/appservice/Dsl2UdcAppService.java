package com.seeyon.ai.schematransformer.appservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.seeyon.ai.schematransformer.model.DslTransformParams;
import com.seeyon.ai.schematransformer.service.BatchDsl2UdcService;
import com.seeyon.ai.schematransformer.util.JsonUtil;
import com.seeyon.boot.annotation.AppService;
import com.seeyon.boot.transport.SingleRequest;
import com.seeyon.boot.transport.SingleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@AppService(value = "批量DSL转Udc", description = "批量DSL转Udc")
//@DubboService
@Service
@Slf4j
public class Dsl2UdcAppService {


    @Autowired
    private BatchDsl2UdcService batchDsl2UdcService;


//    @AppServiceOperation(openApi=  @OpenApi(  url = "/" + StandardAppId.CIP_CAPABILITY + "/form/assistant/batchOcr2Udc",
//            customs = @Custom(name = CipConstants.OPEN_API_CUSTOM, properties = {
//                    // 此开放API不显示
//                    @CustomAttribute(name = CipConstants.NOT_SHOW_IN_MANUAL, value = "true")
//            })),value = "批量生成文单页面", description = "输入多个\"ocr schema信息\",将其结合模版信息合并. 最终转换为公文文单schema结构.并保存到文单中.")
    public SingleResponse<JsonNode> batchOcr2Udc(@RequestBody SingleRequest<DslTransformParams> params) {

        DslTransformParams dslTransformParams = params.getData();
        try {

            JsonNode udcNode = batchDsl2UdcService.transformDsl(dslTransformParams);

            //根据allUdcReferenceFullName 查询所有参照的映射关系
            Map<String, String> referenceMap = dslTransformParams.getReferceFullNameMap();

            //更新udcNode 中type= "UdcReference"  , 设置settings-> dataReference -> fullName
            JsonUtil.updateUdcReference(udcNode, referenceMap);

            return SingleResponse.from(udcNode);
        } catch (Exception e) {

            log.error("Schema转换失败", e);
            throw new RuntimeException("Schema转换失败: " + e.getMessage());
        }

    }


    public SingleResponse<JsonNode> udc2Dsl(SingleRequest<JsonNode> udcSchema) {



        return null;

    }
}
