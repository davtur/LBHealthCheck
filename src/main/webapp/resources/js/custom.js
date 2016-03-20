function handleSubmit(args, dialog) {
    var jqDialog = jQuery('#' + dialog);
    if (args.validationFailed) {
        jqDialog.effect('shake', {times: 3}, 100);
    } else {
        PF(dialog).hide();
    }
}

function handleCreateRequest(xhr, status, args, dialog, listTable) {
    var jqDialog = jQuery('#' + dialog);
    if (args.validationFailed) {
        jqDialog.jq.effect("shake", {times: 5}, 100);
    }
    else {
        PF(dialog).hide();
        PF(listTable).filter();
    }
}

function handlePollComplete(xhr, status, args, poller) {
    if (args.stopPolling) {
        PF(poller).stop();
        //alert('Stopping Poller');
    }
}

function customUserStatsLineChartExtender() {
    this.cfg.grid = {
        background: 'transparent',
        gridLineColor: '#a0a0a0',
        drawBorder: false,
    }
}

function customUserStatsBarChartExtender() {
    this.cfg.grid = {
        background: 'transparent',
        gridLineColor: '#a0a0a0',
        drawBorder: false,
    }
}

//background: '#FFF' ,