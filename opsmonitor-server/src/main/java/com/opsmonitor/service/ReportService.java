package com.opsmonitor.service;

import com.opsmonitor.entity.AlarmRecord;
import com.opsmonitor.entity.ReportLog;
import com.opsmonitor.entity.SystemInfo;
import com.opsmonitor.util.FormatUtil;
import com.opsmonitor.util.msg.WarnMailUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName:ReportService.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 定时报表服务（聚合监控数据生成 HTML 报表并发送）
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");

    @Resource
    private SystemInfoService systemInfoService;
    @Resource
    private AlarmRecordService alarmRecordService;
    @Resource
    private ReportLogService reportLogService;
    @Resource
    private HeathMonitorService heathMonitorService;
    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 生成每日资源报表 HTML 正文
     */
    public String buildDailyHtml() {
        StringBuilder sb = new StringBuilder();
        int totalHost = 0, onlineHost = 0, offlineHost = 0;
        StringBuilder hostRows = new StringBuilder();
        try {
            List<SystemInfo> hostList = systemInfoService.selectAllByParams(new HashMap<String, Object>());
            if (hostList != null) {
                totalHost = hostList.size();
                for (SystemInfo h : hostList) {
                    if ("1".equals(h.getState())) {
                        onlineHost++;
                    } else {
                        offlineHost++;
                    }
                    String cpuColor = h.getCpuPer() != null && h.getCpuPer() >= 90 ? "#d9534f" : (h.getCpuPer() != null && h.getCpuPer() >= 70 ? "#f0ad4e" : "#5cb85c");
                    String memColor = h.getMemPer() != null && h.getMemPer() >= 90 ? "#d9534f" : (h.getMemPer() != null && h.getMemPer() >= 70 ? "#f0ad4e" : "#5cb85c");
                    String diskColor = h.getDiskPer() != null && h.getDiskPer() >= 90 ? "#d9534f" : (h.getDiskPer() != null && h.getDiskPer() >= 70 ? "#f0ad4e" : "#5cb85c");
                    String groupName = StringUtils.isEmpty(h.getGroupName()) ? "-" : h.getGroupName();
                    String stateText = "1".equals(h.getState()) ? "在线" : "离线";
                    hostRows.append("<tr>")
                            .append("<td>").append(esc(h.getHostname())).append("</td>")
                            .append("<td>").append(esc(groupName)).append("</td>")
                            .append("<td align=\"center\">").append(h.getCpuPer() == null ? "-" : h.getCpuPer()).append("%</td>")
                            .append("<td align=\"center\">").append(h.getMemPer() == null ? "-" : h.getMemPer()).append("%</td>")
                            .append("<td align=\"center\">").append(h.getDiskPer() == null ? "-" : h.getDiskPer()).append("%</td>")
                            .append("<td align=\"center\" style=\"color:").append(stateText.equals("在线") ? "#5cb85c" : "#d9534f").append("\">").append(stateText).append("</td>")
                            .append("</tr>");
                }
            }
        } catch (Exception e) {
            logger.error("生成主机报表数据错误：", e);
        }

        // 告警统计
        int alarmTotal = 0, alarmUnhandled = 0;
        StringBuilder alarmRows = new StringBuilder();
        try {
            Map<String, Object> ap = new HashMap<String, Object>();
            alarmTotal = alarmRecordService.countByParams(ap);
            Map<String, Object> up = new HashMap<String, Object>();
            up.put("state", "0");
            alarmUnhandled = alarmRecordService.countByParams(up);
            List<AlarmRecord> alarmList = alarmRecordService.selectAllByParams(ap);
            if (alarmList != null && !alarmList.isEmpty()) {
                int limit = Math.min(alarmList.size(), 20);
                for (int i = 0; i < limit; i++) {
                    AlarmRecord a = alarmList.get(i);
                    String levelColor = "error".equals(a.getLevel()) ? "#d9534f" : ("warn".equals(a.getLevel()) ? "#f0ad4e" : "#5cb85c");
                    alarmRows.append("<tr>")
                            .append("<td>").append(a.getHostname() == null ? "-" : esc(a.getHostname())).append("</td>")
                            .append("<td>").append(a.getTitle() == null ? "-" : esc(a.getTitle())).append("</td>")
                            .append("<td align=\"center\" style=\"color:").append(levelColor).append("\">").append(a.getLevel() == null ? "-" : a.getLevel()).append("</td>")
                            .append("<td align=\"center\">").append("0".equals(a.getState()) ? "未处理" : ("1".equals(a.getState()) ? "已处理" : "已忽略")).append("</td>")
                            .append("<td>").append(a.getCreateTime() == null ? "-" : sdf.format(a.getCreateTime())).append("</td>")
                            .append("</tr>");
                }
            }
        } catch (Exception e) {
            logger.error("生成告警报表数据错误：", e);
        }

        sb.append("<html><body style=\"font-family:'Microsoft YaHei',Arial,sans-serif;color:#333;font-size:14px;\">");
        sb.append("<h2 style=\"color:#2c3e50;border-bottom:2px solid #3498db;padding-bottom:8px;\">OpsMonitor 每日监控报表</h2>");
        sb.append("<p style=\"color:#888;font-size:12px;\">生成时间：").append(sdf.format(new Date())).append("</p>");
        sb.append("<table style=\"width:100%;border-collapse:collapse;margin:12px 0;\">");
        sb.append("<tr>");
        sb.append("<td style=\"padding:10px;background:#eaf4fd;border:1px solid #d0e6f7;border-radius:4px;\"><b>主机总数</b><br/><span style=\"font-size:22px;color:#3498db;\">").append(totalHost).append("</span></td>");
        sb.append("<td style=\"padding:10px;background:#e9f9ee;border:1px solid #c9ecd4;border-radius:4px;\"><b>在线主机</b><br/><span style=\"font-size:22px;color:#27ae60;\">").append(onlineHost).append("</span></td>");
        sb.append("<td style=\"padding:10px;background:#fdf0ea;border:1px solid #f5cfc2;border-radius:4px;\"><b>离线主机</b><br/><span style=\"font-size:22px;color:#d9534f;\">").append(offlineHost).append("</span></td>");
        sb.append("<td style=\"padding:10px;background:#fff8e6;border:1px solid #f5e6b8;border-radius:4px;\"><b>告警总数</b><br/><span style=\"font-size:22px;color:#f0ad4e;\">").append(alarmTotal).append("</span></td>");
        sb.append("<td style=\"padding:10px;background:#fdf0ea;border:1px solid #f5cfc2;border-radius:4px;\"><b>未处理告警</b><br/><span style=\"font-size:22px;color:#d9534f;\">").append(alarmUnhandled).append("</span></td>");
        sb.append("</tr></table>");

        sb.append("<h3 style=\"color:#2c3e50;\">一、主机资源概览</h3>");
        sb.append("<table style=\"width:100%;border-collapse:collapse;font-size:13px;\">");
        sb.append("<tr style=\"background:#3498db;color:#fff;\">");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">主机</th>");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">分组</th>");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">CPU%</th>");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">内存%</th>");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">磁盘%</th>");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">状态</th>");
        sb.append("</tr>");
        sb.append(hostRows.length() > 0 ? hostRows.toString() : "<tr><td colspan=\"6\" align=\"center\" style=\"padding:10px;border:1px solid #ddd;color:#999;\">暂无主机数据</td></tr>");
        sb.append("</table>");

        sb.append("<h3 style=\"color:#2c3e50;\">二、最近告警</h3>");
        sb.append("<table style=\"width:100%;border-collapse:collapse;font-size:13px;\">");
        sb.append("<tr style=\"background:#e67e22;color:#fff;\">");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">主机</th>");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">告警内容</th>");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">级别</th>");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">状态</th>");
        sb.append("<th style=\"padding:6px;border:1px solid #ddd;\">时间</th>");
        sb.append("</tr>");
        sb.append(alarmRows.length() > 0 ? alarmRows.toString() : "<tr><td colspan=\"5\" align=\"center\" style=\"padding:10px;border:1px solid #ddd;color:#999;\">暂无告警</td></tr>");
        sb.append("</table>");
        sb.append("<p style=\"color:#999;font-size:11px;margin-top:16px;\">本邮件由 OpsMonitor 运维监控系统自动生成，请勿直接回复。</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * 生成并发送报表
     *
     * @param reportType daily/weekly/manual
     * @param receiver   收件人（分号分隔），空则用邮件配置默认收件人
     * @return success/error
     */
    public String generateAndSend(String reportType, String receiver) {
        try {
            String title;
            String content;
            if ("daily".equals(reportType)) {
                title = "每日监控报表 " + sdfDate.format(new Date());
                content = buildDailyHtml();
            } else {
                title = "监控报表 " + sdfDate.format(new Date());
                content = buildDailyHtml();
            }

            // 收件人优先用传入，其次系统配置，再次邮件配置
            if (StringUtils.isEmpty(receiver)) {
                receiver = systemConfigService.getValue("reportMail", "");
            }
            if (StringUtils.isEmpty(receiver) && com.opsmonitor.util.staticvar.StaticKeys.mailSet != null) {
                receiver = com.opsmonitor.util.staticvar.StaticKeys.mailSet.getToMail();
            }

            // 写报表记录
            ReportLog log = new ReportLog();
            log.setId(com.opsmonitor.util.UUIDUtil.getUUID());
            log.setReportType(reportType);
            log.setTitle(title);
            log.setContent(content);
            log.setReceiver(receiver);
            log.setSendStatus("0");
            log.setCreateTime(new Date());
            reportLogService.save(log);

            if (StringUtils.isEmpty(receiver)) {
                log.setSendStatus("2");
                log.setTitle(title + "（未配置收件人，未发送）");
                reportLogService.updateById(log);
                return "error：未配置收件人，请在系统设置-定时报表或邮件配置中填写收件人";
            }

            String res = WarnMailUtil.sendMail(receiver, title, content);
            if ("success".equals(res)) {
                log.setSendStatus("1");
                reportLogService.updateById(log);
                return "success";
            } else {
                log.setSendStatus("2");
                reportLogService.updateById(log);
                return "error：邮件发送失败，请检查邮件配置";
            }
        } catch (Exception e) {
            logger.error("生成/发送报表错误：", e);
            return "error：" + e.toString();
        }
    }

    /**
     * HTML 转义
     */
    private String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
