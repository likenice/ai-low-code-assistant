package com.seeyon.ai.ocrprocess.util;

import cn.hutool.json.JSONUtil;
import com.seeyon.ai.common.dto.AppProperties;
import com.seeyon.ai.common.exception.PlatformException;
import com.seeyon.boot.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Autowired;

import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

@Slf4j
public class HttpRequestUtil {
    private static final String HTTPHeaderContentType = "Content-Type";
    @Autowired
    private AppProperties appProperties;

    public static HttpEntity httpGetRequest(String url, String desc, String token) {
        try {
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            return true;
                        }
                    }).build();
            CloseableHttpClient client = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            log.info(desc + "请求地址:{},请求参数：{}", url);
//            CloseableHttpClient client = HttpClientBuilder.create().build();
            CloseableHttpResponse response = null;

            long now = System.currentTimeMillis();
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader(HTTPHeaderContentType, "application/json;charset=UTF-8");
            if (!token.equals("")) {
                httpGet.addHeader("Cookie", token);
            }
            response = client.execute(httpGet);
            HttpEntity respEntity = response.getEntity();
            return respEntity;
        } catch (Exception e) {
            log.error(desc + "接口异常", e);
            throw new PlatformException("三方接口错误");
        }
    }

    public static HttpEntity httpPostRequest(String url, Map params, String apiKey) {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        CloseableHttpResponse response;
        HttpEntity respEntity = null;
        try {
            HttpPost httpPost = new HttpPost(url);
            StringEntity stringEntity = new StringEntity(JSONUtil.toJsonStr(params), "UTF-8");
            httpPost.addHeader(HTTPHeaderContentType, "application/json;charset=UTF-8");
            httpPost.addHeader("api-key", apiKey);
            httpPost.setEntity(stringEntity);
            response = client.execute(httpPost);
            respEntity = response.getEntity();
            return respEntity;
        } catch (Exception e) {
            log.error(apiKey + "接口异常", e);
            throw new BusinessException("三方接口错误");
        }
    }

}
