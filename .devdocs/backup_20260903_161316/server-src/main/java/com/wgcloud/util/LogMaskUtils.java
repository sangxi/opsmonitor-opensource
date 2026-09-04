package com.wgcloud.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 日志脱敏工具类，用于处理日志中的敏感信息
 */
public class LogMaskUtils {
    
    /**
     * 脱敏密码
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String maskPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return password;
        }
        return "******";
    }
    
    /**
     * 脱敏手机号
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (StringUtils.isEmpty(phone) || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 脱敏邮箱
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (StringUtils.isEmpty(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 3) {
            return parts[0] + "****@" + parts[1];
        }
        return parts[0].substring(0, 3) + "****@" + parts[1];
    }
    
    /**
     * 脱敏身份证号
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (StringUtils.isEmpty(idCard) || idCard.length() < 15) {
            return idCard;
        }
        return idCard.substring(0, 3) + "********" + idCard.substring(11);
    }
    
    /**
     * 脱敏银行卡号
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (StringUtils.isEmpty(bankCard) || bankCard.length() < 16) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + "********" + bankCard.substring(12);
    }
    
    /**
     * 脱敏URL中的敏感参数
     * @param url URL
     * @return 脱敏后的URL
     */
    public static String maskUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return url;
        }
        // 脱敏常见的敏感参数
        String[] sensitiveParams = {"password", "passwd", "pwd", "token", "secret", "key"};
        String maskedUrl = url;
        for (String param : sensitiveParams) {
            String regex = param + "=[^&]*";
            maskedUrl = maskedUrl.replaceAll(regex, param + "=******");
        }
        return maskedUrl;
    }
    
    /**
     * 脱敏JSON中的敏感字段
     * @param json JSON字符串
     * @return 脱敏后的JSON字符串
     */
    public static String maskJson(String json) {
        if (StringUtils.isEmpty(json)) {
            return json;
        }
        // 脱敏常见的敏感字段
        String[] sensitiveFields = {"password", "passwd", "pwd", "token", "secret", "key", "phone", "email", "idCard", "bankCard"};
        String maskedJson = json;
        for (String field : sensitiveFields) {
            String regex = "\"" + field + "\"\\s*:\\s*\"([^\"]*)\"";
            maskedJson = maskedJson.replaceAll(regex, "\"" + field + "\"\\s*:\\s*\"******\"");
        }
        return maskedJson;
    }
}
