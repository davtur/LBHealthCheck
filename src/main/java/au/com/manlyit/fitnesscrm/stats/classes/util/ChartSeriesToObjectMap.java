/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import org.primefaces.model.chart.ChartSeries;

/**
 *
 * @author david
 */
public class ChartSeriesToObjectMap {

    private ChartSeries chartSeries;
    private BaseEntity entity;

    
    public ChartSeriesToObjectMap(BaseEntity entity,ChartSeries chartSeries){
        this.entity = entity;
        this.chartSeries = chartSeries;
    }

    /**
     * @return the chartSeries
     */
    public ChartSeries getChartSeries() {
        return chartSeries;
    }

    /**
     * @param chartSeries the chartSeries to set
     */
    public void setChartSeries(ChartSeries chartSeries) {
        this.chartSeries = chartSeries;
    }

    /**
     * @return the entity
     */
    public BaseEntity getEntity() {
        return entity;
    }

    /**
     * @param entity the entity to set
     */
    public void setEntity(BaseEntity entity) {
        this.entity = entity;
    }
    
   
    
    
}
