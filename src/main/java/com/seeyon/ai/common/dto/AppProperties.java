package com.seeyon.ai.common.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
//import com.seeyon.boot.starter.nacos.annotation.ConfigAutoRefresh;

@Setter
@Getter
@Component
//@ConfigAutoRefresh
@ConfigurationProperties(prefix = "seeyon.ai-low-code-assistant")
public class AppProperties {
    boolean enableGongwen=true;
    boolean enableUdc=true;

    String  deployType = "public";  //public:公网部署  private:私有化部署
    String  aiManagerAddress = "https://ai.seeyonv8.com";
    String  aiManagerApiKey;
    String  llmModelType;  //text: 普通模型 multi: 多模态模型
    String  llmUrl;
    String  llmModel;
    String  llmApiVersion ;
    String  llmApiKey;

    String  ocrUrl;

    String pageProps = "[\"发起人\",\"创建人\",\"创建时间\"]";
    Integer keySize = 5 ;
//    String  commonFilePath;

    static String DEFAULT_MODEL = "gpt-4o";

    public String getLlmUrl() {
        if("public".equalsIgnoreCase(deployType)){
            return aiManagerAddress;
        }
        return llmUrl;
    }
    public String getLlmModel() {
        if("public".equalsIgnoreCase(deployType)){
            return DEFAULT_MODEL;
        }
        return llmModel;
    }

    public String getLlmApiKey() {
        if("public".equalsIgnoreCase(deployType)){
            return aiManagerApiKey;
        }
        return llmApiKey;
    }



    public String getLlmApiVersion() {

        if("public".equalsIgnoreCase(deployType)){
            return "2024-02-15-preview";
        }
        return llmApiVersion;
    }



    public String getLlmModelType() {
        if("public".equalsIgnoreCase(deployType)){
            return "multi";
        }

        if("Qwen2.5-72B-Instruct-AWQ".equalsIgnoreCase(llmModel)){
            return "text";
        } else if ("gpt-4o".equalsIgnoreCase(llmModel)){
            return "multi";
        } else if ("deepseek-v3".equalsIgnoreCase(llmModel)){
            return "multi";
        } else {
            return "multi";
        }

    }
}
