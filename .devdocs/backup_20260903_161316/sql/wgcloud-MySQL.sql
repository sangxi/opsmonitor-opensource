/* ============================================================
 * OpsMonitor (wgcloud v3.6.3 二开版) 全量初始化脚本 - MySQL
 * 特性：
 *   1. 自动建库（utf8mb4），可在空实例上直接执行
 *   2. 幂等：重复执行会先 DROP 再建表（注意：会清空已有数据）
 *   3. 已包含 v3.6.3 全部字段（Webhook 告警 / TCP 端口监控 / 磁盘告警）
 *      全新部署无需再执行 upgrade-v3.6.3-webhook.sql
 * 导入方式：
 *   mysql --default-character-set=utf8mb4 -h<host> -P<port> -u<user> -p < 本文件
 * ============================================================ */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

CREATE DATABASE IF NOT EXISTS `wgcloud` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wgcloud`;


-- ----------------------------
-- Table structure for app_info
-- ----------------------------
DROP TABLE IF EXISTS `APP_INFO`;
CREATE TABLE `APP_INFO` (
  `ID` char(32) NOT NULL,
  `HOST_NAME` char(30) DEFAULT NULL,
  `APP_PID` char(200) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  `APP_NAME` varchar(50) DEFAULT NULL,
  `CPU_PER` double(8,2) DEFAULT NULL,
  `MEM_PER` double(10,2) DEFAULT NULL,
  `APP_TYPE` char(1) DEFAULT NULL,
  `STATE` char(1) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for app_state
-- ----------------------------
DROP TABLE IF EXISTS `APP_STATE`;
CREATE TABLE `APP_STATE` (
  `ID` char(32) NOT NULL,
  `APP_INFO_ID` char(32) DEFAULT NULL,
  `CPU_PER` double(8,2) DEFAULT NULL,
  `MEM_PER` double(10,2) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `APP_STAT_INDEX` (`APP_INFO_ID`,`CREATE_TIME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for cpu_state
