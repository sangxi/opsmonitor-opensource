function saveReportCfg() {
    var data = {
        reportEnable: $("#reportEnable").is(":checked") ? "1" : "0",
        reportTime: $("#reportTime").val(),
        reportMail: $("#reportMail").val()
    };
    $.ajax({
        type: "POST",
        url: "/opsmonitor/report/saveConfig",
        data: data,
        success: function (res) {
            if (res && String(res).indexOf("success") === 0) {
                alert("报表配置已保存");
            } else {
                alert(res || "保存失败");
            }
        },
        error: function () {
            alert("保存失败，请重试");
        }
    });
}

function sendReport(type) {
    if (!confirm("确认立即生成并发送" + (type === "daily" ? "每日" : type === "weekly" ? "每周" : "") + "报表吗？")) {
        return;
    }
    $.ajax({
        type: "POST",
        url: "/opsmonitor/report/send",
        data: { reportType: type },
        success: function (res) {
            if (res && String(res).indexOf("success") === 0) {
                alert("报表已发送");
                window.location.reload();
            } else {
                alert(res || "发送失败");
            }
        },
        error: function () {
            alert("发送失败，请重试");
        }
    });
}

function preview(id) {
    $.ajax({
        type: "GET",
        url: "/opsmonitor/report/detail",
        data: { id: id },
        dataType: "json",
        success: function (res) {
            if (res && res.code === 0) {
                $("#reportViewTitle").text(res.title);
                $("#reportViewBody").html(res.content || "<p class='text-muted'>暂无内容</p>");
                $("#modal-report-view").modal("show");
            } else {
                alert("报表不存在");
            }
        },
        error: function () {
            alert("预览失败，请重试");
        }
    });
}

function del(id) {
    if (confirm('你确定要删除该报表记录吗？')) {
        window.location.href = "/opsmonitor/report/del?id=" + id;
    }
}
