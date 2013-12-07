/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.callable;

import java.util.concurrent.Callable;
import org.quartz.JobDataMap;



/**
 *
 * @author david
 */
public class callableTaskTemplate implements Callable<CallableTaskResults> {

    private final JobDataMap paramMap;

    public callableTaskTemplate(JobDataMap parameters) {
        this.paramMap = parameters;
    }

    @Override
    public CallableTaskResults call() {
           CallableTaskResults result = new CallableTaskResults();
           result.setResultData("Put some results here");
        return result;
    }

    
}
