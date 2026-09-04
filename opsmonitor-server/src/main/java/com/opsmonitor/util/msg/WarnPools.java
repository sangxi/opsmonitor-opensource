package com.opsmonitor.util.msg;

import java.util.HashMap;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:WarnPools.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: WarnPools.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public class WarnPools {


    /**
     * 存贮每天发送的内存告警信息map<主机名，1>
     */
    public static Map<String, String> MEM_WARN_MAP = new HashMap<String, String>();

    /**
     * 存贮每天发送的CPU告警信息map<主机名，1>
     * <p>
     * 必须与 MEM_WARN_MAP 分开：两者都以主机名为 key，若共用同一个 Map，
     * 内存告警一旦触发，同主机的 CPU 告警会被永久抑制（反之亦然）。
     */
    public static Map<String, String> CPU_WARN_MAP = new HashMap<String, String>();

    /**
     * 存贮每天发送的磁盘告警信息map<主机名+挂载点，1>
     * <p>
     * 一台主机可有多个分区，故 key 需带上挂载点，否则首个分区告警后会掩盖其余分区。
     */
    public static Map<String, String> DISK_WARN_MAP = new HashMap<String, String>();

    public static void clearOldData() {
        MEM_WARN_MAP.clear();
        CPU_WARN_MAP.clear();
        DISK_WARN_MAP.clear();
    }

}
