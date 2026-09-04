package com.wgcloud.task;


import cn.hutool.core.collection.CollectionUtil;
import com.wgcloud.config.CommonConfig;
import com.wgcloud.config.MailConfig;
import com.wgcloud.entity.*;
import com.wgcloud.mapper.*;
import com.wgcloud.service.*;
import com.wgcloud.util.DateUtil;
import com.wgcloud.util.RestUtil;
import com.wgcloud.util.jdbc.ConnectionUtil;
import com.wgcloud.util.jdbc.RDSConnection;
import com.wgcloud.util.msg.WarnMailUtil;
import com.wgcloud.util.msg.WarnPools;
import com.wgcloud.util.staticvar.BatchData;
import com.wgcloud.util.staticvar.StaticKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @version v2.3
 * @ClassName:ScheduledTask.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: ScheduledTask.java
 * @Copyright: 2017-2024 wgcloud. All rights reserved.
 */
@Component
public class ScheduledTask {

    private Logger logger = LoggerFactory.getLogger(ScheduledTask.class);

    /**
     * 线程池
     */
    static ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 40, 2, TimeUnit.MINUTES, new LinkedBlockingDeque<>());

    @Autowired
    SystemInfoService systemInfoService;
    @Autowired
    DeskStateService deskStateService;
    @Autowired
    LogInfoService logInfoService;
    @Autowired
    AppInfoService appInfoService;
    @Autowired
    CpuStateService cpuStateService;
    @Autowired
    MemStateService memStateService;
    @Autowired
    NetIoStateService netIoStateService;
    @Autowired
    SysLoadStateService sysLoadStateService;
    @Autowired
    TcpStateService tcpStateService;
    @Autowired
    AppStateService appStateService;
    @Autowired
    MailSetService mailSetService;
    @Autowired
    IntrusionInfoService intrusionInfoService;
    @Autowired
    HostInfoService hostInfoService;
    @Autowired
    DbInfoService dbInfoService;
    @Autowired
    DbTableService dbTableService;
    @Autowired
    DbTableCountService dbTableCountService;
    @Autowired
    HeathMonitorService heathMonitorService;
    @Autowired
    SystemConfigService systemConfigService;
    @Autowired
    private RestUtil restUtil;
    @Autowired
    ConnectionUtil connectionUtil;
    @Autowired
    CommonConfig commonConfig;
    @Autowired
    MailConfig mailConfig;

    /**
     * 20秒后执行
     * 初始化操作：加载邮件/Webhook 配置 + 加载数据库中的告警阈值覆盖 yml
     */
    @Scheduled(initialDelay = 20000L, fixedRate = 600 * 60 * 1000)
    public void initTask() {
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            List<MailSet> list = mailSetService.selectAllByParams(params);
            if (list.size() > 0) {
                StaticKeys.mailSet = list.get(0);
            }
        } catch (Exception e) {
            logger.error("初始化操作错误", e);
        }

        // 从 SYSTEM_CONFIG 读取告警阈值覆盖 application.yml 中的默认值
        // 逐项解析，避免单个配置值非法导致其余阈值全部加载失败
        loadWarnVal("cpuWarnVal", mailConfig::setCpuWarnVal);
        loadWarnVal("memWarnVal", mailConfig::setMemWarnVal);
        loadWarnVal("diskWarnVal", mailConfig::setDiskWarnVal);
        logger.info("告警阈值已加载：CPU={}%, 内存={}%, 磁盘={}%",
                mailConfig.getCpuWarnVal(), mailConfig.getMemWarnVal(), mailConfig.getDiskWarnVal());
    }

    /**
     * 从 SYSTEM_CONFIG 加载单个告警阈值，失败时保留 application.yml 中的默认值。
     * 配置项为空或不是合法数字时静默跳过，不抛出异常中断初始化。
     *
     * @param configKey 配置键
     * @param setter    目标阈值设置方法
     */
    private void loadWarnVal(String configKey, java.util.function.Consumer<Double> setter) {
        try {
            String value = systemConfigService.getValue(configKey, null);
            if (StringUtils.isEmpty(value)) {
                return;
            }
            double parsed = Double.parseDouble(value.trim());
            if (parsed < 0 || parsed > 100) {
                logger.warn("告警阈值 {} 取值 {} 超出 0-100 范围，已忽略", configKey, value);
                return;
            }
            setter.accept(parsed);
        } catch (Exception e) {
            logger.error("加载告警阈值 " + configKey + " 失败：", e);
        }
    }


    /**
     * 300秒后执行
     * 检测主机是否已经下线，检测进程是否下线
     */
    @Scheduled(initialDelay = 300000L, fixedRate = 20 * 60 * 1000)
    public void hostDownCheckTask() {
        Date date = DateUtil.getNowTime();
        long delayTime = 900 * 1000;

        try {
            Map<String, Object> params = new HashMap<String, Object>();
            List<SystemInfo> list = systemInfoService.selectAllByParams(params);
            if (!CollectionUtil.isEmpty(list)) {
                List<SystemInfo> updateList = new ArrayList<SystemInfo>();
                List<LogInfo> logInfoList = new ArrayList<LogInfo>();
                for (SystemInfo systemInfo : list) {

                    Date createTime = systemInfo.getCreateTime();
                    long diff = date.getTime() - createTime.getTime();
                    if (diff > delayTime) {
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(systemInfo.getId()))) {
                            continue;
                        }
                        systemInfo.setState(StaticKeys.DOWN_STATE);
                        LogInfo logInfo = new LogInfo();
                        logInfo.setHostname("主机下线：" + systemInfo.getHostname());
                        logInfo.setInfoContent("超过10分钟未上报状态，可能已下线：" + systemInfo.getHostname());
                        logInfo.setState(StaticKeys.LOG_ERROR);
                        logInfoList.add(logInfo);
                        updateList.add(systemInfo);
                        Runnable runnable = () -> {
                            WarnMailUtil.sendHostDown(systemInfo, true);
                        };
                        executor.execute(runnable);
                    } else {
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(systemInfo.getId()))) {
                            Runnable runnable = () -> {
                                WarnMailUtil.sendHostDown(systemInfo, false);
                            };
                            executor.execute(runnable);
                        }
                    }
                }
                if (updateList.size() > 0) {
                    systemInfoService.updateRecord(updateList);
                }
                if (logInfoList.size() > 0) {
                    logInfoService.saveRecord(logInfoList);
                }
            }
        } catch (Exception e) {
            logger.error("检测主机是否下线错误", e);
        }

        try {
            Map<String, Object> params = new HashMap<String, Object>();
            List<AppInfo> list = appInfoService.selectAllByParams(params);
            if (!CollectionUtil.isEmpty(list)) {
                List<AppInfo> updateList = new ArrayList<AppInfo>();
                List<LogInfo> logInfoList = new ArrayList<LogInfo>();
                for (AppInfo appInfo : list) {

                    Date createTime = appInfo.getCreateTime();
                    long diff = date.getTime() - createTime.getTime();
                    if (diff > delayTime) {
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(appInfo.getId()))) {
                            continue;
                        }
                        appInfo.setState(StaticKeys.DOWN_STATE);
                        LogInfo logInfo = new LogInfo();
                        logInfo.setHostname("进程下线IP：" + appInfo.getHostname() + "，名称：" + appInfo.getAppName());
                        logInfo.setInfoContent("超过10分钟未上报状态，可能已下线IP：" + appInfo.getHostname() + "，名称：" + appInfo.getAppName() + "，进程ID：" + appInfo.getAppPid());
                        logInfo.setState(StaticKeys.LOG_ERROR);
                        logInfoList.add(logInfo);
                        updateList.add(appInfo);
                        Runnable runnable = () -> {
                            WarnMailUtil.sendAppDown(appInfo, true);
                        };
                        executor.execute(runnable);
                    } else {
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(appInfo.getId()))) {
                            Runnable runnable = () -> {
                                WarnMailUtil.sendAppDown(appInfo, false);
                            };
                            executor.execute(runnable);
                        }
                    }
                }
                if (updateList.size() > 0) {
                    appInfoService.updateRecord(updateList);
                }
                if (logInfoList.size() > 0) {
                    logInfoService.saveRecord(logInfoList);
                }
            }
        } catch (Exception e) {
            logger.error("检测进程是否下线错误", e);
        }


    }


    /**
     * 90秒后执行，之后每隔10分钟执行, 单位：ms。
     * 检测心跳
     */
    @Scheduled(initialDelay = 90000L, fixedRateString = "${base.heathTimes}")
    public void heathMonitorTask() {
        logger.info("heathMonitorTask------------" + DateUtil.getDateTimeString(new Date()));
        Map<String, Object> params = new HashMap<>();
        List<HeathMonitor> heathMonitors = new ArrayList<HeathMonitor>();
        List<LogInfo> logInfoList = new ArrayList<LogInfo>();
        Date date = DateUtil.getNowTime();
        try {
            List<HeathMonitor> heathMonitorAllList = heathMonitorService.selectAllByParams(params);
            if (heathMonitorAllList.size() > 0) {
                for (HeathMonitor h : heathMonitorAllList) {
                    int status = 500;
                    // 根据监控类型选择检测方式：1 HTTP接口 2 TCP端口
                    if ("2".equals(h.getMonitorType())) {
                        if (StringUtils.isEmpty(h.getHostIp()) || h.getPort() == null) {
                            // TCP 监控配置不完整（多为升级前的历史数据），跳过检测。
                            // 若此时仍走 HTTP 分支，会因 heathUrl 为空而恒定返回异常，造成持续误报宕机。
                            logger.warn("TCP 监控配置不完整，跳过检测：{}，hostIp={}，port={}",
                                    h.getAppName(), h.getHostIp(), h.getPort());
                            continue;
                        }
                        int timeout = (h.getTimeoutMs() == null || h.getTimeoutMs() <= 0) ? 3000 : h.getTimeoutMs();
                        status = restUtil.checkTcp(h.getHostIp(), h.getPort(), timeout);
                    } else if (!StringUtils.isEmpty(h.getHeathUrl())) {
                        status = restUtil.get(h.getHeathUrl());
                    } else {
                        // HTTP 监控未配置接口地址，同样跳过，避免空 URL 请求恒定失败刷屏告警
                        logger.warn("HTTP 监控未配置接口地址，跳过检测：{}", h.getAppName());
                        continue;
                    }
                    h.setCreateTime(date);
                    h.setHeathStatus(status + "");
                    heathMonitors.add(h);
                    if (!"200".equals(h.getHeathStatus())) {
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(h.getId()))) {
                            continue;
                        }
                        String target = "2".equals(h.getMonitorType())
                                ? (h.getHostIp() + ":" + h.getPort())
                                : h.getHeathUrl();
                        LogInfo logInfo = new LogInfo();
                        logInfo.setHostname("服务接口检测异常：" + h.getAppName());
                        logInfo.setInfoContent("服务接口检测异常：" + h.getAppName() + "，" + target + "，返回状态" + h.getHeathStatus());
                        logInfo.setState(StaticKeys.LOG_ERROR);
                        logInfoList.add(logInfo);
                        Runnable runnable = () -> {
                            WarnMailUtil.sendHeathInfo(h, true);
                        };
                        executor.execute(runnable);
                    } else {
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(h.getId()))) {
                            Runnable runnable = () -> {
                                WarnMailUtil.sendHeathInfo(h, false);
                            };
                            executor.execute(runnable);
                        }
                    }
                }
                heathMonitorService.updateRecord(heathMonitors);
                if (logInfoList.size() > 0) {
                    logInfoService.saveRecord(logInfoList);
                }
            }
        } catch (Exception e) {
            logger.error("服务接口检测任务错误", e);
            logInfoService.save("服务接口检测错误", e.toString(), StaticKeys.LOG_ERROR);
        }
    }


    /**
     * 60秒后执行，之后每隔120分钟执行, 单位：ms。
     * 数据表监控
     */
    @Scheduled(initialDelay = 60000L, fixedRateString = "${base.dbTableTimes}")
    public void tableCountTask() {
        Map<String, Object> params = new HashMap<>();
        List<DbTable> dbTablesUpdate = new ArrayList<DbTable>();
        List<DbTableCount> dbTableCounts = new ArrayList<DbTableCount>();
        Date date = DateUtil.getNowTime();
        String sql = "";
        Long tableCount = 0l;
        try {
            List<DbInfo> dbInfos = dbInfoService.selectAllByParams(params);
            for (DbInfo dbInfo : dbInfos) {
                params.put("dbInfoId", dbInfo.getId());
                List<DbTable> dbTables = dbTableService.selectAllByParams(params);
                for (DbTable dbTable : dbTables) {
                    String whereAnd = "";
                    if (!StringUtils.isEmpty(dbTable.getWhereVal())) {
                        whereAnd = " and ";
                    }
                    if ("postgresql".equals(dbInfo.getDbType())) {
                        sql = RDSConnection.query_table_count_pg.replace("{tableName}", dbTable.getTableName()) + whereAnd + dbTable.getWhereVal();
                    } else {
                        sql = RDSConnection.query_table_count.replace("{tableName}", dbTable.getTableName()) + whereAnd + dbTable.getWhereVal();
                    }
                    tableCount = connectionUtil.queryTableCount(dbInfo, sql);
                    DbTableCount dbTableCount = new DbTableCount();
                    dbTableCount.setCreateTime(date);
                    dbTableCount.setDbTableId(dbTable.getId());
                    dbTableCount.setTableCount(tableCount);
                    dbTableCounts.add(dbTableCount);
                    dbTable.setDateStr(DateUtil.getDateTimeString(date));
                    dbTable.setTableCount(tableCount);
                    dbTablesUpdate.add(dbTable);
                }
            }
            if (dbTableCounts.size() > 0) {
                dbTableCountService.saveRecord(dbTableCounts);
                dbTableService.updateRecord(dbTablesUpdate);
            }
        } catch (Exception e) {
            logger.error("数据表监控任务错误", e);
            logInfoService.save("数据表监控任务错误", e.toString(), StaticKeys.LOG_ERROR);
        }
    }

    /**
     * 30秒后执行，之后每隔1分钟执行, 单位：ms。
     * 批量提交数据
     */
    @Scheduled(initialDelay = 30000L, fixedRate = 1 * 60 * 1000)
    public synchronized void commitTask() {
        logger.info("批量提交监控数据任务开始----------" + DateUtil.getCurrentDateTime());
        try {
            if (BatchData.APP_STATE_LIST.size() > 0) {
                List<AppState> APP_STATE_LIST = new ArrayList<AppState>();
                APP_STATE_LIST.addAll(BatchData.APP_STATE_LIST);
                BatchData.APP_STATE_LIST.clear();
                appStateService.saveRecord(APP_STATE_LIST);
            }
            if (BatchData.CPU_STATE_LIST.size() > 0) {
                List<CpuState> CPU_STATE_LIST = new ArrayList<CpuState>();
                CPU_STATE_LIST.addAll(BatchData.CPU_STATE_LIST);
                BatchData.CPU_STATE_LIST.clear();
                cpuStateService.saveRecord(CPU_STATE_LIST);
            }
            if (BatchData.MEM_STATE_LIST.size() > 0) {
                List<MemState> MEM_STATE_LIST = new ArrayList<MemState>();
                MEM_STATE_LIST.addAll(BatchData.MEM_STATE_LIST);
                BatchData.MEM_STATE_LIST.clear();
                memStateService.saveRecord(MEM_STATE_LIST);
            }
            if (BatchData.NETIO_STATE_LIST.size() > 0) {
                List<NetIoState> NETIO_STATE_LIST = new ArrayList<NetIoState>();
                NETIO_STATE_LIST.addAll(BatchData.NETIO_STATE_LIST);
                BatchData.NETIO_STATE_LIST.clear();
                netIoStateService.saveRecord(NETIO_STATE_LIST);
            }
            if (BatchData.SYSLOAD_STATE_LIST.size() > 0) {
                List<SysLoadState> SYSLOAD_STATE_LIST = new ArrayList<SysLoadState>();
                SYSLOAD_STATE_LIST.addAll(BatchData.SYSLOAD_STATE_LIST);
                BatchData.SYSLOAD_STATE_LIST.clear();
                sysLoadStateService.saveRecord(SYSLOAD_STATE_LIST);
            }
            if (BatchData.LOG_INFO_LIST.size() > 0) {
                List<LogInfo> LOG_INFO_LIST = new ArrayList<LogInfo>();
                LOG_INFO_LIST.addAll(BatchData.LOG_INFO_LIST);
                BatchData.LOG_INFO_LIST.clear();
                logInfoService.saveRecord(LOG_INFO_LIST);
            }
            if (BatchData.DESK_STATE_LIST.size() > 0) {
                Map<String, Object> paramsDel = new HashMap<String, Object>();

                List<DeskState> DESK_STATE_LIST = new ArrayList<DeskState>();
                DESK_STATE_LIST.addAll(BatchData.DESK_STATE_LIST);
                BatchData.DESK_STATE_LIST.clear();
                List<String> hostnameList = new ArrayList<String>();
                for (DeskState deskState : DESK_STATE_LIST) {
                    if (!hostnameList.contains(deskState.getHostname())) {
                        hostnameList.add(deskState.getHostname());
                    }
                }
                for (String hostname : hostnameList) {
                    paramsDel.put("hostname", hostname);
                    deskStateService.deleteByAccHname(paramsDel);
                }
                deskStateService.saveRecord(DESK_STATE_LIST);
            }
            if (BatchData.SYSTEM_INFO_LIST.size() > 0) {
                Map<String, Object> paramsDel = new HashMap<String, Object>();
                List<SystemInfo> SYSTEM_INFO_LIST = new ArrayList<SystemInfo>();
                SYSTEM_INFO_LIST.addAll(BatchData.SYSTEM_INFO_LIST);
                BatchData.SYSTEM_INFO_LIST.clear();
                List<SystemInfo> updateList = new ArrayList<SystemInfo>();
                List<SystemInfo> insertList = new ArrayList<SystemInfo>();
                List<SystemInfo> savedList = systemInfoService.selectAllByParams(paramsDel);
                for (SystemInfo systemInfo : SYSTEM_INFO_LIST) {
                    boolean issaved = false;
                    for (SystemInfo systemInfoS : savedList) {
                        if (systemInfoS.getHostname().equals(systemInfo.getHostname())) {
                            systemInfo.setId(systemInfoS.getId());
                            updateList.add(systemInfo);
                            issaved = true;
                            break;
                        }
                    }
                    if (!issaved) {
                        insertList.add(systemInfo);
                    }
                }
                systemInfoService.updateRecord(updateList);
                systemInfoService.saveRecord(insertList);
            }
            if (BatchData.APP_INFO_LIST.size() > 0) {
                Map<String, Object> paramsDel = new HashMap<String, Object>();
                List<AppInfo> APP_INFO_LIST = new ArrayList<AppInfo>();
                APP_INFO_LIST.addAll(BatchData.APP_INFO_LIST);
                BatchData.APP_INFO_LIST.clear();

                List<AppInfo> updateList = new ArrayList<AppInfo>();
                List<AppInfo> insertList = new ArrayList<AppInfo>();
                List<AppInfo> savedList = appInfoService.selectAllByParams(paramsDel);
                for (AppInfo systemInfo : APP_INFO_LIST) {
                    boolean issaved = false;
                    for (AppInfo systemInfoS : savedList) {
                        if (systemInfoS.getHostname().equals(systemInfo.getHostname()) && systemInfoS.getAppPid().equals(systemInfo.getAppPid())) {
                            systemInfo.setId(systemInfoS.getId());
                            updateList.add(systemInfo);
                            issaved = true;
                            break;
                        }
                    }
                    if (!issaved) {
                        insertList.add(systemInfo);
                    }
                }
                appInfoService.updateRecord(updateList);
                appInfoService.saveRecord(insertList);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("批量提交监控数据错误----------", e);
            logInfoService.save("commitTask", "批量提交监控数据错误：" + e.toString(), StaticKeys.LOG_ERROR);
        }
        logger.info("批量提交监控数据任务结束----------" + DateUtil.getCurrentDateTime());
    }

    @Autowired
    SystemInfoMapper systemInfoMapper;
    @Autowired
    CpuStateMapper cpuStateMapper;
    @Autowired
    DeskStateMapper deskStateMapper;
    @Autowired
    MemStateMapper memStateMapper;
    @Autowired
    NetIoStateMapper netIoStateMapper;
    @Autowired
    SysLoadStateMapper sysLoadStateMapper;
    @Autowired
    TcpStateMapper tcpStateMapper;
    @Autowired
    AppInfoMapper appInfoMapper;
    @Autowired
    AppStateMapper appStateMapper;
    @Autowired
    MailSetMapper mailSetMapper;
    @Autowired
    IntrusionInfoMapper intrusionInfoMapper;
    @Autowired
    LogInfoMapper logInfoMapper;

    /**
     * 每天凌晨1:10执行
     * 删除历史数据，15天
     */
    @Scheduled(cron = "0 10 1 * * ?")
    public void clearHisdataTask() {
        logger.info("定时清空历史数据任务开始----------" + DateUtil.getCurrentDateTime());
        WarnPools.clearOldData();//清空发告警邮件的记录
        String nowTime = DateUtil.getCurrentDateTime();
        //15天前时间
        String thrityDayBefore = DateUtil.getDateBefore(nowTime, 15);
        Map<String, Object> paramsDel = new HashMap<String, Object>();
        try {
            paramsDel.put(StaticKeys.SEARCH_END_TIME, thrityDayBefore);
            //执行删除操作begin
            if (paramsDel.get(StaticKeys.SEARCH_END_TIME) != null && !"".equals(paramsDel.get(StaticKeys.SEARCH_END_TIME))) {
                cpuStateMapper.deleteByAccountAndDate(paramsDel);//删除cpu监控信息
                deskStateMapper.deleteByAccountAndDate(paramsDel);//删除磁盘监控信息
                memStateMapper.deleteByAccountAndDate(paramsDel);//删除内存监控信息
                netIoStateMapper.deleteByAccountAndDate(paramsDel);//删除吞吐率监控信息
                sysLoadStateMapper.deleteByAccountAndDate(paramsDel);//删除负载状态监控信息
                tcpStateMapper.deleteByAccountAndDate(paramsDel);//删除tcp监控信息
                appStateMapper.deleteByDate(paramsDel);
                //删除15天前的日志信息
                logInfoMapper.deleteByDate(paramsDel);
                //删除15天前数据库表统计信息
                dbTableCountService.deleteByDate(paramsDel);

                logInfoService.save("定时清空历史数据完成", "定时清空历史数据完成：", StaticKeys.LOG_ERROR);
            }
            //执行删除操作end

        } catch (Exception e) {
            logger.error("定时清空历史数据任务出错：", e);
            logInfoService.save("定时清空历史数据错误", "定时清空历史数据错误：" + e.toString(), StaticKeys.LOG_ERROR);
        }
        logger.info("定时清空历史数据任务结束----------" + DateUtil.getCurrentDateTime());
    }


}
