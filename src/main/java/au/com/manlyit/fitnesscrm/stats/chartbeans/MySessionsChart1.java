/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.chartbeans;

import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import  org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.CartesianChartModel;

/**
 *
 * @author david
 */
import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedBean;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@ManagedBean(name = "mySessionsChart1")
@RequestScoped
public class MySessionsChart1 implements Serializable {

    private CartesianChartModel model;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");

    /** Creates a new instance of MeasurementsChart */
    public MySessionsChart1() {
    }
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.ParticipantsFacade ejbParticipantsFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade ejbSessionHistoryFacade;

    @PostConstruct
    public void createChart() {
        FacesContext context = FacesContext.getCurrentInstance();
        Customers loggedInUser = null;
        try {
            //loggedInUser = ejbCustomerFacade.findCustomerByUsername(context.getExternalContext().getRemoteUser());

            CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
            loggedInUser = custController.getSelected();

        } catch (Exception e) {
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

    public CartesianChartModel getModel() {
        if (model == null) {
            createChart();
        }
        return model;
    }
}
