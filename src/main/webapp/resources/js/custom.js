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
    } else {
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
        drawBorder: false
    };
}

function customUserStatsBarChartExtender() {
    this.cfg.grid = {
        background: 'transparent',
        gridLineColor: '#a0a0a0',
        drawBorder: false
    };
    this.cfg.legend = {
        show: true,
        location: 'ne',
        placement: 'outsideGrid'
    };
    /*this.cfg.axes = {*/
    this.cfg.yaxis = {
        tickOptions: {
            formatString: '$%d'
        }
       /* }*/
    };
this.cfg.seriesDefaults = {
       pointLabels: {
            show: true,
            formatString: '%d',
            location: 's',
            ypadding: 5
        }
    };
}
function monthlyRevenueBarChartExtender() {
    this.cfg.grid = {
        background: 'transparent',
        gridLineColor: '#a0a0a0',
        drawBorder: false
    };
    this.cfg.legend = {
        show: true,
        location: 'ne',
        placement: 'outsideGrid'
    };
    /*this.cfg.axes = {*/
    this.cfg.yaxis = {
        tickOptions: {
            formatString: '$%d'
        }
       /* }*/
    };
this.cfg.seriesDefaults = {
       pointLabels: {
            show: true,
            formatString: '$%d',
            location: 's',
            ypadding: 5
        }
    };
}
/* $('#SessionTimetableListForm:j_idt11_container').load(function () {
 renderSchedule();
 });
 
 function renderSchedule() {
 var schedule = PF('timetable3Schedule');
 if (schedule) {
 var height = $(window).height();
 var width = $(window).width();
 var aspectRatio = 1.5;
 if (width > 320 && width < 600) {
 aspectRatio = 0.75;
 }
 if (width >= 600 && width <= 1200) {
 aspectRatio = 1;
 }
 if (width > 1200) {
 aspectRatio = 1.5;
 }
 schedule.defineProperty('option', 'aspectRatio', aspectRatio);
 $('#SessionTimetableListForm\:j_idt11_container').fullCalendar('option', 'aspectRatio', aspectRatio);
 
 alert('Changing aspect ratio to ' + aspectRatio);
 
 } else {
 console.warn("Couldn't find schedule");
 }
 isRunning = false;
 }     */
//background: '#FFF' ,