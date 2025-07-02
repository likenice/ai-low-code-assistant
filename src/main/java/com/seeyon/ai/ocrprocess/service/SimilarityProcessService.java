package com.seeyon.ai.ocrprocess.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Service;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class SimilarityProcessService {

    public boolean isDirectMath(String text, String targetText) {
        return text.equals(targetText);
    }

    public boolean isLevenshteinMatch(String text, String targetText, double threshold) {
        double distance = minDistance(text, targetText);
        return (distance == 1 && text.length() > 1) ||
                (distance / Math.max(text.length(), targetText.length())) < threshold;
    }

    public  double minDistance(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();

        // 创建一个二维数组 dp，dp[i][j]表示将 word1[0..i-1] 转换为 word2[0..j-1] 的最小编辑距离
        int[][] dp = new int[m + 1][n + 1];

        // 初始化边界条件
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;  // word1 转换为空字符串需要 i 次删除操作
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;  // 空字符串转换为 word2 需要 j 次插入操作
        }

        // 填充 dp 数组
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    // 如果字符相同，编辑距离不增加
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    // 否则，考虑三种操作：插入、删除、替换
                    dp[i][j] = Math.min(dp[i - 1][j - 1],  // 替换
                            Math.min(dp[i - 1][j],        // 删除
                                    dp[i][j - 1])) + 1;    // 插入
                }
            }
        }

        // 返回最终的编辑距离
        return dp[m][n];
    }


    /**
     * 余弦判断相似度
     *
     * @param text
     * @param targetText
     * @param threshold
     * @return
     * @throws IOException
     */
    public boolean isCosineMatch(String text, String targetText, double threshold) {
        double similarity = cosine(text, targetText);
        // 判断是否大于阈值
        return similarity >= threshold;
    }
    /**
     * 计算两个文本的余弦相似度
     * @param text 第一个文本
     * @param targetText 第二个文本
     * @return 余弦相似度
     */
    public  double cosine(String text, String targetText) {
        // 分词并转换为词频向量
        List<String> textTokens = tokenize(text);
        List<String> targetTokens = tokenize(targetText);

        // 构建词汇表并生成TF-IDF向量
        List<String> vocabulary = buildVocabulary(textTokens, targetTokens);
        RealVector textVector = getTFIDFVector(textTokens, vocabulary);
        RealVector targetVector = getTFIDFVector(targetTokens, vocabulary);

        // 计算余弦相似度
        double similarity = cosineSimilarity(textVector, targetVector);

        return similarity;
    }

    /**
     * 使用HanLP对文本进行分词
     * @param text 要分词的文本
     * @return 分词后的词列表
     */
//    private  List<String> tokenize(String text) {
//        List<Term> termList = HanLP.segment(text);
//        List<String> tokens = new ArrayList<>();
//        for (Term term : termList) {
//            if(term.word.equals(" ")){
//                continue;
//            }
//            tokens.add(term.word);
//        }
//        return tokens;
//    }
    private  List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Analyzer analyzer = new IKAnalyzer(true); // true表示使用细粒度分词
        try (TokenStream tokenStream = analyzer.tokenStream("", text)) {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                CharTermAttribute term = tokenStream.getAttribute(CharTermAttribute.class);
                tokens.add(term.toString());
            }
            tokenStream.end();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tokens;
    }


    /**
     * 构建词汇表，将两个文本的所有词去重后合并
     * @param textTokens 第一个文本的分词列表
     * @param targetTokens 第二个文本的分词列表
     * @return 去重后的词汇表
     */
    private  List<String> buildVocabulary(List<String> textTokens, List<String> targetTokens) {
        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(textTokens);
        vocabulary.addAll(targetTokens);
        return new ArrayList<>(vocabulary);
    }

    /**
     * 根据词汇表和文本的分词列表生成TF-IDF向量
     * @param tokens 文本的分词列表
     * @param vocabulary 词汇表
     * @return TF-IDF向量
     */
    private  RealVector getTFIDFVector(List<String> tokens, List<String> vocabulary) {
        Map<String, Integer> tokenFrequency = getTokenFrequency(tokens);
        RealVector vector = new ArrayRealVector(vocabulary.size());

        for (int i = 0; i < vocabulary.size(); i++) {
            String word = vocabulary.get(i);
            int count = tokenFrequency.getOrDefault(word, 0);
            vector.setEntry(i, count);
        }

        return vector;
    }

    /**
     * 计算词频
     * @param tokens 文本的分词列表
     * @return 词频Map
     */
    private  Map<String, Integer> getTokenFrequency(List<String> tokens) {
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String token : tokens) {
            frequencyMap.put(token, frequencyMap.getOrDefault(token, 0) + 1);
        }
        return frequencyMap;
    }

    /**
     * 计算两个向量的余弦相似度
     * @param v1 第一个向量
     * @param v2 第二个向量
     * @return 余弦相似度
     */
    private  double cosineSimilarity(RealVector v1, RealVector v2) {
        double dotProduct = v1.dotProduct(v2);
        double magnitudeV1 = v1.getNorm();
        double magnitudeV2 = v2.getNorm();
        return dotProduct / (magnitudeV1 * magnitudeV2);
    }


}