-- ----------------------------
DROP TABLE IF EXISTS `CPU_STATE`;
CREATE TABLE `CPU_STATE` (
  `ID` char(32) NOT NULL,
  `HOST_NAME` char(30) DEFAULT NULL,
  `USER` char(30) DEFAULT NULL,
  `SYS` double(8,2) DEFAULT NULL,
  `IDLE` double(8,2) DEFAULT NULL,
  `IOWAIT` double(8,2) DEFAULT NULL,
  `IRQ` char(30) DEFAULT NULL,
  `SOFT` char(30) DEFAULT NULL,
  `DATE_STR` char(30) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `CPU_ACC_HOST_INDEX` (`HOST_NAME`,`CREATE_TIME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for db_info
-- ----------------------------
DROP TABLE IF EXISTS `DB_INFO`;
CREATE TABLE `DB_INFO` (
  `ID` char(32) NOT NULL,
  `DBTYPE` char(32) DEFAULT NULL,
  `USER` varchar(50) DEFAULT NULL,
  `PASSWD` varchar(50) DEFAULT NULL,
  `IP` char(20) DEFAULT NULL,
  `PORT` char(10) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `DBNAME` char(50) DEFAULT NULL,
  `DB_STATE` char(1) DEFAULT NULL,
  `ALIAS_NAME` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for db_table
-- ----------------------------
DROP TABLE IF EXISTS `DB_TABLE`;
CREATE TABLE `DB_TABLE` (
  `ID` char(32) NOT NULL,
  `TABLE_NAME` varchar(50) DEFAULT NULL,
  `WHERE_VAL` varchar(200) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `REMARK` varchar(50) DEFAULT NULL,
  `TABLE_COUNT` bigint(20) DEFAULT NULL,
  `DATE_STR` char(30) DEFAULT NULL,
  `DBINFO_ID` char(32) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for db_table_count
-- ----------------------------
DROP TABLE IF EXISTS `DB_TABLE_COUNT`;
CREATE TABLE `DB_TABLE_COUNT` (
  `ID` char(32) NOT NULL,
  `DB_TABLE_ID` char(32) DEFAULT NULL,
  `TABLE_COUNT` bigint(20) DEFAULT NULL,
  `DATE_STR` char(30) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for desk_state
-- ----------------------------
DROP TABLE IF EXISTS `DESK_STATE`;
CREATE TABLE `DESK_STATE` (
  `ID` char(32) NOT NULL,
  `HOST_NAME` char(30) DEFAULT NULL,
  `FILE_STSTEM` char(50) DEFAULT NULL,
  `SIZE` char(30) DEFAULT NULL,
  `USED` char(30) DEFAULT NULL,
  `AVAIL` char(30) DEFAULT NULL,
  `USE_PER` char(10) DEFAULT NULL,
  `DATE_STR` char(30) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `DESK_ACC_HOST_INDEX` (`HOST_NAME`,`CREATE_TIME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for heath_monitor
-- ----------------------------
DROP TABLE IF EXISTS `HEATH_MONITOR`;
CREATE TABLE `HEATH_MONITOR` (
  `ID` char(32) NOT NULL,
  `APP_NAME` char(50) DEFAULT NULL,
  `MONITOR_TYPE` char(1) DEFAULT '1' COMMENT '监控类型 1 HTTP 2 TCP',
  `HOST_IP` varchar(100) DEFAULT NULL COMMENT 'TCP目标主机IP',
  `PORT` int(11) DEFAULT NULL COMMENT 'TCP目标端口',
  `TIMEOUT_MS` int(11) DEFAULT 3000 COMMENT 'TCP连接超时毫秒',
  `HEATH_URL` varchar(255) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `HEATH_STATUS` char(10) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for host_info
-- ----------------------------
DROP TABLE IF EXISTS `HOST_INFO`;
CREATE TABLE `HOST_INFO` (
  `ID` char(32) NOT NULL,
  `IP` char(30) DEFAULT NULL,
  `PORT` char(20) DEFAULT NULL,
  `ROOT` char(50) DEFAULT NULL,
  `PASSWD` char(50) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `REMARK` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for intrusion_info
-- ----------------------------
DROP TABLE IF EXISTS `INTRUSION_INFO`;
CREATE TABLE `INTRUSION_INFO` (
  `ID` char(32) NOT NULL,
  `HOST_NAME` char(30) DEFAULT NULL,
  `LSMOD` text,
  `PASSWD_INFO` varchar(100) DEFAULT NULL,
  `CRONTAB` text,
  `PROMISC` varchar(100) DEFAULT NULL,
  `RPCINFO` text,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for log_info
-- ----------------------------
DROP TABLE IF EXISTS `LOG_INFO`;
CREATE TABLE `LOG_INFO` (
  `ID` char(32) NOT NULL,
  `HOST_NAME` char(50) DEFAULT NULL,
  `INFO_CONTENT` text,
  `STATE` char(1) DEFAULT NULL,
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for mail_set
-- ----------------------------
DROP TABLE IF EXISTS `MAIL_SET`;
CREATE TABLE `MAIL_SET` (
  `ID` char(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SEND_MAIL` char(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FROM_MAIL_NAME` char(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FROM_PWD` char(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SMTP_HOST` char(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SMTP_PORT` char(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SMTP_SSL` char(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TO_MAIL` char(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CPU_PER` char(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  `MEM_PER` char(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `HEATH_PER` char(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SEND_WEBHOOK` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0',
  `DINGDING_WEBHOOK` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DINGDING_SECRET` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `WEIXIN_WEBHOOK` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for mem_state
-- ----------------------------
DROP TABLE IF EXISTS `MEM_STATE`;
CREATE TABLE `MEM_STATE` (
  `ID` char(32) NOT NULL,
  `HOST_NAME` char(30) DEFAULT NULL,
  `TOTAL` char(30) DEFAULT NULL,
  `USED` char(30) DEFAULT NULL,
  `FREE` char(30) DEFAULT NULL,
  `USE_PER` double(8,2) DEFAULT NULL,
  `DATE_STR` char(30) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `MEM_ACC_HOST_INDEX` (`HOST_NAME`,`CREATE_TIME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for netio_state
-- ----------------------------
DROP TABLE IF EXISTS `NETIO_STATE`;
CREATE TABLE `NETIO_STATE` (
  `ID` char(32) NOT NULL,
  `HOST_NAME` char(30) DEFAULT NULL,
  `RXPCK` char(30) DEFAULT NULL,
  `TXPCK` char(30) DEFAULT NULL,
  `RXBYT` char(30) DEFAULT NULL,
  `TXBYT` char(30) DEFAULT NULL,
  `RXCMP` char(30) DEFAULT NULL,
  `TXCMP` char(30) DEFAULT NULL,
  `RXMCST` char(30) DEFAULT NULL,
  `DATE_STR` char(30) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `NETIO_ACC_HOST_INDEX` (`HOST_NAME`,`CREATE_TIME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for system_info
-- ----------------------------
DROP TABLE IF EXISTS `SYSTEM_INFO`;
CREATE TABLE `SYSTEM_INFO` (
  `ID` char(32) NOT NULL,
  `HOST_NAME` char(30) DEFAULT NULL,
  `VERSION` char(100) DEFAULT NULL,
  `VERSION_DETAIL` char(200) DEFAULT NULL,
  `CPU_PER` double(8,2) DEFAULT NULL,
  `MEM_PER` double(8,2) DEFAULT NULL,
  `CPU_CORE_NUM` char(10) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  `CPU_XH` char(150) DEFAULT NULL,
  `STATE` char(1) DEFAULT NULL,
  `REMARK` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for sys_load_state
-- ----------------------------
DROP TABLE IF EXISTS `SYS_LOAD_STATE`;
CREATE TABLE `SYS_LOAD_STATE` (
  `ID` char(32) NOT NULL,
  `HOST_NAME` char(30) DEFAULT NULL,
  `ONE_LOAD` double(8,2) DEFAULT NULL,
  `FIVE_LOAD` double(8,2) DEFAULT NULL,
  `FIFTEEN_LOAD` double(8,2) DEFAULT NULL,
  `USERS` char(10) DEFAULT NULL,
  `DATE_STR` char(30) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `LOAD_ACC_HOST_INDEX` (`HOST_NAME`,`CREATE_TIME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for tcp_state
-- ----------------------------
DROP TABLE IF EXISTS `TCP_STATE`;
CREATE TABLE `TCP_STATE` (
  `ID` char(32) NOT NULL,
  `HOST_NAME` char(30) DEFAULT NULL,
  `ACTIVE` char(30) DEFAULT NULL,
  `PASSIVE` char(30) DEFAULT NULL,
  `RETRANS` char(30) DEFAULT NULL,
  `DATE_STR` char(30) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `TCP_ACC_HOST_INDEX` (`HOST_NAME`,`CREATE_TIME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `SYSTEM_CONFIG`;
CREATE TABLE `SYSTEM_CONFIG` (
  `ID` char(32) NOT NULL,
  `CONFIG_KEY` varchar(100) DEFAULT NULL,
  `CONFIG_VALUE` varchar(500) DEFAULT NULL,
  `CONFIG_DESC` varchar(200) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_CONFIG_KEY` (`CONFIG_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of system_config
-- ----------------------------
INSERT INTO `SYSTEM_CONFIG` (`ID`, `CONFIG_KEY`, `CONFIG_VALUE`, `CONFIG_DESC`, `CREATE_TIME`) VALUES
('cfg_system_name',      'systemName',      'OpsMonitor运维监控系统',           '系统名称（浏览器标题、登录页）',       NOW()),
('cfg_system_shortname', 'systemShortName', 'OpsMonitor',                       '系统简称（左侧导航 Logo 旁）',          NOW()),
('cfg_copyright',        'copyright',       '©2026 OpsMonitor. All Rights Reserved.', '版权信息（登录页底部）',         NOW()),
('cfg_cpu_warn',         'cpuWarnVal',      '95',                               'CPU使用率告警阈值(%)',                 NOW()),
('cfg_mem_warn',         'memWarnVal',      '90',                               '内存使用率告警阈值(%)',                NOW()),
('cfg_disk_warn',        'diskWarnVal',     '90',                               '磁盘使用率告警阈值(%)',                NOW());
