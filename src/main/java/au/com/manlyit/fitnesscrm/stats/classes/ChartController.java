/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.FitnessTestCharts;
import au.com.manlyit.fitnesscrm.stats.db.Stat;
import au.com.manlyit.fitnesscrm.stats.db.StatTypes;
import au.com.manlyit.fitnesscrm.stats.db.StatsTaken;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.faces.context.FacesContext;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.DateAxis;

/**
 *
 * @author david
 */
@Named("chartController")
@SessionScoped
public class ChartController implements Serializable {

    /**
     * Creates a new instance of chartController
     */
    public ChartController() {
    }
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.StatsFacade ejbStatsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.FitnessTestChartsFacade ejbFitnessTestChartsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.StatTypesFacade ejbStatTypesFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.StatsTakenFacade ejbStatsTakenFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;

    //private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private final int numberOfStatisticTypes = 14;
    private ArrayList<LineChartModel> modelList;

    public int getUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        return custController.getSelected().getId();
    }

    public Customers getCustomer() {
        // String passCheck = PasswordService.getInstance().encrypt("1234567890");
        //  System.out.println("\n\ne-check:" + passCheck + "\n\n");
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        return custController.getSelected();
    }

    /**
     * @param stypes
     * @return the chartData
     */
    public List<LineChartPointsVertical> getChartData(List<StatTypes> stypes) {
        List<LineChartPointsVertical> chartMeasurements = new ArrayList<>();
        List<StatsTaken> statsTakenList = ejbStatsTakenFacade.findAllByCustomer(getCustomer(),false);
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

    public List<ChartSeries> getChartDataForModel(ArrayList<StatTypes> stypes) {

        List<ChartSeries> chartMeasurements = new ArrayList<>();
        for (StatTypes stype : stypes) {
            chartMeasurements.add(new ChartSeries());
        }
        List<StatsTaken> statsTakenList = ejbStatsTakenFacade.findAllByCustomer(getCustomer(),false);
        for (StatsTaken statTaken : statsTakenList) {
            List<Stat> stats = ejbStatsFacade.findAll(statTaken.getId());
            int numberOfStats = stats.size();
            if (numberOfStats == numberOfStatisticTypes) {

                String xAxixValue = sdf.format(statTaken.getDateRecorded());
                // Date xAxixValue = statTaken.getDateRecorded();
                for (Stat stat : stats) {
                    for (int c = 0; c < stypes.size(); c++) {
                        StatTypes stype = stypes.get(c);
                        if (stat.getStatType().getName().compareTo(stype.getName()) == 0) {
                            ChartSeries series = chartMeasurements.get(c);
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

    /**
     * @return the modelList
     */
    public ArrayList<LineChartModel> getModelList() {
        if (modelList == null) {
            modelList = new ArrayList<>();
            //get list of charts types that customer has
            List<FitnessTestCharts> chartsList = ejbFitnessTestChartsFacade.findAll();
            for (FitnessTestCharts chrt : chartsList) {
                Collection<StatTypes> stypes = chrt.getStatTypesCollection();
                LineChartModel mod = createModel(chrt, stypes);
                if (mod.getSeries().isEmpty() == false) {
                    modelList.add(mod);
                }

            }
        }
        return modelList;
    }

    public void recreateModel() {
        if (modelList != null) {
            modelList.clear();
        }
        modelList = null;

    }

    private LineChartModel createModel(FitnessTestCharts chrt, Collection<StatTypes> stypes) {
        List<ChartSeries> seriesList = getChartDataForModel(new ArrayList<>(stypes));
        LineChartModel model = new LineChartModel();
        for (ChartSeries cs : seriesList) {
            if (cs.getData().isEmpty() == false) {
                model.addSeries(cs);
            }

        }
        model.setTitle(chrt.getName());
        model.getAxis(AxisType.Y).setLabel(chrt.getYaxisLabel());
        DateAxis axis = new DateAxis();
        axis.setTickAngle(-50);
        GregorianCalendar gc = new GregorianCalendar();
        gc.add(Calendar.DAY_OF_YEAR, 3);
        axis.setMax(sdf.format(gc.getTime()));
        axis.setTickFormat("%b %#d, %y");
        model.getAxes().put(AxisType.X, axis);
        model.setLegendPosition("ne");
        return model;
    }

    /**
     * @param modelList the modelList to set
     */
    public void setModelList(ArrayList<LineChartModel> modelList) {
        this.modelList = modelList;
    }
}
