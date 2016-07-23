/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.Date;
import java.util.concurrent.Future;

/**
 *
 * @author david
 */
public class AsyncJob {
    private final String jobName;
    private final Future<PaymentGatewayResponse> future;
    private final Date startTime;
    private  long batchId;
    
    public AsyncJob(String key,Future<PaymentGatewayResponse> ft){
        this.jobName = key;
        this.future=ft;
        this.startTime = new Date();
        // zero means it does not belong to a batch
        this.batchId = 0;
        
    }

    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @return the future
     */
    public Future<PaymentGatewayResponse> getFuture() {
        return future;
    }

    /**
     * @return the startTime
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * @return the batchId
     */
    public long getBatchId() {
        return batchId;
    }

    /**
     * @param batchId the batchId to set
     */
    public void setBatchId(long batchId) {
        this.batchId = batchId;
    }
    
}
