function add() {
    window.location.href = "/opsmonitor/heathMonitor/edit";
}

function view(id) {
    window.location.href = "/opsmonitor/heathMonitor/view?id=" + id;
}

function del(id) {
    if (confirm('你确定要删除吗？')) {
        window.location.href = "/opsmonitor/heathMonitor/del?id=" + id;
    }
}

function edit(id) {
    window.location.href = "/opsmonitor/heathMonitor/edit?id=" + id;
}

function cancel() {
    history.back();
}
