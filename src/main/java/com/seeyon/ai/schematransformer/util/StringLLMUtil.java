package com.seeyon.ai.schematransformer.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringLLMUtil {


    public static boolean isMatch(String classFullName,List<String> mockPackageList){
        if (mockPackageList == null || mockPackageList.size() == 0) {
            return false;
        }
        for (String packagePath : mockPackageList) {
            if(StringUtils.isEmpty(packagePath)){
                return false;
            }
            if (classFullName.startsWith(packagePath)) {
                return true;
            }
        }
        return false;
    }


    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return isEmpty(str) == false;
    }

    public static boolean isLong(String s) {
        try {
            // 尝试将字符串解析为长整型
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException e) {
            // 如果解析失败，则说明不是有效的长整型数字
            return false;
        }
    }

    /**
     * Converts the first character of the given string to uppercase.
     * If the string is empty or null, it returns the string as is.
     *
     * @param input the input string
     * @return a new string with the first character in uppercase
     */
    public static String capitalizeFirstLetter(String input) {
        // Check if the input string is null or empty
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Convert the first character to uppercase and concatenate with the rest of the string
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }


    public static void main(String[] args) {
        String str = "```java\n" +
                "import com.seeyon.app.album.appservice.KnowledgeAlbumAppService;\n" +
                "import com.seeyon.app.album.domain.entity.DocKnowledgeAlbum;\n" +
                "import com.seeyon.app.album.domain.service.DocAlbumAuditHistoryService;\n" +
                "import com.seeyon.app.album.domain.service.DocAlbumExtendService;\n" +
                "import com.seeyon.app.album.domain.service.DocKnowledgeAlbumService;\n" +
                "import com.seeyon.app.album.dto.KnowledgeAlbumAuditDto;\n" +
                "import com.seeyon.app.common.integral.enums.OperationTypeEnum;\n" +
                "import com.seeyon.app.doc.common.DocCommonUtils;\n" +
                "import com.seeyon.app.doc.util.AssertUtil;\n" +
                "import com.seeyon.app.manage.common.MessageConstants;\n" +
                "import com.seeyon.app.manage.domain.service.ClassAuthorityService;\n" +
                "import com.seeyon.app.manage.domain.service.DocIntegralOperationalService;\n" +
                "import com.seeyon.app.manage.enums.Apply;\n" +
                "import com.seeyon.app.manage.enums.AuditResult;\n" +
                "import com.seeyon.app.manage.enums.DataStatus;\n" +
                "import com.seeyon.boot.context.RequestContext;\n" +
                "import com.seeyon.boot.transport.SingleRequest;" +
                "```";
        System.out.println(extractCodeBlock(str));
    }
    public static String extractCodeBlock(String input) {
        // 定义正则表达式来匹配 ``` 或 ```java 或 ```json 代码块

//        String regex = "```(?:java)?(.*?)```";
//        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
//        Matcher matcher = pattern.matcher(input);
//
//        // 如果找到匹配的代码块，返回第一个代码块的内容
//        if (matcher.find()) {
//            return matcher.group(1).trim();
//        }
//
//        // 如果没有找到代码块，返回输入的字符串
//        return input;


//        String text = "Here is some text.\n" +
//                "```java\n" +
//                "System.out.println(\"Hello, World!\");\n" +
//                "```\n" +
//                "And here is some more text.\n" +
//                "```json\n" +
//                "{ \"key\": \"value\" }\n" +
//                "```\n";

        // 定义正则表达式
        String regex = "```(\\w+)?\\n([\\s\\S]*?)\\n```";

        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        // 查找所有匹配的代码块
        while (matcher.find()) {
            // 获取可选的语言标识符
            String language = matcher.group(1);
            // 获取代码块内容
            String codeContent = matcher.group(2);
            return codeContent;
        }

        if(input.startsWith("```java")){
            input =  input.substring(7);
        }
        if(input.startsWith("```json")){
            input =  input.substring(7);
        }
        if(input.startsWith("```")){
            input =  input.substring(3);
        }
        if(input.endsWith("```")){
            input = input.substring(0, input.length() - 3);
        }

        return input;
    }

    /**
     * 从Markdown文本中移除代码块。
     *
     * @param text 包含Markdown格式的文本。
     * @return 移除代码块后的文本。
     */
    public static String removeMarkdownCodeBlocks(String text) {
        // 正则表达式匹配以三个反引号开头和结尾的代码块
        String codeBlockPattern = "```[\\s\\S]*?```";

        // 创建Pattern对象
        Pattern pattern = Pattern.compile(codeBlockPattern);

        // 创建Matcher对象
        Matcher matcher = pattern.matcher(text);

        // 使用Matcher替换代码块为空字符串
        String result = matcher.replaceAll("");

        return result;
    }


    /**
     * 判断字符串是否int
     * @param str
     * @return
     */
    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    /**
     * 去掉某个类的引用.
     * TODO: wx 性能后续优化
     * 入参1: 字符串(
     * import com.seeyon.cap4.permission.po.BtnForbiddenPO;
     * import com.seeyon.cap4.permission.po.RuleReferencePO;
     * import com.seeyon.cap4.permission.vo.BindForbiddenBtnVO;
     * import com.seeyon.cap4.permission.vo.ForbiddenBtnVO;
     * import com.seeyon.cap4.permission.vo.PermissionRuleReferenceVO;
     * import com.seeyon.ctp.common.exceptions.BusinessException;
     * import com.seeyon.ctp.util.AppContext;
     * import com.seeyon.ctp.util.AppContextTest;
     * import org.junit.Assert;
     * )
     * 入参2:  字符串(AppContext)
     *
     * 输出: 字符串(
     * import com.seeyon.cap4.permission.po.BtnForbiddenPO;
     * import com.seeyon.cap4.permission.po.RuleReferencePO;
     * import com.seeyon.cap4.permission.vo.BindForbiddenBtnVO;
     * import com.seeyon.cap4.permission.vo.ForbiddenBtnVO;
     * import com.seeyon.cap4.permission.vo.PermissionRuleReferenceVO;
     * import com.seeyon.ctp.common.exceptions.BusinessException;
     * import com.seeyon.ctp.util.AppContextTest;
     * import org.junit.Assert;
     * )
     */
    public static String removeClassReference(String imports, String className) {
        // Split the import statements into an array of lines
        String[] lines = imports.split("\n");
        StringBuilder result = new StringBuilder();

        // Iterate through each line
        for (String line : lines) {
            // If the line does not contain the className, append it to the result
            if (line.startsWith("import ") && line.endsWith("."+className+";")) {

            } else {
                result.append(line).append("\n");
            }
        }

        // Return the result as a string
        return result.toString().trim();
    }

    /**
     * 按逗号分隔, 过滤换行符等信息
     * @param stringData
     * @return
     */
    public static List<String> spliteByComma(String stringData) {
        ArrayList result = new ArrayList<>();

        if(stringData == null){
            return result;
        }

        String[] split = stringData.replace("\r", "").replace("\n", "").split(",");
        for (String s : split) {
            if (s == null || s.trim().length() == 0) {
                continue;
            }
            String trim = s.trim();

            result.add(trim);
        }
        return result;
    }
}
