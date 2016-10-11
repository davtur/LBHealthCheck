/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.chartbeans;

import au.com.manlyit.fitnesscrm.stats.beans.ExpensesFacade;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentsFacade;
import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentStatus;
import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import au.com.manlyit.fitnesscrm.stats.classes.SessionHistoryController;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Expenses;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import javax.inject.Inject;
import javax.faces.context.FacesContext;

/**
 *
 * @author david
 */
import javax.inject.Named;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.el.ELException;
import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.BarChartSeries;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.LinearAxis;

@Named("mySessionsChart1")
@SessionScoped
public class MySessionsChart1 implements Serializable {

    private static final Logger logger = Logger.getLogger(MySessionsChart1.class.getName());
    private BarChartModel model;
    private BarChartModel model2;
    private BarChartModel customModel;
    private BarChartModel dashboardMonthlyRevenueModel;
    private BarChartModel dashboardMonthlySessionsModel;

    private Customers selectedCustomer;

    private Date startDate;
    private Date endDate;
    private int dateInterval = 1;
    private boolean showAllUsers = false;

    private String dashboardXAxisLabel = "Month";
    private Date dashboardStartDate;
    private Date dashboardEndDate;
    private int dashboardDateInterval = 2;
    private boolean dashboardShowAllUsers = true;

    @PostConstruct
    private void initDates() {
        GregorianCalendar cal1 = new GregorianCalendar();
        cal1.add(Calendar.DAY_OF_YEAR, 1);
        setEndDate(cal1.getTime());
        cal1.add(Calendar.DAY_OF_YEAR, -1);
        cal1.add(Calendar.MONTH, -2);
        setStartDate(cal1.getTime());

        cal1.setTime(zeroTimeField(new Date()));
        cal1.add(Calendar.MONTH, -8);
        cal1.set(Calendar.DAY_OF_MONTH, 1);
        setDashboardStartDate(cal1.getTime());
        cal1.add(Calendar.YEAR, 1);
        setDashboardEndDate(cal1.getTime());
    }

    /**
     * Creates a new instance of MeasurementsChart
     */
    public MySessionsChart1() {
    }
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ParticipantsFacade ejbParticipantsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionTypesFacade ejbSessionTypesFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade ejbSessionHistoryFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.GroupsFacade ejbGroupsFacade;
    @Inject
    private PaymentsFacade paymentsFacade;
    @Inject
    private ExpensesFacade expensesFacade;

    //@PostConstruct
    /* public void createChart() {
     SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
     FacesContext context = FacesContext.getCurrentInstance();
     Customers loggedInUser = null;
     try {
     //loggedInUser = ejbCustomerFacade.findCustomerByUsername(context.getExternalContext().getRemoteUser());

     CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
     loggedInUser = custController.getSelected();

     } catch (ELException e) {
     JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't find the customer in the database.");
     }
     ChartSeries groupSessionSeries = new ChartSeries();
     groupSessionSeries.setLabel("Group Sessions");
     ChartSeries ptSessionSeries = new ChartSeries();
     ptSessionSeries.setLabel("PT Sessions");
     int weeksToDisplay = 26;
     GregorianCalendar cal = new GregorianCalendar();
     cal.set(Calendar.MILLISECOND, 999);
     cal.set(Calendar.SECOND, 59);
     cal.set(Calendar.MINUTE, 59);
     cal.set(Calendar.HOUR_OF_DAY, 23);
        

     try {
     for (int x = 0; x < weeksToDisplay; x++) {
     Date top = cal.getTime();
     cal.add(Calendar.WEEK_OF_YEAR, -1);
     Date bottom = cal.getTime();
     String xAxixValue = sdf.format(top);
     String ptSessionIDs = "1,2,8";// 1,2,8 = PT
     String groupSessionIDs = "3,5,6,7";   // 3,5,6,7 = Group
     int numberOfPTSessionsInTheWeek = ejbSessionHistoryFacade.findMySessionsChartData(loggedInUser.getId(), top, bottom, ptSessionIDs);
     ptSessionSeries.set(xAxixValue, numberOfPTSessionsInTheWeek);
     int numberOfGroupSessionsInTheWeek = ejbSessionHistoryFacade.findMySessionsChartData(loggedInUser.getId(), top, bottom, groupSessionIDs);
     groupSessionSeries.set(xAxixValue, numberOfGroupSessionsInTheWeek);
     }
     } catch (Exception e) {
     JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't get customer session data from the database.");
     }
     model = new BarChartModel();
     model.addSeries(groupSessionSeries);
     model.addSeries(ptSessionSeries);

     }*/
 /*  public Date getChartStartTime() {
        GregorianCalendar startCal = new GregorianCalendar();
        startCal.setTime(getStartDate());
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        setStartDate(startCal.getTime());
        return startCal.getTime();
    }

    public Date getChartEndTime() {
        GregorianCalendar endCal = new GregorianCalendar();
        endCal.setTime(getEndDate());
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        setEndDate(endCal.getTime());
        return endCal.getTime();
    }*/
    public Date zeroTimeField(Date d) {
        GregorianCalendar startCal = new GregorianCalendar();
        startCal.setTime(d);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        return startCal.getTime();
    }

