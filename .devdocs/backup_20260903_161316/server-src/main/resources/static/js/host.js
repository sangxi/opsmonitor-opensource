function searchByPara() {
    var account = $("#account").val();
    window.location.href = "/opsmonitor/log/list?account=" + escape(escape(account));
}

function viewDashView(id) {
    window.location.href = "/opsmonitor/dash/detail?dashView=1&id=" + id;
}

function viewChartDashView(id) {
    window.location.href = "/opsmonitor/dash/chart?dashView=1&id=" + id;
}

function viewDatetDashView(id, dates) {
    window.location.href = "/opsmonitor/dash/chart?dashView=1&id=" + id + "&date=" + dates;
}

function view(id) {
    window.location.href = "/opsmonitor/dash/detail?id=" + id;
}

function viewChart(id) {
    window.location.href = "/opsmonitor/dash/chart?id=" + id;
}

function del(id) {
    if (confirm('你确定要删除吗？')) {
        window.location.href = "/opsmonitor/dash/del?id=" + id;
    }
}


function viewDate(id, dates) {
    window.location.href = "/opsmonitor/dash/chart?id=" + id + "&date=" + dates;
}

function viewApps(hostname) {
    window.location.href = "/opsmonitor/appInfo/list?hostname=" + hostname;
}

function ajaxSaveRemark() {
    $("#form2").ajaxSubmit(function (message) {
        window.location.href = window.location.href;
    });
}

function setHostRemark(id, hostRemark) {
    $("#id").val(id);
    $("#remark").val(hostRemark);
}

function cancel() {
    history.back();
}

function addHost() {
    var hostname = $.trim($("#addHostname").val());
    if (!hostname) {
        alert("请输入主机IP / 名称");
        return;
    }
    $.ajax({
        type: "POST",
        url: "/opsmonitor/host/add",
        data: {
            hostname: hostname,
            remark: $("#addRemark").val()
        },
        success: function (res) {
            if (res && String(res).indexOf("success") === 0) {
                window.location.reload();
            } else {
                alert(res || "新增主机失败");
            }
        },
        error: function () {
            alert("新增主机失败，请重试");
        }
    });
}
