/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.chartbeans;

import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import javax.annotation.PostConstruct;
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
        LineChartSeries groupSessionSeries = new LineChartSeries();
        groupSessionSeries.setLabel("Group Sessions");
        LineChartSeries ptSessionSeries = new LineChartSeries();
        ptSessionSeries.setLabel("PT Sessions");
        int weeksToDisplay = 26;
        GregorianCalendar cal = new GregorianCalendar();
        List<SessionHistory> sessions = null;//ejbSessionHistoryFacade.findSessionsByTrainerAndDateRange(loggedInUser.getId(), top, bottom, ptSessionIDs);
        try {
            for (int x = 0; x < weeksToDisplay; x++) {
                Date top = cal.getTime();
                cal.add(Calendar.WEEK_OF_YEAR, -1);
                Date bottom = cal.getTime();
                String xAxixValue = sdf.format(top);
                sessions = ejbSessionHistoryFacade.findSessionsByTrainerAndDateRange(loggedInUser, top, bottom, true);
                int ptCount = 0;
                int groupCount = 0;
                for (SessionHistory sess : sessions) {
                    switch (sess.getId()) {
                        case 1:// PT Sessions
                        case 2:
                        case 8:
                            ptCount++;
                            break;
                        default:// other types
                            groupCount++;
                            break;
                    }
                }
                ptSessionSeries.set(xAxixValue, ptCount);
                groupSessionSeries.set(xAxixValue, groupCount);
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "My Sessions Chart Critical Error", "Couldn't get customer session data from the database.");
        }
        model = new CartesianChartModel();
        model.addSeries(groupSessionSeries);
        model.addSeries(ptSessionSeries);

    }

    public CartesianChartModel getModel() {
        if (model == null) {
            createSessionsChart();
        }
        return model;
    }
}
