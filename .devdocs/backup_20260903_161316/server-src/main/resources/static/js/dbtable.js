function view(id) {
    window.location.href = "/opsmonitor/dbTable/edit?id=" + id;
}


function add() {
    window.location.href = "/opsmonitor/dbTable/edit";
}

function del(id) {
    if (confirm('你确定要删除吗？')) {
        window.location.href = "/opsmonitor/dbTable/del?id=" + id;
    }
}

function viewChart(id) {
    window.location.href = "/opsmonitor/dbTable/viewChart?id=" + id;
}

function cancel() {
    history.back();
}
