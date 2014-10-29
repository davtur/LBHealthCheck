/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.chartbeans;

import org.primefaces.model.chart.LineChartModel;

/**
 *
 * @author david
 */
public class FitnessCartesianChartModel extends LineChartModel{
    

    private String chartName;
    private String yAxisLabel;
    private String xAxisLabel;

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

    /**
     * @return the yAxisLabel
     */
    public String getyAxisLabel() {
        return yAxisLabel;
    }

    /**
     * @param yAxisLabel the yAxisLabel to set
     */
    public void setyAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;
    }

    /**
     * @return the xAxisLabel
     */
    public String getxAxisLabel() {
        return xAxisLabel;
    }

    /**
     * @param xAxisLabel the xAxisLabel to set
     */
    public void setxAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
    }
    
}