    private Date getRolling12MonthRevenue() {

        GregorianCalendar startCal = new GregorianCalendar();
        startCal.setTime(zeroTimeField(new Date()));
        startCal.add(Calendar.MONTH, -8);
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        return startCal.getTime();

    }

    private BarChartModel createSessionsChart(boolean isTrainer, Customers user, int dateInterval, Date startDate, Date endDate) {
        BarChartModel ccModel = null;
        String xAxisLabel = "";
        GregorianCalendar startCal = new GregorianCalendar();
        GregorianCalendar endCal = new GregorianCalendar();
        int calendarIncrementInterval;
        int numberOfSeriesPoints = 0;
        String dateFormatString;
        switch (dateInterval) {
            case 1: //weekly
            default:
                calendarIncrementInterval = Calendar.WEEK_OF_YEAR;
                xAxisLabel = "Week Starting On";
                dateFormatString = "dd MMM yy";
                break;
            case 2: //monthly
                calendarIncrementInterval = Calendar.MONTH;
                xAxisLabel = "Month";
                dateFormatString = "MMM yy";
                break;
            case 3: //daily
                calendarIncrementInterval = Calendar.DAY_OF_YEAR;
                xAxisLabel = "Day";
                dateFormatString = "dd MMM yy";
                break;

        }
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormatString);

