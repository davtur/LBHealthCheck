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
    private final Future future;
    private final Date startTime;
    
    public AsyncJob(String key,Future ft){
        this.jobName = key;
        this.future=ft;
        this.startTime = new Date();
        
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
    public Future getFuture() {
        return future;
    }

    /**
     * @return the startTime
     */
    public Date getStartTime() {
        return startTime;
    }
    
}
