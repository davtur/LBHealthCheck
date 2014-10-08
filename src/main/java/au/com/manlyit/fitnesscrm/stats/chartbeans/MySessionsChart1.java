/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.chartbeans;

import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import javax.inject.Inject;
import javax.faces.context.FacesContext;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.CartesianChartModel;

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
import org.primefaces.model.chart.BarChartSeries;

@Named("mySessionsChart1")
@SessionScoped
public class MySessionsChart1 implements Serializable {

    private static final Logger logger = Logger.getLogger(MySessionsChart1.class.getName());
    private CartesianChartModel model;
    private CartesianChartModel model2;
    private CartesianChartModel customModel;

    private Customers selectedCustomer;
    private String xAxisLabel = "Week Starting On";
    private Date startDate;
    private Date endDate;
    private int dateInterval = 1;

    @PostConstruct
    private void initDates() {
        GregorianCalendar cal1 = new GregorianCalendar();
        setEndDate(cal1.getTime());
        cal1.add(Calendar.MONTH, -3);
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

    //@PostConstruct
    public void createChart() {
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
        LineChartSeries groupSessionSeries = new LineChartSeries();
        groupSessionSeries.setLabel("Group Sessions");
        LineChartSeries ptSessionSeries = new LineChartSeries();
        ptSessionSeries.setLabel("PT Sessions");
        int weeksToDisplay = 26;
        GregorianCalendar cal = new GregorianCalendar();

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
        model = new CartesianChartModel();
        model.addSeries(groupSessionSeries);
        model.addSeries(ptSessionSeries);

    }
    public Date getChartStartTime(){
         GregorianCalendar startCal = new GregorianCalendar();
         startCal.setTime(getStartDate());
        startCal.set(Calendar.HOUR, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        
        return startCal.getTime();
    }
    public Date getChartEndTime(){
        GregorianCalendar endCal = new GregorianCalendar();
        endCal.setTime(getEndDate());
        endCal.set(Calendar.HOUR, 23);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        return endCal.getTime();
    }

    private CartesianChartModel createSessionsChart(boolean isTrainer, Customers user) {
        CartesianChartModel ccModel;
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
        setStartDate(getChartStartTime());
        startCal.setTime(getStartDate());
        endCal.setTime(getChartEndTime());
        
        while (startCal.compareTo(endCal) < 0) {
            startCal.add(calendarIncrementInterval, 1);
            numberOfSeriesPoints++;
        }
        startCal.setTime(getStartDate());
        endCal.setTime(getStartDate());
        endCal.set(Calendar.HOUR, 23);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        endCal.add(calendarIncrementInterval, 1);

        List<BarChartSeries> seriesList = new ArrayList<>();
        List<SessionHistory> sessions;//ejbSessionHistoryFacade.findSessionsByTrainerAndDateRange(user.getId(), top, bottom, ptSessionIDs);
        List<SessionTypes> sessionTypesList = ejbSessionTypesFacade.findAll();

        for (SessionTypes st : sessionTypesList) {
            BarChartSeries lcs = new BarChartSeries();
            lcs.setLabel(st.getName());
            seriesList.add(lcs);
        }
        try {
            for (int x = 0; x < numberOfSeriesPoints; x++) {

                String xAxixValue = sdf.format(startCal.getTime());
                for (BarChartSeries lcs : seriesList) {
                    lcs.set(xAxixValue, new Double(0));
                }
                if (isTrainer == false) {
                    sessions = ejbSessionHistoryFacade.findSessionsByParticipantAndDateRange(user, startCal.getTime(), endCal.getTime(), true);
                } else {
                    sessions = ejbSessionHistoryFacade.findSessionsByTrainerAndDateRange(user, startCal.getTime(), endCal.getTime(), true);
                }

                for (SessionHistory sess : sessions) {
                    String type = sess.getSessionTypesId().getName();
                    for (BarChartSeries lcs : seriesList) {
                        if (lcs.getLabel().compareTo(type) == 0) {
                            Double c = (Double) lcs.getData().get(xAxixValue);
                            if (c == null) {
                                c = new Double(1);
                            } else {
                                c = c + new Double(1);
                            }
                            lcs.set(xAxixValue, c);
                            int index = seriesList.indexOf(lcs);
                            seriesList.set(index, lcs);
                        }
                    }
                }
                startCal.add(calendarIncrementInterval, 1);
                endCal.add(calendarIncrementInterval, 1);

            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't get customer session data from the database.");
        }
        ccModel = new CartesianChartModel();
        int numberOfSeriesAddedToChart = 0;
        for (BarChartSeries bcs : seriesList) {
            Collection<Number> values = bcs.getData().values();
            Double totalSessions = new Double(0);
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
                ccModel.addSeries(seriesList.get(0));
            } else {
                logger.log(Level.WARNING, "Cannot creat a chart model as the seriesList is empty!");
            }

        }
        return ccModel;
    }

    public void recreateModel() {
        model = null;
        model2 = null;
        customModel = null;
    }

    public CartesianChartModel getModel() {
        if (model == null) {
            try {
                FacesContext context = FacesContext.getCurrentInstance();
                CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                model = createSessionsChart(false, custController.getSelected());
            } catch (ELException e) {
                JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't find the customer in the database.");
            }
        }
        return model;
    }

    public CartesianChartModel getModel2() {
        if (model2 == null) {
            try {
                FacesContext context = FacesContext.getCurrentInstance();
                CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                model2 = createSessionsChart(true, custController.getSelected());
            } catch (ELException e) {
                JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't find the customer in the database.");
            }

        }

        return model2;
    }

    public CartesianChartModel getCustomChartModel() {
        if (customModel == null) {
            boolean isTrainer = FacesContext.getCurrentInstance().getExternalContext().isUserInRole("TRAINER");
            customModel = createSessionsChart(isTrainer, getSelectedCustomer());
        }
        return customModel;
    }

    /**
     * @return the selectedCustomer
     */
    public Customers getSelectedCustomer() {
        if (selectedCustomer == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);

            selectedCustomer = custController.getLoggedInUser();
        }
        return selectedCustomer;
    }

    /**
     * @param selectedCustomer the selectedCustomer to set
     */
    public void setSelectedCustomer(Customers selectedCustomer) {
        customModel = null;
        this.selectedCustomer = selectedCustomer;
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
}
