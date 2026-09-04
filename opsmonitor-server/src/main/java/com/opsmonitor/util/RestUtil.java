package com.opsmonitor.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * @version V1.0
 * @ClassName:RestUtil.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: RestUtil.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Component
public class RestUtil {

    private Logger logger = LoggerFactory.getLogger(RestUtil.class);

    @Autowired
    private RestTemplate restTemplate;

    public JSONObject post(String url, JSONObject jsonObject) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("Accept", MediaType.APPLICATION_JSON_UTF8.toString());
        HttpEntity<String> httpEntity = new HttpEntity<>(JSONUtil.parse(jsonObject).toString(), headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, httpEntity, String.class);
        return JSONUtil.parseObj(responseEntity.getBody());
    }

    public JSONObject post(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("Accept", MediaType.APPLICATION_JSON_UTF8.toString());
        HttpEntity<String> httpEntity = new HttpEntity<>("", headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, httpEntity, String.class);
        return JSONUtil.parseObj(responseEntity.getBody());
    }

    public int get(String url) {
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
            return responseEntity.getStatusCodeValue();
        } catch (HttpClientErrorException e) {
            logger.error("服务接口检测任务错误", e);
            return e.getRawStatusCode();
        } catch (Exception e) {
            logger.error("服务接口检测任务错误", e);
            return 500;
        }
    }

    /**
     * TCP 端口连通性检测
     *
     * @param host      目标主机
     * @param port      目标端口
     * @param timeoutMs 超时毫秒
     * @return 200 连通成功，500 失败
     */
    public int checkTcp(String host, int port, int timeoutMs) {
        // 注意：端口不通是被监控对象的正常状态之一，用 WARN 记录即可。
        // 若用 ERROR，一个持续宕机的端口会在每个检测周期刷一条错误日志，淹没真正的系统异常。
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
            return 200;
        } catch (Exception e) {
            logger.warn("TCP 端口检测失败 host={}, port={}, timeoutMs={}：{}", host, port, timeoutMs, e.toString());
            return 500;
        }
    }


}
