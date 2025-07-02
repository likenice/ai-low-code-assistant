package com.seeyon.ai.ocrprocess.util;

public class FilterUtil {
    public static String filter(String str){
        String caption =  str.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "");
        // 检查结果是否以数字开头，如果是，去掉开头的数字
        char[] charArray = caption.toCharArray();
        if (!caption.isEmpty() ) {
            for (int i =0;i<charArray.length;i++) {
                if (!Character.isDigit(charArray[i])) {
                    if(i==0){
                        break;
                    }
                    caption = caption.substring(i); // 删除开头的数字
                    break;
                }
            }
        }
        caption = caption.trim();
        if(caption.length()>50){
            return caption.substring(0,50);
        }
        return caption;
    }
}
