package com.opsmonitor.task;

import com.opsmonitor.service.ReportLogService;
import com.opsmonitor.service.ReportService;
import com.opsmonitor.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName:ReportTask.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 定时报表任务（每日/每周自动生成监控报表并发送）
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Component
public class ReportTask {

    private static final Logger logger = LoggerFactory.getLogger(ReportTask.class);

    private static final SimpleDateFormat sdfHHmm = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");

    @Resource
    private ReportService reportService;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private ReportLogService reportLogService;

    /**
     * 每日报表：每 5 分钟检查一次是否到发送时间（避免 cron 固定写死，配置即时生效）
     * 默认发送时间 08:30，可通过系统配置 reportTime 调整
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void dailyReportCheck() {
        try {
            // 开关检查
            if (!"1".equals(systemConfigService.getValue("reportEnable", "1"))) {
                return;
            }
            String reportTime = systemConfigService.getValue("reportTime", "08:30");
            if (!sdfHHmm.format(new Date()).equals(reportTime)) {
                return;
            }
            // 当天是否已发送（幂等，避免重复）
            if (hasSentToday("daily")) {
                return;
            }
            logger.info("开始生成每日监控报表");
            String res = reportService.generateAndSend("daily", null);
            logger.info("每日监控报表发送结果：" + res);
        } catch (Exception e) {
            logger.error("每日报表任务错误：", e);
        }
    }

    /**
     * 每周报表：每周一 08:40 生成上周周报
     */
    @Scheduled(cron = "0 40 8 * * MON")
    public void weeklyReportCheck() {
        try {
            if (!"1".equals(systemConfigService.getValue("reportEnable", "1"))) {
                return;
            }
            // 本周是否已发送（幂等）
            Calendar cal = Calendar.getInstance();
            int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("reportType", "weekly");
            java.util.List<com.opsmonitor.entity.ReportLog> list = reportLogService.selectAllByParams(params);
            if (list != null) {
                for (com.opsmonitor.entity.ReportLog r : list) {
                    if (r.getCreateTime() != null) {
                        Calendar rc = Calendar.getInstance();
                        rc.setTime(r.getCreateTime());
                        if (rc.get(Calendar.WEEK_OF_YEAR) == weekOfYear) {
                            return;
                        }
                    }
                }
            }
            logger.info("开始生成每周监控报表");
            String res = reportService.generateAndSend("weekly", null);
            logger.info("每周监控报表发送结果：" + res);
        } catch (Exception e) {
            logger.error("每周报表任务错误：", e);
        }
    }

    /**
     * 判断指定类型报表今天是否已发送过
     */
    private boolean hasSentToday(String reportType) {
        try {
            String today = sdfDate.format(new Date());
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("reportType", reportType);
            java.util.List<com.opsmonitor.entity.ReportLog> list = reportLogService.selectAllByParams(params);
            if (list != null) {
                for (com.opsmonitor.entity.ReportLog r : list) {
                    if (r.getCreateTime() != null && today.equals(sdfDate.format(r.getCreateTime()))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("检查今日报表发送状态错误：", e);
        }
        return false;
    }
}
