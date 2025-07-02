package com.seeyon.ai.ocrprocess.util;

public class SequenceMatcher {

    // 计算相似度（ratio方法）
    public static double ratio(String str1,String str2) {
        // 获取匹配块的总长度
        int matchCount = getMatchingBlocksLength(str1,str2);
        // 计算两个字符串的总长度
        int totalLength = str1.length() + str2.length();
        // 相似度公式
        return (2.0 * matchCount) / totalLength;
    }

    // 获取匹配块的长度（返回匹配字符的总数）
    private static int getMatchingBlocksLength(String str1,String str2) {
        // 使用动态规划计算最长公共子序列的匹配块
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        // 填充动态规划表
        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        /*// 计算匹配的字符数
        int matchCount = 0;
        int i = str1.length();
        int j = str2.length();

        // 通过动态规划表回溯提取匹配块
        while (i > 0 && j > 0) {
            if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                int length = 0;
                // 向前遍历，找到连续的匹配块
                while (i - length > 0 && j - length > 0 && str1.charAt(i - length - 1) == str2.charAt(j - length - 1)) {
                    length++;
                }
                matchCount += length;
                i -= length;
                j -= length;
            } else {
                i--;
                j--;
            }
        }*/

//        return matchCount;
        return  dp[str1.length()][str2.length()];
    }

    public static void main(String[] args) {
        String seq1 = "采购申请行号 aaaa 包装规格 aaa";
        String seq2 = "采购申请行号 2 物料 bbbb 包装规格 bbb";

        SequenceMatcher matcher = new SequenceMatcher();

        // 输出相似度
        System.out.println("Similarity Ratio: " + matcher.ratio(seq1,seq2));

    }
}
