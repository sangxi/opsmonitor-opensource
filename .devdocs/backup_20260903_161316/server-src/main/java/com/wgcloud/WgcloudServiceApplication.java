package com.wgcloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.time.Duration;

@SpringBootApplication
@MapperScan("com.wgcloud.mapper")
@ServletComponentScan("com.wgcloud.filter")
@ComponentScan(basePackages = "com.wgcloud")
@EnableCaching
@EnableScheduling
public class WgcloudServiceApplication {

    /**
     * HTTP 连接超时（毫秒）
     */
    private static final int HTTP_CONNECT_TIMEOUT_MS = 5000;

    /**
     * HTTP 读取超时（毫秒）
     */
    private static final int HTTP_READ_TIMEOUT_MS = 10000;

    public static void main(String[] args) {
        SpringApplication.run(WgcloudServiceApplication.class, args);
    }

    /**
     * 全局 RestTemplate。
     * 必须显式设置连接/读取超时：RestTemplateBuilder 无参构建时底层 SimpleClientHttpRequestFactory
     * 的 connectTimeout 与 readTimeout 默认为 -1（无限等待）。该实例同时被 HTTP 健康检测
     * (RestUtil) 与钉钉/企业微信 Webhook 推送 (WarnWebhookUtil) 复用，若目标主机不可达
     * 会永久阻塞定时任务线程，导致后续所有监控任务停滞。
     */
    @Bean
    public RestTemplate restTemplate() {
        StringHttpMessageConverter m = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        RestTemplate restTemplate = new RestTemplateBuilder()
                .additionalMessageConverters(m)
                .setConnectTimeout(Duration.ofMillis(HTTP_CONNECT_TIMEOUT_MS))
                .setReadTimeout(Duration.ofMillis(HTTP_READ_TIMEOUT_MS))
                .build();
        return restTemplate;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(50);
        return taskScheduler;
    }

}
