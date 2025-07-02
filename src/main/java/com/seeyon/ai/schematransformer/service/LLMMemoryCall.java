package com.seeyon.ai.schematransformer.service;



import com.seeyon.ai.common.dto.AppProperties;
import com.seeyon.ai.schematransformer.exception.BuzinessException;
import com.seeyon.ai.schematransformer.util.AIManagerHttpUtil;
import com.seeyon.ai.schematransformer.util.AzureHttpUtil;
import com.seeyon.ai.schematransformer.util.DeepSeekHttpUtil;
import com.seeyon.ai.schematransformer.util.JsonUtil;
import com.seeyon.ai.schematransformer.util.LLMMessageType;
import com.seeyon.ai.schematransformer.util.LLMMultiModalMessage;
import com.seeyon.ai.schematransformer.util.LLMTextModalMessage;
import com.seeyon.ai.schematransformer.util.QwenHttpUtil;
import com.seeyon.ai.schematransformer.util.StringLLMUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 大语言模型带记忆 调用. (根据application.yml 来选择模型调用)
 */

public class LLMMemoryCall {

    private AppProperties appProperties;

    private String systemTemplate;
    private List llmMessages = new ArrayList<>();

//    String model = AppSettingsState.getInstance().getGptModel();
//    String modelModal = AppSettingsState.getInstance().getGptModelType(); // "text"文本对话  "multi"多模态
    public static Long TOKEN_TOTAL = 0L;
    public static Long TIME_TOTAL = 0L;

    public int maxLoop = 10;

    public LLMMemoryCall(String systemTemplate, AppProperties appProperties){
        this.appProperties = appProperties;
        String modelModal = appProperties.getLlmModelType();

        switch(modelModal){
            case "text": //普通模态
                llmMessages.add(new LLMTextModalMessage(LLMMessageType.SYSTEM,systemTemplate));
                break;
            case "multi": //多模态
                llmMessages.add(new LLMMultiModalMessage(LLMMessageType.SYSTEM,systemTemplate));
                break;

        }
    }
    public LLMMemoryCall( AppProperties appProperties){
        this.appProperties = appProperties;
    }


    public static void initTotal(){
        TOKEN_TOTAL = 0L;
        TIME_TOTAL = 0L;
    }

    public String call(String name, String data ){
        List<String> historyMessageList = new ArrayList<>();
        //如果阻断, 递归调用.拼接输出结果.
        recursionCall(name, data, historyMessageList);
        //去除 代码块
        String resultMessage = JsonUtil.deleteDuplite(historyMessageList);//String.join("", historyMessageList);

        return resultMessage;
    }
    /**
     * 保留历史记忆的对话.
     * @param data 请求大模型prompt
     * @return
     */
    public void recursionCall(String name, String data ,List<String> historyMessageList){
        String modelModal = appProperties.getLlmModelType();
        String model = appProperties.getLlmModel();
        String errorInfo = "";
        try {
            System.out.println("sendMessage start:"+name);
            long start = System.currentTimeMillis();
            String message = "";

            if(modelModal.equals("multi")){
                llmMessages.add(new LLMMultiModalMessage(LLMMessageType.USER,data));
            } else {
                llmMessages.add(new LLMTextModalMessage(LLMMessageType.USER,data));
            }

            switch(model){
                case "gpt-4o":
                    message = AIManagerHttpUtil.sendMessage(llmMessages,appProperties);
                    break;
                case "Qwen2.5-72B-Instruct-AWQ":
                    message = QwenHttpUtil.sendMessage(llmMessages,appProperties);
                    break;
                case "DeepSeek-V3":
                    message = DeepSeekHttpUtil.sendMessage(llmMessages,appProperties);
                    break;
                case "azure":
                    message = AzureHttpUtil.sendMessage(llmMessages,appProperties);
                    break;

            }

            errorInfo = message;

            String azureMessage = JsonUtil.getAzureMessage(message);

            //没有返回值, 说明出错了
            if(StringLLMUtil.isEmpty(azureMessage)){
                throw new BuzinessException(message);
            }
            long end = System.currentTimeMillis();
//            if(CopilotConstants.isDebug){
                long takeSeconds = (end-start);
                String azureUseTokens = JsonUtil.getAzureUseTokens(message);

                TIME_TOTAL += takeSeconds;
                if(StringLLMUtil.isLong(azureUseTokens)) {
                    TOKEN_TOTAL += Long.parseLong(azureUseTokens);
                }
//                System.out.println("\ncall content:"+azureMessage+"\n\n");
//            }

            switch(modelModal){
                case "text": //普通模态
                    llmMessages.add(new LLMTextModalMessage(LLMMessageType.ASSISTANT,azureMessage));
                    break;
                case "multi": //多模态
                    llmMessages.add(new LLMMultiModalMessage(LLMMessageType.ASSISTANT,azureMessage));
                    break;

            }
            System.out.println("call:"+name+" . llm speed："+takeSeconds+"ms, tokens:"+azureUseTokens);
            if(maxLoop > 0 ){
                maxLoop--;
                boolean isLengthFinish = JsonUtil.isLengthFinish(message);
                azureMessage = StringLLMUtil.extractCodeBlock(azureMessage);
                historyMessageList.add(azureMessage);
                if(isLengthFinish){
                    System.out.println("lengthFinish:"+azureMessage);
                    recursionCall(name,"如果输出的是json结构,请继续输出剩余的JSON，从上次结束的地方开始.",historyMessageList);
                }else{
                    return ;
                }
            } else {
                System.out.println("call:"+name+" . llm speed："+takeSeconds+"ms, tokens:"+azureUseTokens);
                throw new BuzinessException("超过单词循环调用深度.");
            }
            return ;

        } catch (Exception e) {
            throw new BuzinessException(e.getMessage()+"\n"+errorInfo);
        }

    }

    public AppProperties getAppProperties(){
        return appProperties;
    }

//    public static AppProperties getStaticAppProperties(){
//        LLMMemoryCall llmMemoryCall = new LLMMemoryCall();
//        return llmMemoryCall.getAppProperties();
//    }


}
