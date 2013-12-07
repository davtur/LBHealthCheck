/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.chartbeans;

import au.com.manlyit.fitnesscrm.stats.classes.ChartController;
import au.com.manlyit.fitnesscrm.stats.db.StatTypes;
import java.util.ArrayList;
import java.util.List;
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

@ManagedBean(name = "fitnessTestsChart")
@RequestScoped
public class FitnessTestsChart  implements Serializable {



    private CartesianChartModel model;

    /** Creates a new instance of measurementsChart */
    public FitnessTestsChart() {
    }
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.StatTypesFacade ejbStatTypesFacade;

    @PostConstruct
    public void createChart() {

        FacesContext context = FacesContext.getCurrentInstance();
        ChartController chartController = (ChartController) context.getApplication().evaluateExpressionGet(context, "#{chartController}", ChartController.class);
        ArrayList<StatTypes> stypes = new ArrayList<StatTypes>();
        // add the types of statistics we want to show on this chart - see the stat_types table in the DB for names.
        stypes.add(ejbStatTypesFacade.find(7)); //push up
        stypes.add(ejbStatTypesFacade.find(9)); //Situp

        List<LineChartSeries> seriesList = chartController.getChartDataForModel(stypes);
        model = new CartesianChartModel();
        for (LineChartSeries cs : seriesList) {
            model.addSeries(cs);
        }
    }

    public CartesianChartModel getModel() {

        return model;
    }
}
