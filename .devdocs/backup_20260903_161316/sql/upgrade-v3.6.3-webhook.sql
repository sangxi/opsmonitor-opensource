-- OpsMonitor v3.6.3 升级脚本：钉钉/企业微信告警 + 端口监控
-- 用法：在已部署的 jk 库执行本脚本即可升级，无需重建表
-- 执行：mysql -h<host> -P<port> -u<user> -p<password> jk < upgrade-v3.6.3-webhook.sql

-- 1. MAIL_SET 表增加 Webhook 告警字段（钉钉/企业微信）
ALTER TABLE `MAIL_SET`
    ADD COLUMN `SEND_WEBHOOK` char(1) COLLATE utf8_unicode_ci DEFAULT '0' COMMENT '是否发送Webhook告警 1是0否' AFTER `HEATH_PER`,
    ADD COLUMN `DINGDING_WEBHOOK` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '钉钉机器人Webhook地址' AFTER `SEND_WEBHOOK`,
    ADD COLUMN `DINGDING_SECRET` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '钉钉机器人加签密钥' AFTER `DINGDING_WEBHOOK`,
    ADD COLUMN `WEIXIN_WEBHOOK` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '企业微信群机器人Webhook地址' AFTER `DINGDING_SECRET`;

-- 2. HEATH_MONITOR 表增加端口监控字段（监控类型、目标主机、端口、超时）
ALTER TABLE `HEATH_MONITOR`
    ADD COLUMN `MONITOR_TYPE` char(1) COLLATE utf8_unicode_ci DEFAULT '1' COMMENT '监控类型 1 HTTP接口 2 TCP端口' AFTER `APP_NAME`,
    ADD COLUMN `HOST_IP` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '目标主机IP（TCP监控用）' AFTER `MONITOR_TYPE`,
    ADD COLUMN `PORT` int(11) DEFAULT NULL COMMENT '目标端口（TCP监控用）' AFTER `HOST_IP`,
    ADD COLUMN `TIMEOUT_MS` int(11) DEFAULT 3000 COMMENT '连接超时毫秒' AFTER `PORT`;

-- 3. SYSTEM_CONFIG 表增加告警阈值配置项（若不存在则插入）
INSERT IGNORE INTO `SYSTEM_CONFIG` (`ID`, `CONFIG_KEY`, `CONFIG_VALUE`, `CONFIG_DESC`, `CREATE_TIME`) VALUES
    ('cfg_cpu_warn_threshold', 'cpuWarnVal', '95', 'CPU使用率告警阈值(%)', NOW()),
    ('cfg_mem_warn_threshold', 'memWarnVal', '90', '内存使用率告警阈值(%)', NOW()),
    ('cfg_disk_warn_threshold', 'diskWarnVal', '90', '磁盘使用率告警阈值(%)', NOW());
