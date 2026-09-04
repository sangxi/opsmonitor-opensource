function searchByPara() {
    var account = $("#account").val();
    window.location.href = "/opsmonitor/log/list?account=" + escape(escape(account));
}

function view(id) {
    window.location.href = "/opsmonitor/log/view?id=" + id;
}

function cancel() {
    history.back();
}
