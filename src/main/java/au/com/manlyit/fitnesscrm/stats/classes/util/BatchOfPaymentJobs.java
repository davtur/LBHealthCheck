/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author david
 */
public class BatchOfPaymentJobs {
    
    
    private final String jobName;
    private final long batchId;
    private final Date startTime;
    private final ArrayList<Integer> jobs;
    
    public BatchOfPaymentJobs(String jobName,ArrayList<Integer> jobs){
        this.jobName = jobName;
        this.startTime = new Date();
        this.jobs = jobs;
        this.batchId = System.nanoTime();
        
    }

    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @return the startTime
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * @return the jobs
     */
    public ArrayList<Integer> getJobs() {
        return jobs;
    }

    /**
     * @return the batchId
     */
    public long getBatchId() {
        return batchId;
    }
    
    
}
