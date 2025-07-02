package com.seeyon.ai.ocrprocess.util;

/**
 * 字符串相似程度
 * Levenshtein距离算法来计算两个字符串的相似度。Levenshtein距离是指两个字符串之间，由一个转换成另一个所需的最少编辑操作次数
 */
public class StringSimilarity {
    public static int levenshteinDistance(String str1, String str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            distance[i][0] = i;
        }

        for (int j = 0; j <= str2.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    distance[i][j] = distance[i - 1][j - 1];
                } else {
                    distance[i][j] = 1 + Math.min(Math.min(distance[i - 1][j], distance[i][j - 1]), distance[i - 1][j - 1]);
                }
            }
        }

        return distance[str1.length()][str2.length()];
    }

    public static double similarity(String str1, String str2) {
        int maxLength = Math.max(str1.length(), str2.length());
        return 1 - (double) levenshteinDistance(str1, str2) / maxLength;
    }


}
