/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.Serializable;

/**
 *
 * @author david
 */
public class PlanSummary implements Serializable{

    private static final long serialVersionUID = 1L;
    private  final String planName;
    private int numberOfPlans = 0;
    public PlanSummary(String planName,int numberOfPlans){
        this.numberOfPlans = numberOfPlans;
        this.planName = planName;
    }

    /**
     * @return the planName
     */
    public String getPlanName() {
        return planName;
    }

    public void incrementCount(){
        numberOfPlans += 1;
    }

    public String getNumberOfPlansAsString() {
        return Integer.toString(numberOfPlans);
    }
    /**
     * @return the numberOfPlans
     */
    public int getNumberOfPlans() {
        return numberOfPlans;
    }

    /**
     * @param numberOfPlans the numberOfPlans to set
     */
    public void setNumberOfPlans(int numberOfPlans) {
        this.numberOfPlans = numberOfPlans;
    }
  
    
}
