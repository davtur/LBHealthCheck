/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Stat;
import au.com.manlyit.fitnesscrm.stats.db.StatTypes;
import au.com.manlyit.fitnesscrm.stats.db.StatsTaken;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import  org.primefaces.model.chart.LineChartSeries;

/**
 *
 * @author david
 */
@ManagedBean(name = "chartController")
@SessionScoped
public class ChartController implements Serializable {

    /** Creates a new instance of chartController */
    public ChartController() {
    }
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.StatsFacade ejbStatsFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.StatTypesFacade ejbStatTypesFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.StatsTakenFacade ejbStatsTakenFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    private JsfUtil jsfUtil = new JsfUtil();
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
    private int numberOfStatisticTypes = 14;

    public int getUser() {
        int cust_id = 0;
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        String name = custController.getSelected().getUsername();
        Customers cust = ejbCustomerFacade.findCustomerByUsername(name);
        cust_id = cust.getId();
        return cust_id;
    }

    public Customers getCustomer() {
        Customers cust = null;
        String passCheck = PasswordService.getInstance().encrypt("1234567890");
        System.out.println("\n\ne-check:" + passCheck + "\n\n");
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        String name = custController.getSelected().getUsername();
        cust = ejbCustomerFacade.findCustomerByUsername(name);

        return cust;
    }

    /**
     * @return the chartData
     */
    public List<LineChartPointsVertical> getChartData(List<StatTypes> stypes) {
        List<LineChartPointsVertical> chartMeasurements = new ArrayList<LineChartPointsVertical>();
        List<StatsTaken> statsTakenList = ejbStatsTakenFacade.findAllByCustId(getUser());
        for (StatsTaken st : statsTakenList) {
            List<Stat> stats = ejbStatsFacade.findAll(st.getId());
            int numberOfStats = stats.size();
            if (numberOfStats == numberOfStatisticTypes) {
                LineChartPointsVertical points = new LineChartPointsVertical();
                points.setTimeTaken(sdf.format(st.getDateRecorded()));
                for (Stat stat : stats) {
                    for (StatTypes stype : stypes) {
                        if (stat.getStatType().getName().compareTo(stype.getName()) == 0) {
                            points.addStat(stat);
                        }
                    }
                }
                chartMeasurements.add(points);
            } else {
                String mess = "The number of stats retrieved for " + st.getCustomerId().getUsername() + " at collection id " + st.getId().toString() + " is " + numberOfStats + " when it should be " + numberOfStatisticTypes + ". Skipping this one!";
                JsfUtil.addErrorMessage(mess);
            }
        }
        return chartMeasurements;
    }

    public List<LineChartSeries> getChartDataForModel(List<StatTypes> stypes) {

        List<LineChartSeries> chartMeasurements = new ArrayList<LineChartSeries>();
        for (StatTypes stype : stypes) {
            chartMeasurements.add(new LineChartSeries());
        }
        List<StatsTaken> statsTakenList = ejbStatsTakenFacade.findAllByCustId(getUser());
        for (StatsTaken statTaken : statsTakenList) {
            List<Stat> stats = ejbStatsFacade.findAll(statTaken.getId());
            int numberOfStats = stats.size();
            if (numberOfStats == numberOfStatisticTypes) {

                String xAxixValue = sdf.format(statTaken.getDateRecorded());
                for (Stat stat : stats) {
                    for (int c = 0; c < stypes.size(); c++) {
                        StatTypes stype = stypes.get(c);
                        if (stat.getStatType().getName().compareTo(stype.getName()) == 0) {
                            LineChartSeries series = chartMeasurements.get(c);
                            float val = stat.getValue();
                            if (val > 0) {
                                series.set(xAxixValue, stat.getValue());
                                series.setLabel(stat.getStatType().getName());
                            }
                            chartMeasurements.set(c, series);
                        }
                    }
                }

            } else {
                String mess = "\nThe number of stats retrieved for " + statTaken.getCustomerId().getUsername() + " at collection id " + statTaken.getId().toString() + " is " + numberOfStats + " when it should be " + numberOfStatisticTypes + ". Skipping this one!";
                JsfUtil.addErrorMessage(mess);
            }
        }
        return chartMeasurements;
    }
}
/*  public List<LineChartPointsVertical> getChartMeasurements() {

ArrayList<StatTypes> stypes = new ArrayList<StatTypes>();
stypes.add(ejbStatTypesFacade.find(1));
stypes.add(ejbStatTypesFacade.find(2));
stypes.add(ejbStatTypesFacade.find(3));
stypes.add(ejbStatTypesFacade.find(4));
stypes.add(ejbStatTypesFacade.find(5));
return getChartData(stypes);
}

public List<LineChartPointsVertical> getChartPerMInuteTests() {

ArrayList<StatTypes> stypes = new ArrayList<StatTypes>();
stypes.add(ejbStatTypesFacade.find(7));
stypes.add(ejbStatTypesFacade.find(8));
stypes.add(ejbStatTypesFacade.find(9));
return getChartData(stypes);
}

public List<LineChartPointsVertical> getChartBeepAndVo2Test() {

ArrayList<StatTypes> stypes = new ArrayList<StatTypes>();
stypes.add(ejbStatTypesFacade.find(6));
stypes.add(ejbStatTypesFacade.find(11));

return getChartData(stypes);
}

public List<LineChartPointsVertical> getChartMaxHeartrate() {

ArrayList<StatTypes> stypes = new ArrayList<StatTypes>();
stypes.add(ejbStatTypesFacade.find(10));

return getChartData(stypes);
}

public List<LineChartPointsVertical> getChartBodyFat() {

ArrayList<StatTypes> stypes = new ArrayList<StatTypes>();
stypes.add(ejbStatTypesFacade.find(12));

return getChartData(stypes);
}

public List<LineChartPointsVertical> getChartWeight() {

ArrayList<StatTypes> stypes = new ArrayList<StatTypes>();
stypes.add(ejbStatTypesFacade.find(13));

return getChartData(stypes);
}

public List<LineChartPointsVertical> getChart2kRun() {

ArrayList<StatTypes> stypes = new ArrayList<StatTypes>();
stypes.add(ejbStatTypesFacade.find(14));

return getChartData(stypes);
}*/