        try {
            startCal.setTime(startDate);
            endCal.setTime(endDate);

            while (startCal.compareTo(endCal) < 0) {
                startCal.add(calendarIncrementInterval, 1);
                numberOfSeriesPoints++;
            }
            startCal.setTime(startDate);
            endCal.setTime(startDate);

            endCal.add(calendarIncrementInterval, 1);

            List<BarChartSeries> seriesList = new ArrayList<>();
            List<SessionHistory> sessions;//ejbSessionHistoryFacade.findSessionsByTrainerAndDateRange(user.getId(), top, bottom, ptSessionIDs);
            List<SessionTypes> sessionTypesList = ejbSessionTypesFacade.findAll();

            for (SessionTypes st : sessionTypesList) {
                BarChartSeries barChartSeries = new BarChartSeries();

                barChartSeries.setLabel(st.getName());
                seriesList.add(barChartSeries);
            }
            try {
                for (int x = 0; x < numberOfSeriesPoints; x++) {

                    String xAxixValue = sdf.format(startCal.getTime());
                    for (BarChartSeries lcs : seriesList) {
                        lcs.set(xAxixValue, (double) 0);
                    }
                    Date strt = startCal.getTime();
                    Date end = endCal.getTime();
                    if (user == null) {
                        sessions = ejbSessionHistoryFacade.findSessionsByDateRange(strt, end, true);
                        logger.log(Level.INFO, "Get ALL Sessions , No.Sessions:{1},startdate:{2},End date:{3}", new Object[]{sessions.size(), strt, end});
                    } else if (isTrainer == false) {
                        sessions = ejbSessionHistoryFacade.findSessionsByParticipantAndDateRange(user, strt, end, true);
                        logger.log(Level.INFO, "Get Sessions for Participant:{0}, No.Sessions:{1},startdate:{2},End date:{3}", new Object[]{user.getUsername(), sessions.size(), strt, end});

                    } else {
                        sessions = ejbSessionHistoryFacade.findSessionsByTrainerAndDateRange(user, strt, end, true);
                        logger.log(Level.INFO, "Get Sessions for Trainer:{0}, No.Sessions:{1},startdate:{2},End date:{3}", new Object[]{user.getUsername(), sessions.size(), strt, end});
                    }

                    for (SessionHistory sess : sessions) {
                        String type = sess.getSessionTypesId().getName();
                        for (BarChartSeries barChartSeries : seriesList) {
                            if (barChartSeries.getLabel().compareTo(type) == 0) {
                                Double c = (Double) barChartSeries.getData().get(xAxixValue);

                                if (c == null) {
                                    c = new Double(1);
                                } else {
                                    c = c + (double) 1;
                                }
                                logger.log(Level.FINE, "Get Sessions for Trainer.Add chartpoint at {1}, Value:{1}", new Object[]{c.toString(), xAxixValue});
                                barChartSeries.set(xAxixValue, c);
                                int index = seriesList.indexOf(barChartSeries);
                                seriesList.set(index, barChartSeries);
                            }
                        }
                    }
                    startCal.add(calendarIncrementInterval, 1);
                    endCal.add(calendarIncrementInterval, 1);

                }
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't get customer session data from the database.");
            }
            ccModel = new BarChartModel();

            int numberOfSeriesAddedToChart = 0;
            for (BarChartSeries bcs : seriesList) {
                Collection<Number> values = bcs.getData().values();
                Double totalSessions = (double) 0;
                Iterator i = values.iterator();
                while (i.hasNext()) {
                    totalSessions = totalSessions + (Double) i.next();
                }

                if (totalSessions.compareTo(new Double(0)) > 0) {
                    ccModel.addSeries(bcs);
                    numberOfSeriesAddedToChart++;
                }
            }
            if (numberOfSeriesAddedToChart == 0) { // add an empty series with a label of empty
                if (seriesList.isEmpty() == false) {
                    BarChartSeries bcs = seriesList.get(0);
                    bcs.setLabel("Empty");
                    bcs.set(sdf.format(startDate), (double) 0);
                    bcs.set(sdf.format(endDate), (double) 0);
                    ccModel.addSeries(bcs);
                    logger.log(Level.INFO, "The date range selected is for the seriesList is empty!");
                } else {
                    logger.log(Level.WARNING, "Cannot creat a chart model as the seriesList is empty!");
                }

            }
            if (user != null) {
                ccModel.setTitle(getSelectedCustomer().getFirstname() + " " + getSelectedCustomer().getLastname());
            } else {
                ccModel.setTitle("All Sessions");
            }
            ccModel.setLegendPosition("ne");
            ccModel.setStacked(true);
            ccModel.setExtender("customUserStatsBarChartExtender");

            Axis xAxis = ccModel.getAxis(AxisType.X);
            xAxis.setLabel(xAxisLabel);
            xAxis.setTickAngle(65);

        } catch (Exception e) {
            logger.log(Level.WARNING, "AN error occurred creating the customChart", e);
        }
        // Axis yAxis = ccModel.getAxis(AxisType.Y);
        // yAxis.setLabel(getY);
        return ccModel;

    }

    private BarChartModel createMonthlyRevenueChart(int datePeriodInterval, Date startDate, Date endDate) {
        BarChartModel ccModel = null;

        GregorianCalendar startCal = new GregorianCalendar();
        GregorianCalendar endCal = new GregorianCalendar();
        GregorianCalendar comparisonCal = new GregorianCalendar();

        int calendarIncrementInterval;
        int numberOfSeriesPoints = 0;
        String dateFormatString;

        String xAxisLabel;

        switch (datePeriodInterval) {
            case 1: //weekly
            default:
                calendarIncrementInterval = Calendar.WEEK_OF_YEAR;
                xAxisLabel = "Week Starting On";
                dateFormatString = "dd MMM yy";
                break;
            case 2: //monthly
                calendarIncrementInterval = Calendar.MONTH;
                xAxisLabel = "Month";
                dateFormatString = "MMM yy";
                break;
            case 3: //daily
                calendarIncrementInterval = Calendar.DAY_OF_YEAR;
                xAxisLabel = "Day";
                dateFormatString = "dd MMM yy";
                break;

        }

        SimpleDateFormat sdf = new SimpleDateFormat(dateFormatString);

        try {
            startCal.setTime(startDate);
            comparisonCal.setTime(startCal.getTime());
            endCal.setTime(endDate);

            while (comparisonCal.compareTo(endCal) < 0) {
                comparisonCal.add(calendarIncrementInterval, 1);
                numberOfSeriesPoints++;
            }

            endCal.setTime(startDate);
            //endCal.set(Calendar.HOUR, 23);
            //endCal.set(Calendar.SECOND, 59);
            // endCal.set(Calendar.MINUTE, 59);
            //endCal.set(Calendar.MILLISECOND, 999);
            endCal.add(calendarIncrementInterval, 1);

            List<ChartSeries> seriesList = new ArrayList<>();

            List<Payments> paymentList;
            List<Expenses> expenseList;

            BarChartSeries barChartSeries1 = new BarChartSeries();
            barChartSeries1.setLabel("Payments");
            seriesList.add(barChartSeries1);

            BarChartSeries barChartSeries2 = new BarChartSeries();
            barChartSeries2.setLabel("Scheduled");
            seriesList.add(barChartSeries2);

            LineChartSeries lineChartSeries1 = new LineChartSeries();
            lineChartSeries1.setLabel("Expenses");
            lineChartSeries1.setYaxis(AxisType.Y2);
            seriesList.add(lineChartSeries1);

            LineChartSeries lineChartSeries2 = new LineChartSeries();
            lineChartSeries2.setLabel("Profit");
            lineChartSeries2.setYaxis(AxisType.Y2);
            seriesList.add(lineChartSeries2);

            double maxTotal = 0;
            try {
                for (int x = 0; x < numberOfSeriesPoints; x++) {

                    String xAxixValue = sdf.format(startCal.getTime());
                    for (ChartSeries lcs : seriesList) {
                        lcs.set(xAxixValue, (double) 0);
                    }
                    Date strt = startCal.getTime();
                    Date end = endCal.getTime();

                    //sessions = ejbSessionHistoryFacade.findSessionsByDateRange(strt, end, true);
                    float reportTotalSuccessful = 0;
                    float reportTotalDishonoured = 0;
                    float reportTotalScheduled = 0;
                    float reportTotalExpenses = 0;

                    boolean reportUseSettlementDate = false;
                    boolean reportShowSuccessful = true;
                    boolean reportShowFailed = false;
                    boolean reportShowPending = true;
                    boolean isReportShowScheduled = true;

                    paymentList = paymentsFacade.findPaymentsByDateRange(reportUseSettlementDate, reportShowSuccessful, reportShowFailed, reportShowPending, isReportShowScheduled, strt, end, false, null);

                    expenseList = expensesFacade.findExpensesByDateRange(strt, end, false);

                    if (paymentList != null) {
                        for (Payments p : paymentList) {
                            if (p.getPaymentStatus().contains(PaymentStatus.SUCESSFUL.value()) || p.getPaymentStatus().contains(PaymentStatus.PENDING.value())) {
                                reportTotalSuccessful += p.getPaymentAmount().floatValue();

                            } else if (p.getPaymentStatus().contains(PaymentStatus.DISHONOURED.value()) || p.getPaymentStatus().contains(PaymentStatus.FATAL_DISHONOUR.value())) {
                                reportTotalDishonoured += p.getPaymentAmount().floatValue();
                            } else if (p.getPaymentStatus().contains(PaymentStatus.SCHEDULED.value())) {
                                reportTotalScheduled += p.getPaymentAmount().floatValue();
                            }
                        }
                        if (expenseList != null) {
                            for (Expenses e : expenseList) {
                                reportTotalExpenses += e.getExpenseAmount().floatValue();
                            }
                        }

                        barChartSeries1.set(xAxixValue, (double) reportTotalSuccessful);
                        int index = seriesList.indexOf(barChartSeries1);
                        seriesList.set(index, barChartSeries1);

                        barChartSeries2.set(xAxixValue, (double) reportTotalScheduled);
                        index = seriesList.indexOf(barChartSeries2);
                        seriesList.set(index, barChartSeries2);

                        double total = (double) reportTotalScheduled + (double) reportTotalSuccessful;
                        double reportTotalProfit = total - (double) reportTotalExpenses;

                        lineChartSeries1.set(xAxixValue, reportTotalExpenses );// as the chart is stacked we need to subract the total for teh line charts
                        index = seriesList.indexOf(lineChartSeries1);
                        seriesList.set(index, lineChartSeries1);

                        lineChartSeries2.set(xAxixValue, reportTotalProfit );// as the chart is stacked we need to subract the total for teh line charts
                        index = seriesList.indexOf(lineChartSeries2);
                        seriesList.set(index, lineChartSeries2);

                        if (total > maxTotal) {
                            maxTotal = total;
                        }
                        if (reportTotalProfit > maxTotal) {
                            maxTotal = reportTotalProfit;
                        }

                        logger.log(Level.INFO, "Get ALL Payments , No.Payments:{1},startdate:{2},End date:{3}", new Object[]{paymentList.size(), strt, end});

                    } else {

                        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.WARNING, "Report Failed - paymentsFacade.findPaymentsByDateRange returned NULL");
                    }

                    startCal.add(calendarIncrementInterval, 1);
                    endCal.add(calendarIncrementInterval, 1);

                }
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't get customer session data from the database.");
            }
            ccModel = new BarChartModel();

            int numberOfSeriesAddedToChart = 0;
            for (ChartSeries bcs : seriesList) {
                Collection<Number> values = bcs.getData().values();
                Double totalSessions = (double) 0;
                Iterator<Number> i = values.iterator();
                while (i.hasNext()) {
                    totalSessions += i.next().doubleValue();
                }

                if (totalSessions.compareTo(new Double(0)) > 0) {
                    if (bcs.getClass() == BarChartSeries.class) {
                        ccModel.addSeries(bcs);
                        numberOfSeriesAddedToChart++;
                    }
                    if (bcs.getClass() == LineChartSeries.class) {
                        ccModel.addSeries(bcs);
                        numberOfSeriesAddedToChart++;
                    }

                }
            }
            if (numberOfSeriesAddedToChart == 0) { // add an empty series with a label of empty 
                if (seriesList.isEmpty() == false) {
                    BarChartSeries bcs = (BarChartSeries) seriesList.get(0);
                    bcs.setLabel("Empty");
                    bcs.set(sdf.format(startDate), (double) 0);
                    bcs.set(sdf.format(endDate), (double) 0);
                    ccModel.addSeries(bcs);
                    logger.log(Level.INFO, "The date range selected is for the seriesList is empty!");
                } else {
                    logger.log(Level.WARNING, "Cannot creat a chart model as the seriesList is empty!");
                }

            }
            ccModel.setTitle("Revenue Report");
            ccModel.setLegendPosition("ne");
            ccModel.setStacked(false);
            ccModel.setExtender("monthlyRevenueBarChartExtender");

            Axis xAxis = ccModel.getAxis(AxisType.X);
            xAxis.setLabel(xAxisLabel);
            xAxis.setTickAngle(65);
            Axis yAxis = ccModel.getAxis(AxisType.Y);
            yAxis.setLabel("Revenue $");
            yAxis.setMin(0);
            Axis y2Axis = new LinearAxis("");
            y2Axis.setMin(0);
            ccModel.getAxes().put(AxisType.Y2, y2Axis);
            double yAxisHeght = 1000;
            while (yAxisHeght < maxTotal) {
                yAxisHeght += 1000;
            }
            yAxis.setMax(yAxisHeght);
            y2Axis.setMax(yAxisHeght);

            // Axis yAxis = ccModel.getAxis(AxisType.Y);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Monthly REvenue Chart Error", e);
        }
        // yAxis.setLabel(getY);
        return ccModel;
    }

    public void recreateModel() {
        model = null;
        model2 = null;
        customModel = null;
    }

    public void recreateDashboardModels() {
        dashboardMonthlyRevenueModel = null;
        dashboardMonthlySessionsModel = null;
        // RequestContext requestContext = RequestContext.getCurrentInstance();

        // requestContext.execute("PF('sessionsDataTable').filter();");
    }

    public BarChartModel getModel() {
        if (model == null) {
            try {
                FacesContext context = FacesContext.getCurrentInstance();
                CustomersController custController = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                model = createSessionsChart(false, custController.getSelected(), getDateInterval(), getStartDate(), getEndDate());
            } catch (ELException e) {
                JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't find the customer in the database.");
            }
        }
        return model;
    }

    public BarChartModel getModel2() {
        if (model2 == null) {
            try {
                FacesContext context = FacesContext.getCurrentInstance();
                CustomersController custController = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                model2 = createSessionsChart(true, custController.getSelected(), getDateInterval(), getStartDate(), getEndDate());
            } catch (ELException e) {
                JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't find the customer in the database.");
            }

        }
        // if(model2.getSeries().isEmpty()){
        //     model2 = null;
        // }

        return model2;
    }

    public BarChartModel getDashboardMonthlyRevenueModel() {
        if (dashboardMonthlyRevenueModel == null) {
            dashboardMonthlyRevenueModel = createMonthlyRevenueChart(getDashboardDateInterval(), getDashboardStartDate(), getDashboardEndDate());
        }
        return dashboardMonthlyRevenueModel;
    }

    public BarChartModel getDashboardMonthlySessionsChartModel() {
        if (dashboardMonthlySessionsModel == null) {
            //boolean isTrainer = FacesContext.getCurrentInstance().getExternalContext().isUserInRole("TRAINER");
            boolean isTrainer = ejbGroupsFacade.isCustomerInGroup(getSelectedCustomer(), "TRAINER");
            if (dashboardShowAllUsers) {
                dashboardMonthlySessionsModel = createSessionsChart(isTrainer, null, getDashboardDateInterval(), getDashboardStartDate(), getDashboardEndDate());
            } else {
                dashboardMonthlySessionsModel = createSessionsChart(isTrainer, getSelectedCustomer(), getDashboardDateInterval(), getDashboardStartDate(), getDashboardEndDate());
            }

        }
        return dashboardMonthlySessionsModel;
    }

    public BarChartModel getCustomChartModel() {
        if (customModel == null) {
            //boolean isTrainer = FacesContext.getCurrentInstance().getExternalContext().isUserInRole("TRAINER");
            boolean isTrainer = ejbGroupsFacade.isCustomerInGroup(getSelectedCustomer(), "TRAINER");
            if (showAllUsers) {
                customModel = createSessionsChart(isTrainer, null, getDateInterval(), getStartDate(), getEndDate());
            } else {
                customModel = createSessionsChart(isTrainer, getSelectedCustomer(), getDateInterval(), getStartDate(), getEndDate());
            }

        }
        return customModel;
    }

    /**
     * @return the selectedCustomer
     */
    public Customers getSelectedCustomer() {
        if (selectedCustomer == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            CustomersController custController = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);

            selectedCustomer = custController.getSelected();
        }
        return selectedCustomer;
    }

    /**
     * @param selectedCustomer the selectedCustomer to set
     */
    public void setSelectedCustomer(Customers selectedCustomer) {
        customModel = null;
        this.selectedCustomer = selectedCustomer;
        FacesContext context = FacesContext.getCurrentInstance();
        SessionHistoryController sessionHistoryController = context.getApplication().evaluateExpressionGet(context, "#{sessionHistoryController}", SessionHistoryController.class);
        sessionHistoryController.recreateModel();
        sessionHistoryController.setSessionHistoryExportFileName();
    }

    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return zeroTimeField(startDate);
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {

        this.startDate = startDate;
        recreateModel();
    }

    /**
     * @return the endDate
     */
    public Date getEndDate() {
        return zeroTimeField(endDate);
    }

    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        recreateModel();
    }

    /**
     * @return the dateInterval
     */
    public int getDateInterval() {
        return dateInterval;
    }

    /**
     * @param dateInterval the dateInterval to set
     */
    public void setDateInterval(int dateInterval) {
        this.dateInterval = dateInterval;
        recreateModel();
    }

    public SelectItem[] getDateIntervalItems() {

        SelectItem[] items = new SelectItem[3];
        items[0] = new SelectItem(1, "Weekly");
        items[1] = new SelectItem(2, "Monthly");
        items[2] = new SelectItem(3, "Daily");

        return items;
    }

    /**
     * @return the showAllUsers
     */
    public boolean isShowAllUsers() {
        return showAllUsers;
    }

    /**
     * @param showAllUsers the showAllUsers to set
     */
    public void setShowAllUsers(boolean showAllUsers) {
        this.showAllUsers = showAllUsers;
        FacesContext context = FacesContext.getCurrentInstance();
        SessionHistoryController sessionHistoryController = context.getApplication().evaluateExpressionGet(context, "#{sessionHistoryController}", SessionHistoryController.class);
        sessionHistoryController.recreateModel();
        sessionHistoryController.setSessionHistoryExportFileName();
    }

    /**
     * @return the dashboardXAxisLabel
     */
    public String getDashboardXAxisLabel() {
        return dashboardXAxisLabel;
    }

    /**
     * @param dashboardXAxisLabel the dashboardXAxisLabel to set
     */
    public void setDashboardXAxisLabel(String dashboardXAxisLabel) {
        this.dashboardXAxisLabel = dashboardXAxisLabel;
    }

    /**
     * @return the dashboardStartDate
     */
    public Date getDashboardStartDate() {
        return dashboardStartDate;
    }

    /**
     * @param dashboardStartDate the dashboardStartDate to set
     */
    public void setDashboardStartDate(Date dashboardStartDate) {
        this.dashboardStartDate = dashboardStartDate;
    }

    /**
     * @return the dashboardEndDate
     */
    public Date getDashboardEndDate() {
        return dashboardEndDate;
    }

    /**
     * @param dashboardEndDate the dashboardEndDate to set
     */
    public void setDashboardEndDate(Date dashboardEndDate) {
        this.dashboardEndDate = dashboardEndDate;
    }

    /**
     * @return the dashboardDateInterval
     */
    public int getDashboardDateInterval() {
        return dashboardDateInterval;
    }

    /**
     * @param dashboardDateInterval the dashboardDateInterval to set
     */
    public void setDashboardDateInterval(int dashboardDateInterval) {
        this.dashboardDateInterval = dashboardDateInterval;
    }

    /**
     * @return the dashboardShowAllUsers
     */
    public boolean isDashboardShowAllUsers() {
        return dashboardShowAllUsers;
    }

    /**
     * @param dashboardShowAllUsers the dashboardShowAllUsers to set
     */
    public void setDashboardShowAllUsers(boolean dashboardShowAllUsers) {
        this.dashboardShowAllUsers = dashboardShowAllUsers;
    }
}
