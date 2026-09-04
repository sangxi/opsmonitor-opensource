function view(id) {
    window.location.href = "/opsmonitor/appInfo/view?id=" + id;
}

function del(id) {
    window.location.href = "/opsmonitor/appInfo/del?id=" + id;
}

function cancel() {
    history.back();
}