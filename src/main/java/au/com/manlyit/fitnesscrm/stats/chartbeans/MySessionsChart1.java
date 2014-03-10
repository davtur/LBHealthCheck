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
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.el.ELException;

@Named("mySessionsChart1")
@RequestScoped
public class MySessionsChart1 implements Serializable {

    private CartesianChartModel model;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");

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

    private void createSessionsChart() {

        FacesContext context = FacesContext.getCurrentInstance();
        Customers loggedInUser = null;
        try {
            //loggedInUser = ejbCustomerFacade.findCustomerByUsername(context.getExternalContext().getRemoteUser());

            CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
            loggedInUser = custController.getSelected();

        } catch (ELException e) {
            JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't find the customer in the database.");
        }
        int weeksToDisplay = 12;
        GregorianCalendar startCal = new GregorianCalendar();
        GregorianCalendar endCal = new GregorianCalendar();
        startCal.set(Calendar.HOUR, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        endCal.setTime(startCal.getTime());
        endCal.add(Calendar.MILLISECOND, -1);
        endCal.add(Calendar.DAY_OF_YEAR, 1);
        startCal.add(Calendar.WEEK_OF_YEAR, -1);
        startCal.add(Calendar.WEEK_OF_YEAR, 0 - weeksToDisplay);
        endCal.add(Calendar.WEEK_OF_YEAR, 0 - weeksToDisplay);

        List<LineChartSeries> seriesList = new ArrayList<>();
        List<SessionHistory> sessions = null;//ejbSessionHistoryFacade.findSessionsByTrainerAndDateRange(loggedInUser.getId(), top, bottom, ptSessionIDs);
        List<SessionTypes> sessionTypesList = ejbSessionTypesFacade.findAll();
       
        for (SessionTypes st : sessionTypesList) {
            LineChartSeries lcs = new LineChartSeries();
            lcs.setLabel(st.getName());
            seriesList.add(lcs);
        }
        try {
            for (int x = 0; x < weeksToDisplay; x++) {
                startCal.add(Calendar.WEEK_OF_YEAR, 1);
                endCal.add(Calendar.WEEK_OF_YEAR, 1);
                String xAxixValue = sdf.format(startCal.getTime());
                for (LineChartSeries lcs : seriesList) {
                    lcs.set(xAxixValue, new Double(0));
                }
                sessions = ejbSessionHistoryFacade.findSessionsByParticipantAndDateRange(loggedInUser, startCal.getTime(), endCal.getTime(), true);
                for (SessionHistory sess : sessions) {
                    String type = sess.getSessionTypesId().getName();
                    for (LineChartSeries lcs : seriesList) {
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
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't get customer session data from the database.");
        }
        model = new CartesianChartModel();

        for (LineChartSeries lcs : seriesList) {
            if (lcs.getData().isEmpty() == false) {
                model.addSeries(lcs);
            }
        }

    }

    public void recreateModel() {
        model = null;
    }

    public CartesianChartModel getModel() {
        if (model == null) {
            createSessionsChart();
        }
        return model;
    }
}
