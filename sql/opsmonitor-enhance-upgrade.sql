-- ============================================================
-- OpsMonitor v2.0 功能增强增量升级脚本（主机分组标签 + 定时报表）
-- 适用：已部署过 M5 及更早版本的存量库
-- 特性：
--   1. 为 SYSTEM_INFO 增加 DISK_PER / GROUP_NAME / TAGS 字段（幂等，列已存在则跳过）
--   2. 新增 REPORT_LOG 表（定时报表记录）
--   3. 写入报表相关 SYSTEM_CONFIG 配置项（幂等）
--   4. 兼容 MySQL 5.7 / 8.0
-- 导入方式：
--   mysql --default-character-set=utf8mb4 -h<host> -P<port> -u<user> -p opsmonitor < 本文件
-- ============================================================

SET NAMES utf8mb4;

-- 1. 为 SYSTEM_INFO 增加列（列已存在则跳过）
DROP PROCEDURE IF EXISTS `om_add_col_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE om_add_col_if_not_exists(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(255))
BEGIN
    IF NOT EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col
    ) THEN
        SET @om_sql = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN ', ddl);
        PREPARE om_stmt FROM @om_sql;
        EXECUTE om_stmt;
        DEALLOCATE PREPARE om_stmt;
    END IF;
END$$
DELIMITER ;

CALL om_add_col_if_not_exists('SYSTEM_INFO', 'DISK_PER', '`DISK_PER` double(8,2) DEFAULT NULL');
CALL om_add_col_if_not_exists('SYSTEM_INFO', 'GROUP_NAME', '`GROUP_NAME` varchar(64) DEFAULT NULL');
CALL om_add_col_if_not_exists('SYSTEM_INFO', 'TAGS', '`TAGS` varchar(255) DEFAULT NULL');

DROP PROCEDURE IF EXISTS `om_add_col_if_not_exists`;

-- 2. 新增 REPORT_LOG 表（定时报表记录）
CREATE TABLE IF NOT EXISTS `REPORT_LOG` (
  `ID` char(32) NOT NULL,
  `REPORT_TYPE` varchar(20) DEFAULT NULL COMMENT 'daily=每日 weekly=每周 manual=手动',
  `TITLE` varchar(200) DEFAULT NULL,
  `CONTENT` mediumtext COMMENT 'HTML 报表正文',
  `SEND_STATUS` char(1) DEFAULT '0' COMMENT '0未发送 1已发送 2发送失败',
  `RECEIVER` varchar(500) DEFAULT NULL,
  `CREATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_REPORT_TYPE` (`REPORT_TYPE`),
  KEY `IDX_REPORT_TIME` (`CREATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 写入报表相关系统配置（幂等）
INSERT INTO `SYSTEM_CONFIG` (`ID`, `CONFIG_KEY`, `CONFIG_VALUE`, `CONFIG_DESC`, `CREATE_TIME`) VALUES
('cfg_report_enable', 'reportEnable', '1',   '定时报表开关(1开 0关)', NOW()),
('cfg_report_mail',   'reportMail',   '',    '报表收件人(分号分隔,留空则用邮件配置收件人)', NOW()),
('cfg_report_time',   'reportTime',   '08:30', '每日报表发送时间(24小时制 HH:mm)', NOW())
ON DUPLICATE KEY UPDATE CONFIG_DESC = VALUES(CONFIG_DESC);

-- ============================================================
-- 完成提示：可用以下语句核对
--   SELECT CONFIG_KEY,CONFIG_VALUE,CONFIG_DESC FROM SYSTEM_CONFIG WHERE CONFIG_KEY LIKE 'report%';
--   SHOW COLUMNS FROM SYSTEM_INFO;
-- ============================================================
