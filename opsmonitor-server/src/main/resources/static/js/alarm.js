// OpsMonitor 告警中心 JS
// 详情查看
function viewDetail(id) {
    // 从当前页面列表行的数据源取详情——使用 AJAX 获取
    $.ajax({
        url: "/opsmonitor/alarm/detail?id=" + id,
        type: "GET",
        dataType: "json",
        success: function (data) {
            if (data.code === 0) {
                $("#detailTitle").text(data.title);
                var content = data.content || "";
                var host = data.hostname || "-";
                var time = data.createTime || "-";
                var typeMap = {cpu:"CPU 告警", mem:"内存告警", disk:"磁盘告警", hostdown:"主机下线", appdown:"进程下线", heath:"服务接口", intrusion:"入侵检测"};
                var typeName = typeMap[data.alarmType] || data.alarmType || "-";
                var html = '<div style="margin-bottom:12px;">' +
                    '<span style="display:inline-block;min-width:70px;color:#94a3b8;">告警类型</span><span>' + typeName + '</span></div>' +
                    '<div style="margin-bottom:12px;">' +
                    '<span style="display:inline-block;min-width:70px;color:#94a3b8;">主机</span><span>' + host + '</span></div>' +
                    '<div style="margin-bottom:12px;">' +
                    '<span style="display:inline-block;min-width:70px;color:#94a3b8;">时间</span><span>' + time + '</span></div>' +
                    '<div style="border-top:1px solid rgba(148,163,184,0.15);padding-top:12px;">' + content + '</div>';
                $("#detailContent").html(html);
                $("#detailModal").modal("show");
            } else {
                toastr.error("获取告警详情失败");
            }
        },
        error: function () {
            toastr.error("获取告警详情失败");
        }
    });
}

// 处理告警（标记已处理/忽略）
function handle(id, state) {
    var tip = state === "1" ? "标记该告警为已处理？" : "忽略该告警？";
    if (confirm(tip)) {
        window.location.href = "/opsmonitor/alarm/handle?id=" + id + "&state=" + state;
    }
}

// 删除告警
function del(id) {
    if (confirm("确认删除该告警记录？")) {
        window.location.href = "/opsmonitor/alarm/del?id=" + id;
    }
}

// 批量处理全部
function handleAll() {
    if (confirm("确认将全部未处理告警标记为已处理？")) {
        window.location.href = "/opsmonitor/alarm/handleAll";
    }
}

// 回到列表
function cancel() {
    history.back();
}
