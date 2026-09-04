-- =====================================================================
-- OpsMonitor v2.0 功能增强增量脚本（M5）
-- 说明：老版本升级到 v2.0 时执行本脚本；全新部署执行 opsmonitor-MySQL.sql + opsmonitor-help.sql + 本脚本
-- 本脚本仅新增表，不影响已有表结构，可重复执行（已用 IF NOT EXISTS 保护）
-- =====================================================================

-- ----------------------------
-- 1. 告警中心：告警记录表 ALARM_RECORD
--    统一记录内存/CPU/磁盘/主机下线/进程下线/服务接口/入侵检测等全部告警，
--    支持状态流转（0未处理/1已处理/2已忽略）与告警级别（info/warn/error）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ALARM_RECORD` (
  `ID` char(32) NOT NULL,
  `ALARM_TYPE` char(20) DEFAULT NULL COMMENT '告警类型：cpu/mem/disk/hostdown/appdown/heath/intrusion',
  `HOSTNAME` char(100) DEFAULT NULL COMMENT '告警对象主机名',
  `TITLE` varchar(200) DEFAULT NULL COMMENT '告警标题',
  `CONTENT` text COMMENT '告警内容',
  `LEVEL` char(10) DEFAULT 'warn' COMMENT '级别：info/warn/error',
  `STATE` char(1) DEFAULT '0' COMMENT '处理状态：0未处理 1已处理 2已忽略',
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  `UPDATE_TIME` timestamp NULL DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`ID`),
  KEY `ALARM_STATE_INDEX` (`STATE`),
  KEY `ALARM_TYPE_INDEX` (`ALARM_TYPE`),
  KEY `ALARM_TIME_INDEX` (`CREATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警中心记录表';

-- ----------------------------
-- 2. 操作审计：AUDIT_LOG 表
--    记录登录、配置变更、导出、指令下发等操作轨迹
-- ----------------------------
CREATE TABLE IF NOT EXISTS `AUDIT_LOG` (
  `ID` char(32) NOT NULL,
  `ACCOUNT` char(50) DEFAULT NULL COMMENT '操作账号',
  `ACTION` char(50) DEFAULT NULL COMMENT '操作类型：login/logout/config/export/api/other',
  `CONTENT` varchar(500) DEFAULT NULL COMMENT '操作内容',
  `IP` char(50) DEFAULT NULL COMMENT '来源IP',
  `RESULT` char(10) DEFAULT 'success' COMMENT '结果：success/fail',
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `AUDIT_ACTION_INDEX` (`ACTION`),
  KEY `AUDIT_TIME_INDEX` (`CREATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

-- ----------------------------
-- 3. 开放 API 访问令牌表 OPEN_API_TOKEN
--    用于第三方系统通过 REST API 接入（独立于网页登录）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `OPEN_API_TOKEN` (
  `ID` char(32) NOT NULL,
  `TOKEN` char(64) NOT NULL COMMENT 'API 访问令牌',
  `NAME` varchar(100) DEFAULT NULL COMMENT '令牌名称',
  `STATUS` char(1) DEFAULT '1' COMMENT '状态：1启用 0停用',
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  `EXPIRE_TIME` timestamp NULL DEFAULT NULL COMMENT '过期时间，空为永不过期',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `API_TOKEN_UNIQUE` (`TOKEN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放 API 访问令牌';

-- 默认注入一个演示令牌（请部署后登录管理页面修改或删除）
INSERT INTO `OPEN_API_TOKEN` (`ID`,`TOKEN`,`NAME`,`STATUS`,`CREATE_TIME`)
SELECT 'apitoken_default', 'OpsMonitorApiToken2026', '默认演示令牌', '1', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `OPEN_API_TOKEN` WHERE `TOKEN`='OpsMonitorApiToken2026');
