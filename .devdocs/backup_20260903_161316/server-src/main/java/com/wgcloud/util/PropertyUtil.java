package com.wgcloud.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * @version v2.3
 * @ClassName:PropertyUtil.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: PropertyUtil.java
 * @Copyright: 2017-2022 wgcloud. All rights reserved.
 */
public class PropertyUtil {

    private static final Logger logger = LoggerFactory.getLogger(PropertyUtil.class);

    public static Set<String> getKeys(String fileName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(fileName);
        Properties p = new Properties();
        Set<String> keySet = null;
        try {
            p.load(in);
            keySet = p.stringPropertyNames();
            in.close();
        } catch (IOException e) {
            logger.error("加载配置文件失败", e);
        }
        return keySet;
    }


    public static String get(String fileName, String propertyName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(fileName);
        Properties p = new Properties();
        String msg = "";
        try {
            p.load(in);
            msg = (String) p.get(propertyName);
            in.close();
        } catch (IOException e) {
            logger.error("加载配置文件失败", e);
        }
        return msg;
    }


}
