/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.chartbeans;

import org.primefaces.model.chart.CartesianChartModel;

/**
 *
 * @author david
 */
public class FitnessCartesianChartModel extends CartesianChartModel{
    

    private String chartName;

    /**
     * @return the chartName
     */
    public String getChartName() {
        return chartName;
    }

    /**
     * @param chartName the chartName to set
     */
    public void setChartName(String chartName) {
        this.chartName = chartName;
    }
    
}
