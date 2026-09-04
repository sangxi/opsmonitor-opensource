package com.wgcloud.util.msg;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wgcloud.common.ApplicationContextHelper;
import com.wgcloud.entity.MailSet;
import com.wgcloud.service.LogInfoService;
import com.wgcloud.util.staticvar.StaticKeys;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉/企业微信 Webhook 告警工具类
 * <p>
 * 钉钉机器人支持加签（SEC）安全设置；企业微信群机器人直接 POST JSON。
 * 邮件告警同时也会触发本工具的 Webhook 推送，互不影响。
 * <p>
 * 实现要点：
 * 1. Bean 一律惰性获取。若在静态字段中直接 getBean()，一旦本类在 Spring 上下文就绪前被加载，
 * 会抛出 ExceptionInInitializerError，JVM 会永久标记该类不可用，此后所有告警将静默失效。
 * 2. 必须校验响应体中的 errcode。钉钉/企业微信在地址错误、加签失败、被限流等场景下
 * 依然返回 HTTP 200，仅靠状态码判断会把所有失败误判为成功。
 */
public class WarnWebhookUtil {

    private static final Logger logger = LoggerFactory.getLogger(WarnWebhookUtil.class);

    private WarnWebhookUtil() {
    }

    /**
     * 统一发送 Webhook 告警入口：根据 MailSet 配置同时推送钉钉和企业微信
     *
     * @param title   告警标题
     * @param content 告警内容（纯文本）
     * @return true 表示所有已配置的通道均发送成功；false 表示未启用、未配置地址或存在发送失败
     */
    public static boolean sendWarn(String title, String content) {
        if (StaticKeys.mailSet == null) {
            logger.warn("Webhook 告警未发送：告警配置尚未初始化");
            return false;
        }
        MailSet mailSet = StaticKeys.mailSet;
        if (!"1".equals(mailSet.getSendWebhook())) {
            return false;
        }
        String fullText = "【" + title + "】\n" + content;

        boolean hasChannel = false;
        boolean allSuccess = true;

        // 钉钉
        if (!StringUtils.isEmpty(mailSet.getDingdingWebhook())) {
            hasChannel = true;
            try {
                sendDingding(mailSet.getDingdingWebhook(), mailSet.getDingdingSecret(), title, fullText);
            } catch (Exception e) {
                allSuccess = false;
                logger.error("发送钉钉告警失败：", e);
                saveLog("发送钉钉告警失败", ExceptionUtils.getStackTrace(e));
            }
        }
        // 企业微信
        if (!StringUtils.isEmpty(mailSet.getWeixinWebhook())) {
            hasChannel = true;
            try {
                sendWeixin(mailSet.getWeixinWebhook(), title, fullText);
            } catch (Exception e) {
                allSuccess = false;
                logger.error("发送企业微信告警失败：", e);
                saveLog("发送企业微信告警失败", ExceptionUtils.getStackTrace(e));
            }
        }

        if (!hasChannel) {
            logger.warn("Webhook 告警已启用，但未配置任何机器人地址，告警内容未送达：{}", title);
            return false;
        }
        return allSuccess;
    }

    /**
     * 发送钉钉机器人消息（text 类型，支持加签）
     */
    private static void sendDingding(String webhook, String secret, String title, String text) throws Exception {
        String url = webhook;
        if (!StringUtils.isEmpty(secret)) {
            long timestamp = System.currentTimeMillis();
            String sign = sign(secret, timestamp);
            // 兼容 webhook 自带参数与无参数两种情况
            url += (webhook.contains("?") ? "&" : "?")
                    + "timestamp=" + timestamp
                    + "&sign=" + URLEncoder.encode(sign, "UTF-8");
        }
        Map<String, Object> textObj = new HashMap<>();
        textObj.put("content", text);
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "text");
        body.put("text", textObj);
        postJson(url, body, "钉钉");
    }

    /**
     * 发送企业微信群机器人消息（text 类型）
     */
    private static void sendWeixin(String webhook, String title, String text) throws Exception {
        Map<String, Object> textObj = new HashMap<>();
        textObj.put("content", text);
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "text");
        body.put("text", textObj);
        postJson(webhook, body, "企业微信");
    }

    /**
     * 钉钉加签算法：HmacSHA256(timestamp + "\n" + secret)
     */
    private static String sign(String secret, Long timestamp) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }

    /**
     * 通用 JSON POST 请求。
     * <p>
     * 钉钉与企业微信均以「HTTP 200 + 响应体 errcode」表达业务结果：errcode=0 为成功，
     * 非 0 为失败（如 310000 关键词不匹配、130101 被限流、93000 加签无效）。
     * 因此仅判断 HTTP 状态码会让所有失败被静默吞掉，必须解析 errcode。
     *
     * @throws RuntimeException 当 HTTP 状态码非 2xx、响应体为空、非 JSON 或 errcode 非 0 时抛出
     */
    private static String postJson(String url, Map<String, Object> body, String channel) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = getRestTemplate();
        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);

        int status = resp.getStatusCodeValue();
        if (status < 200 || status >= 300) {
            throw new RuntimeException(channel + " Webhook 响应状态码异常：" + status + "，响应体：" + resp.getBody());
        }

        String responseBody = resp.getBody();
        if (StringUtils.isEmpty(responseBody)) {
            throw new RuntimeException(channel + " Webhook 响应体为空，无法确认发送结果");
        }

        JSONObject json;
        try {
            json = JSONUtil.parseObj(responseBody);
        } catch (Exception e) {
            throw new RuntimeException(channel + " Webhook 响应体不是合法 JSON：" + responseBody, e);
        }

        // errcode 缺失时按失败处理，避免"看起来成功、实际没送达"
        int errcode = json.getInt("errcode", -1);
        if (errcode != 0) {
            throw new RuntimeException(channel + " Webhook 发送失败，errcode=" + errcode
                    + "，errmsg=" + json.getStr("errmsg"));
        }
        return responseBody;
    }

    /**
     * 惰性获取 RestTemplate。
     * 该 Bean 已在 WgcloudServiceApplication 中配置连接/读取超时，避免目标不可达时无限阻塞。
     */
    private static RestTemplate getRestTemplate() {
        RestTemplate restTemplate = ApplicationContextHelper.getBean(RestTemplate.class);
        if (restTemplate == null) {
            throw new IllegalStateException("Spring 上下文尚未就绪，无法获取 RestTemplate Bean");
        }
        return restTemplate;
    }

    /**
     * 记录告警发送异常到日志表。上下文未就绪时降级为仅打印日志，避免二次异常扩散。
     */
    private static void saveLog(String title, String content) {
        try {
            LogInfoService logInfoService = ApplicationContextHelper.getBean(LogInfoService.class);
            if (logInfoService != null) {
                logInfoService.save(title, content, StaticKeys.LOG_ERROR);
            }
        } catch (Exception e) {
            logger.error("记录 Webhook 告警日志失败：", e);
        }
    }
}
