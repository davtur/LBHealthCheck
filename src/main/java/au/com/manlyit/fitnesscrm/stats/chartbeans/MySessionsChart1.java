/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.chartbeans;

import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.classes.SessionHistoryController;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import javax.inject.Inject;
import javax.faces.context.FacesContext;
import org.primefaces.model.chart.ChartSeries;

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

@Named("mySessionsChart1")
@SessionScoped
public class MySessionsChart1 implements Serializable {

    private static final Logger logger = Logger.getLogger(MySessionsChart1.class.getName());
    private BarChartModel model;
    private BarChartModel model2;
    private BarChartModel customModel;

    private Customers selectedCustomer;
    private String xAxisLabel = "Week Starting On";
    private Date startDate;
    private Date endDate;
    private int dateInterval = 1;
    private boolean showAllUsers = false;

    @PostConstruct
    private void initDates() {
        GregorianCalendar cal1 = new GregorianCalendar();
        cal1.add(Calendar.DAY_OF_YEAR, 1);
        setEndDate(cal1.getTime());
        cal1.add(Calendar.DAY_OF_YEAR, -1);
        cal1.add(Calendar.MONTH, -2);
        setStartDate(cal1.getTime());
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
    public Date getChartStartTime() {
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
    }

    private BarChartModel createSessionsChart(boolean isTrainer, Customers user) {
        BarChartModel ccModel;
        GregorianCalendar startCal = new GregorianCalendar();
        GregorianCalendar endCal = new GregorianCalendar();
        int calendarIncrementInterval;
        int numberOfSeriesPoints = 0;
        String dateFormatString;
        switch (getDateInterval()) {
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

        startCal.setTime(getChartStartTime());
        endCal.setTime(getChartEndTime());

        while (startCal.compareTo(endCal) < 0) {
            startCal.add(calendarIncrementInterval, 1);
            numberOfSeriesPoints++;
        }
        startCal.setTime(getStartDate());
        endCal.setTime(getStartDate());
        //endCal.set(Calendar.HOUR, 23);
        //endCal.set(Calendar.SECOND, 59);
        // endCal.set(Calendar.MINUTE, 59);
        //endCal.set(Calendar.MILLISECOND, 999);
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
                } else {
                    if (isTrainer == false) {
                        sessions = ejbSessionHistoryFacade.findSessionsByParticipantAndDateRange(user, strt, end, true);
                        logger.log(Level.INFO, "Get Sessions for Participant:{0}, No.Sessions:{1},startdate:{2},End date:{3}", new Object[]{user.getUsername(), sessions.size(), strt, end});

                    } else {
                        sessions = ejbSessionHistoryFacade.findSessionsByTrainerAndDateRange(user, strt, end, true);
                        logger.log(Level.INFO, "Get Sessions for Trainer:{0}, No.Sessions:{1},startdate:{2},End date:{3}", new Object[]{user.getUsername(), sessions.size(), strt, end});
                    }
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
                bcs.set(sdf.format(getChartStartTime()), (double) 0);
                bcs.set(sdf.format(getChartEndTime()), (double) 0);
                ccModel.addSeries(bcs);
                logger.log(Level.INFO, "The date range selected is for the seriesList is empty!");
            } else {
                logger.log(Level.WARNING, "Cannot creat a chart model as the seriesList is empty!");
            }

        }
        ccModel.setTitle(getSelectedCustomer().getFirstname() + " " + getSelectedCustomer().getLastname());
        ccModel.setLegendPosition("ne");
        ccModel.setStacked(true);
        ccModel.setExtender("customUserStatsBarChartExtender");

        Axis xAxis = ccModel.getAxis(AxisType.X);
        xAxis.setLabel(getXaxisLabel());
        xAxis.setTickAngle(65);

        // Axis yAxis = ccModel.getAxis(AxisType.Y);
        // yAxis.setLabel(getY);
        return ccModel;
    }

    public void recreateModel() {
        model = null;
        model2 = null;
        customModel = null;
    }

    public BarChartModel getModel() {
        if (model == null) {
            try {
                FacesContext context = FacesContext.getCurrentInstance();
                CustomersController custController = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                model = createSessionsChart(false, custController.getSelected());
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
                model2 = createSessionsChart(true, custController.getSelected());
            } catch (ELException e) {
                JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't find the customer in the database.");
            }

        }
        // if(model2.getSeries().isEmpty()){
        //     model2 = null;
        // }

        return model2;
    }

    public BarChartModel getCustomChartModel() {
        if (customModel == null) {
            //boolean isTrainer = FacesContext.getCurrentInstance().getExternalContext().isUserInRole("TRAINER");
            boolean isTrainer = ejbGroupsFacade.isCustomerInGroup(getSelectedCustomer(), "TRAINER");
            if (showAllUsers) {
                customModel = createSessionsChart(isTrainer, null);
            } else {
                customModel = createSessionsChart(isTrainer, getSelectedCustomer());
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
     * @return the xAxisLabel
     */
    public String getXaxisLabel() {
        return xAxisLabel;
    }

    /**
     * @param xAxisLabel the xAxisLabel to set
     */
    public void setXaxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
    }

    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
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
        return endDate;
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
}
